package com.example.fitcam.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitcam.data.WorkoutSession
import com.example.fitcam.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val sessions by viewModel.uiSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDateFilter(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK", color = FitCamBlue) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setDateFilter(null) // Clear date filter
                    showDatePicker = false
                }) { Text("CLEAR", color = FitCamRed) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History", fontWeight = FontWeight.Bold, color = FitCamBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FitCamBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FitCamCream)
            )
        },
        containerColor = FitCamCream
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- FILTERS ---
            Column(Modifier.background(FitCamCream).padding(vertical = 8.dp)) {
                // Type Filters
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("All", "PUSH UP", "SQUAT", "PLANK")
                    types.forEach { type ->
                        val isSelected = selectedType.equals(type, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTypeFilter(type) },
                            label = { Text(type.replace("_", " ")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FitCamYellow,
                                selectedLabelColor = FitCamBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = FitCamBlue,
                                borderWidth = 1.dp,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                // Date Filter Button
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, FitCamBlue),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = FitCamBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val label = if (selectedDate != null) formatDate(selectedDate!!) else "Select Date"
                        Text(label, color = FitCamBlue)
                    }

                    if (selectedDate != null) {
                        IconButton(onClick = { viewModel.setDateFilter(null) }) {
                            Icon(Icons.Default.Close, null, tint = FitCamRed)
                        }
                    }
                }
            }

            // --- LIST ---
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FitCamYellow)
                }
            } else if (sessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No workouts found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions) { session ->
                        HistoryItem(session)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(session: WorkoutSession) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, FitCamBlue.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Background
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = FitCamYellow.copy(alpha = 0.2f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FitnessCenter, null, tint = FitCamBlue)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = session.type.uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = FitCamBlue,
                    fontSize = 16.sp
                )
                Text(
                    text = formatDateTime(session.timestamp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats Row
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatBadge("Sets", "${session.setsCompleted}")
                    if (session.type.contains("PLANK", ignoreCase = true)) {
                        StatBadge("Time", "${session.totalDuration}s")
                        StatBadge("Stability", "${session.stabilityAvg}%")
                    } else {
                        StatBadge("Reps", "${session.totalReps}")
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.SemiBold, color = FitCamBlue)
    }
}

// Helpers
fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun formatDateTime(millis: Long): String {
    // Example: "Wednesday, 03 Dec • 14:30"
    val formatter = SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}