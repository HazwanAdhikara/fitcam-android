package com.example.fitcam.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitcam.data.WorkoutRepository
import com.example.fitcam.data.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository()

    // Raw data from DB
    private var allSessions = listOf<WorkoutSession>()

    // Filtered data for UI
    private val _uiSessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val uiSessions: StateFlow<List<WorkoutSession>> = _uiSessions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Filters
    private val _selectedType = MutableStateFlow("All") // "All", "PUSH UP", etc.
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _selectedDate = MutableStateFlow<Long?>(null) // Null = All time
    val selectedDate: StateFlow<Long?> = _selectedDate.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            allSessions = repository.getSessions()
            applyFilters()
            _isLoading.value = false
        }
    }

    fun setTypeFilter(type: String) {
        _selectedType.value = type
        applyFilters()
    }

    fun setDateFilter(millis: Long?) {
        _selectedDate.value = millis
        applyFilters()
    }

    private fun applyFilters() {
        var result = allSessions

        // 1. Filter by Type
        if (_selectedType.value != "All") {
            result = result.filter { it.type.equals(_selectedType.value, ignoreCase = true) }
        }

        // 2. Filter by Date
        _selectedDate.value?.let { filterDate ->
            val formatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val filterString = formatter.format(Date(filterDate))

            result = result.filter {
                val itemString = formatter.format(Date(it.timestamp))
                itemString == filterString
            }
        }

        _uiSessions.value = result
    }
}