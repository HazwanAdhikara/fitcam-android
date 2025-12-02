package com.example.fitcam.ui.screens.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitcam.data.SensorSystem
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
enum class WorkoutState { SETUP, ACTIVE, RESTING, FINISHED }
enum class SaveStatus { IDLE, SAVING, SUCCESS, ERROR }

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorSystem = SensorSystem(application)
    private val repository = WorkoutRepository()

    // --- CONFIGURATION (Targets) ---
    var targetSets = 3
    var targetReps = 10         // For Pushup/Squat
    var targetDuration = 30L    // For Plank (seconds)
    var restTimeSeconds = 30    // Break time

    // --- STATE FLOWS ---
    private val _workoutState = MutableStateFlow(WorkoutState.SETUP)
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    private val _currentType = MutableStateFlow(WorkoutType.PUSH_UP)
    val currentType: StateFlow<WorkoutType> = _currentType.asStateFlow()

    // Progress Data
    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet.asStateFlow()

    private val _currentReps = MutableStateFlow(0)
    val currentReps: StateFlow<Int> = _currentReps.asStateFlow()

    private val _currentDuration = MutableStateFlow(0L) // Active Timer (Plank)
    val currentDuration: StateFlow<Long> = _currentDuration.asStateFlow()

    private val _restTimer = MutableStateFlow(0) // Rest Countdown
    val restTimer: StateFlow<Int> = _restTimer.asStateFlow()

    private val _stabilityScore = MutableStateFlow(100)
    val stabilityScore: StateFlow<Int> = _stabilityScore.asStateFlow()

    private val _postureStatus = MutableStateFlow("Ready")
    val postureStatus: StateFlow<String> = _postureStatus.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.IDLE)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    // --- INTERNAL LOGIC ---
    private var isDown = false
    private var lastRepTime = 0L
    private var previousMagnitude = 9.8f
    private val alpha = 0.15f
    private var activeTimerJob: Job? = null
    private var restTimerJob: Job? = null
    private var isPlanking = false

    // Stats for saving
    private var totalRepsSession = 0
    private var totalDurationSession = 0L

    init {
        startSensorListener()
    }

    // --- USER ACTIONS ---
    fun setType(type: WorkoutType) {
        _currentType.value = type
    }

    fun startWorkout() {
        _workoutState.value = WorkoutState.ACTIVE
        _currentSet.value = 1
        resetCounters()
        totalRepsSession = 0
        totalDurationSession = 0
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
                e.printStackTrace()
                _saveStatus.value = SaveStatus.ERROR
            }
        }
    }

    // Update resetToSetup to also clear the save status
    fun resetToSetup() {
        activeTimerJob?.cancel()
        restTimerJob?.cancel()
        _workoutState.value = WorkoutState.SETUP
        _saveStatus.value = SaveStatus.IDLE // Reset status
        resetCounters()
    }


    fun getFormattedTitle(): String {
        return _currentType.value.name.replace("_", " ")
    }

    private fun resetCounters() {
        _currentReps.value = 0
        _currentDuration.value = 0
        _stabilityScore.value = 100
        isDown = false
        isPlanking = false
    }

    // --- SENSOR LOGIC ---
    private fun startSensorListener() {
        viewModelScope.launch {
            sensorSystem.getAccelerometerData().collect { (x, y, z) ->
                // Only process sensors if we are ACTIVE
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
                        if (isPlanking) stopActiveTimer()
                    }
                }
            }
        }
    }

    // --- CORE LOGIC: REPS ---
    private fun processRepetitionLogic(magnitude: Float) {
        if (isPlanking) stopActiveTimer()

        val lowerThresh = 8.5f
        val upperThresh = 11.0f

        if (!isDown && magnitude < lowerThresh) isDown = true

        if (isDown && magnitude > upperThresh) {
            val now = System.currentTimeMillis()
            if (now - lastRepTime > 500) {
                // REP COUNTED!
                _currentReps.value += 1
                totalRepsSession += 1
                lastRepTime = now
                isDown = false

                // CHECK TARGET
                if (_currentReps.value >= targetReps) {
                    finishSet()
                }
            }
        }
    }

    // --- CORE LOGIC: PLANK ---
    private fun processPlankLogic(x: Float, y: Float, z: Float) {
        if (!isPlanking) startActiveTimer() // Start counting seconds

        // Stability logic
        val shake = abs(x) + abs(y)
        if (shake > 1.5 && _stabilityScore.value > 0) {
            _stabilityScore.value -= 1
        }

        // CHECK TARGET (Handled inside timer coroutine)
        if (_currentDuration.value >= targetDuration) {
            finishSet()
        }
    }

    // --- TIMERS ---
    private fun startActiveTimer() {
        if (isPlanking) return
        isPlanking = true
        activeTimerJob?.cancel()
        activeTimerJob = viewModelScope.launch {
            while (isPlanking && _workoutState.value == WorkoutState.ACTIVE) {
                delay(1000)
                _currentDuration.value += 1
                totalDurationSession += 1

                // Double check for plank target
                if (_currentType.value == WorkoutType.PLANK &&
                    _currentDuration.value >= targetDuration) {
                    finishSet()
                }
            }
        }
    }

    private fun stopActiveTimer() {
        isPlanking = false
        activeTimerJob?.cancel()
    }

    // --- WORKOUT FLOW MANAGEMENT ---
    private fun finishSet() {
        stopActiveTimer()

        if (_currentSet.value < targetSets) {
            // GO TO REST
            startRestTimer()
        } else {
            // WORKOUT COMPLETE
            _workoutState.value = WorkoutState.FINISHED
            // NOTE: We do NOT auto-save here anymore. We wait for user input.
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
            // Rest Over -> Next Set
            nextSet()
        }
    }

    private fun nextSet() {
        _currentSet.value += 1
        resetCounters()
        _workoutState.value = WorkoutState.ACTIVE
    }

    // Helper helper
    private fun checkOrientation(x: Float, y: Float, z: Float, type: WorkoutType): Boolean {
        return when (type) {
            WorkoutType.PUSH_UP -> if (abs(z) > 7.0) { _postureStatus.value = "Correct Orientation"; true } else { _postureStatus.value = "Place phone flat"; false }
            WorkoutType.SQUAT -> if (abs(y) > 7.0) { _postureStatus.value = "Correct Orientation"; true } else { _postureStatus.value = "Phone in pocket (Vertical)"; false }
            WorkoutType.PLANK -> if (abs(z) > 7.0) { _postureStatus.value = "Correct Orientation"; true } else { _postureStatus.value = "Place phone flat"; false }
        }
    }
}