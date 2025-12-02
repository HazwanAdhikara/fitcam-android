package com.example.fitcam.ui.screens.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitcam.data.SensorSystem
import com.example.fitcam.data.VoiceCommandManager
import com.example.fitcam.data.WorkoutRepository
import com.example.fitcam.data.WorkoutSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

enum class WorkoutType { PUSH_UP, SQUAT, PLANK }
enum class WorkoutState { SETUP, ACTIVE, PAUSED, RESTING, FINISHED, STOPPED }
enum class SaveStatus { IDLE, SAVING, SUCCESS, ERROR }

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorSystem = SensorSystem(application)
    private val repository = WorkoutRepository()
    private var voiceManager: VoiceCommandManager? = null

    // CONFIGURATION
    var targetSets = 3
    var targetReps = 10
    var targetDuration = 30L
    var restTimeSeconds = 30

    // STATES
    private val _workoutState = MutableStateFlow(WorkoutState.SETUP)
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    private val _currentType = MutableStateFlow(WorkoutType.PUSH_UP)
    val currentType: StateFlow<WorkoutType> = _currentType.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.IDLE)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    // PROGRESS
    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet.asStateFlow()

    private val _currentReps = MutableStateFlow(0)
    val currentReps: StateFlow<Int> = _currentReps.asStateFlow()

    // Used for Plank target OR General Timer for Rep exercises
    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration: StateFlow<Long> = _currentDuration.asStateFlow()

    private val _restTimer = MutableStateFlow(0)
    val restTimer: StateFlow<Int> = _restTimer.asStateFlow()

    private val _stabilityScore = MutableStateFlow(100)
    val stabilityScore: StateFlow<Int> = _stabilityScore.asStateFlow()

    private val _postureStatus = MutableStateFlow("Ready")
    val postureStatus: StateFlow<String> = _postureStatus.asStateFlow()

    // INTERNAL
    private var isDown = false
    private var lastRepTime = 0L
    private var previousMagnitude = 9.8f
    private val alpha = 0.15f
    private var activeTimerJob: Job? = null
    private var restTimerJob: Job? = null
    private var isPlanking = false // Logic flag

    // Stats
    private var totalRepsSession = 0
    private var totalDurationSession = 0L

    init {
        // Initialize Voice Manager
        voiceManager = VoiceCommandManager(application) { command ->
            handleVoiceCommand(command)
        }
        startSensorListener()
    }

    // --- VOICE LOGIC ---
    fun initVoice(isPermissionGranted: Boolean) {
        if (isPermissionGranted) {
            voiceManager?.startListening()
        }
    }

    private fun handleVoiceCommand(command: String) {
        when (command) {
            "STOP" -> {
                if (_workoutState.value == WorkoutState.ACTIVE || _workoutState.value == WorkoutState.PAUSED) {
                    stopWorkoutManually()
                }
            }
            "PAUSE" -> {
                if (_workoutState.value == WorkoutState.ACTIVE) {
                    _workoutState.value = WorkoutState.PAUSED
                    stopActiveTimer()
                    _postureStatus.value = "PAUSED"
                }
            }
            "START" -> {
                if (_workoutState.value == WorkoutState.PAUSED || _workoutState.value == WorkoutState.SETUP) {
                    if (_workoutState.value == WorkoutState.SETUP) startWorkout()
                    else resumeWorkout()
                }
            }
        }
    }

    private fun stopWorkoutManually() {
        stopActiveTimer()
        _workoutState.value = WorkoutState.STOPPED
    }

    // WORKOUT ACTIONS
    fun setType(type: WorkoutType) { _currentType.value = type }

    fun startWorkout() {
        _workoutState.value = WorkoutState.ACTIVE
        _currentSet.value = 1
        resetCounters()
        totalRepsSession = 0
        totalDurationSession = 0
        startActiveTimer()
    }

    fun pauseWorkout() {
        if (_workoutState.value == WorkoutState.ACTIVE) {
            _workoutState.value = WorkoutState.PAUSED
            stopActiveTimer()
            _postureStatus.value = "PAUSED"
        }
    }

    fun resumeWorkout() {
        if (_workoutState.value == WorkoutState.PAUSED) {
            _workoutState.value = WorkoutState.ACTIVE
            startActiveTimer()
        }
    }

    fun stopWorkout() {
        stopActiveTimer()
        _workoutState.value = WorkoutState.STOPPED
    }

    fun resetToSetup() {
        activeTimerJob?.cancel()
        restTimerJob?.cancel()
        _workoutState.value = WorkoutState.SETUP
        _saveStatus.value = SaveStatus.IDLE
        resetCounters()
    }

    fun saveSessionToCloud() {
        _saveStatus.value = SaveStatus.SAVING
        viewModelScope.launch {
            try {
                val session = WorkoutSession(
                    type = getFormattedTitle(),
                    totalReps = totalRepsSession,
                    totalDuration = totalDurationSession,
                    setsCompleted = _currentSet.value,
                    stabilityAvg = _stabilityScore.value
                )
                repository.saveSession(session)
                _saveStatus.value = SaveStatus.SUCCESS
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.ERROR
            }
        }
    }

    fun getFormattedTitle(): String = _currentType.value.name.replace("_", " ")

    private fun resetCounters() {
        _currentReps.value = 0
        _currentDuration.value = 0
        _stabilityScore.value = 100
        isDown = false
        isPlanking = false
    }

    // SENSOR
    private fun startSensorListener() {
        viewModelScope.launch {
            sensorSystem.getAccelerometerData().collect { (x, y, z) ->
                if (_workoutState.value == WorkoutState.ACTIVE) {
                    val isOrientationCorrect = checkOrientation(x, y, z, _currentType.value)
                    if (isOrientationCorrect) {
                        val rawMag = sqrt(x*x + y*y + z*z)
                        val smoothedMag = (alpha * rawMag) + ((1 - alpha) * previousMagnitude)
                        previousMagnitude = smoothedMag

                        when (_currentType.value) {
                            WorkoutType.PUSH_UP, WorkoutType.SQUAT -> processRepetitionLogic(smoothedMag)
                            WorkoutType.PLANK -> processPlankLogic(x, y, z)
                        }
                    } else {
                        isPlanking = false
                    }
                }
            }
        }
    }

    private fun processRepetitionLogic(magnitude: Float) {
        isPlanking = false

        val lower = 8.5f; val upper = 11.0f
        if (!isDown && magnitude < lower) isDown = true
        if (isDown && magnitude > upper) {
            val now = System.currentTimeMillis()
            if (now - lastRepTime > 500) {
                _currentReps.value += 1
                totalRepsSession += 1
                lastRepTime = now
                isDown = false
                if (_currentReps.value >= targetReps) finishSet()
            }
        }
    }

    private fun processPlankLogic(x: Float, y: Float, z: Float) {
        isPlanking = true // Enable plank timer logic

        val shake = abs(x) + abs(y)
        if (shake > 1.5 && _stabilityScore.value > 0) {
            _stabilityScore.value -= 1
        }
        if (_currentDuration.value >= targetDuration) finishSet()
    }

    // TIMER (Unified)
    private fun startActiveTimer() {
        activeTimerJob?.cancel()
        activeTimerJob = viewModelScope.launch {
            while (_workoutState.value == WorkoutState.ACTIVE) {
                delay(1000)
                _currentDuration.value += 1
                totalDurationSession += 1
            }
        }
    }

    private fun stopActiveTimer() {
        activeTimerJob?.cancel()
    }

    private fun finishSet() {
        stopActiveTimer()
        if (_currentSet.value < targetSets) startRestTimer()
        else {
            _workoutState.value = WorkoutState.FINISHED
        }
    }

    private fun startRestTimer() {
        _workoutState.value = WorkoutState.RESTING
        _restTimer.value = restTimeSeconds
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (_restTimer.value > 0) {
                delay(1000)
                _restTimer.value -= 1
            }
            nextSet()
        }
    }

    private fun nextSet() {
        _currentSet.value += 1
        resetCounters()
        _workoutState.value = WorkoutState.ACTIVE
        startActiveTimer()
    }

    // HELPER
    private fun checkOrientation(x: Float, y: Float, z: Float, type: WorkoutType): Boolean {
        return when (type) {
            WorkoutType.PUSH_UP -> if (abs(z) > 7.0) { _postureStatus.value = "Correct Orientation"; true } else { _postureStatus.value = "Place phone flat"; false }
            WorkoutType.SQUAT -> if (abs(y) > 7.0) { _postureStatus.value = "Correct Orientation"; true } else { _postureStatus.value = "Phone in pocket (Vertical)"; false }
            WorkoutType.PLANK -> if (abs(z) > 7.0) { _postureStatus.value = "Correct Orientation"; true } else { _postureStatus.value = "Place phone flat"; false }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.destroy()
    }
}