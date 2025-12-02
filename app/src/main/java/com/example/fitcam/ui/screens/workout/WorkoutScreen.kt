package com.example.fitcam.ui.screens.workout

import androidx.navigation.NavController
import androidx.compose.material.icons.filled.History
import com.example.fitcam.ui.navigation.Screen
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitcam.ui.theme.FitCamBlue
import com.example.fitcam.ui.theme.FitCamCream
import com.example.fitcam.ui.theme.FitCamRed
import com.example.fitcam.ui.theme.FitCamYellow
import androidx.compose.ui.window.Dialog

@Composable
fun WorkoutScreen(navController: NavController, viewModel: WorkoutViewModel = viewModel()) {
    val state by viewModel.workoutState.collectAsState()
    val type by viewModel.currentType.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState() // <--- Observe Save Status

    Box(modifier = Modifier.fillMaxSize().background(FitCamCream)) {
        when (state) {
            WorkoutState.SETUP -> SetupView(viewModel, type, navController)
            WorkoutState.ACTIVE -> ActiveView(viewModel, type)
            WorkoutState.RESTING -> RestView(viewModel)
            WorkoutState.FINISHED -> FinishedView(viewModel)
        }

        // --- POP UP LOGIC ---
        if (saveStatus == SaveStatus.SUCCESS) {
            SuccessDialog(
                onDismiss = { viewModel.resetToSetup() }
            )
        }

        // Optional: Loading Indicator
        if (saveStatus == SaveStatus.SAVING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FitCamYellow)
            }
        }
    }
}

// --- 1. SETUP VIEW (INPUT FIELDS) ---
@Composable
fun SetupView(viewModel: WorkoutViewModel, type: WorkoutType, navController: NavController) {
    // Text Input States (String)
    var setsInput by remember { mutableStateOf("3") }
    var repsInput by remember { mutableStateOf("10") }
    var durationInput by remember { mutableStateOf("30") }
    var restInput by remember { mutableStateOf("30") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        Text(
            "LET'S DO SOME EXERCISE",
            style = MaterialTheme.typography.headlineLarge,
            color = FitCamBlue,
            fontWeight = FontWeight.Black
        )
            IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = FitCamBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Fun Custom Chip Selector
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            WorkoutType.values().forEach { t ->
                val isSelected = type == t
                Card(
                    onClick = { viewModel.setType(t) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) FitCamYellow else Color.White
                    ),
                    border = BorderStroke(2.dp, FitCamBlue)
                ) {
                    Text(
                        text = t.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = FitCamBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // INPUT FIELDS
        Text("Customize Your Session", fontWeight = FontWeight.Bold, color = FitCamBlue)
        Spacer(modifier = Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FunInput(value = setsInput, onValueChange = { setsInput = it }, label = "Sets", modifier = Modifier.weight(1f))
            FunInput(value = restInput, onValueChange = { restInput = it }, label = "Rest (s)", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (type == WorkoutType.PLANK) {
            FunInput(value = durationInput, onValueChange = { durationInput = it }, label = "Duration (s)", modifier = Modifier.fillMaxWidth())
        } else {
            FunInput(value = repsInput, onValueChange = { repsInput = it }, label = "Target Reps", modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                // Convert Strings to Int/Long with safe fallbacks
                viewModel.targetSets = setsInput.toIntOrNull() ?: 3
                viewModel.targetReps = repsInput.toIntOrNull() ?: 10
                viewModel.targetDuration = durationInput.toLongOrNull() ?: 30L
                viewModel.restTimeSeconds = restInput.toIntOrNull() ?: 30
                viewModel.startWorkout()
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue)
        ) {
            Text("START WORKOUT", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// --- 2. ACTIVE VIEW (BIG & BOLD) ---
@Composable
fun ActiveView(viewModel: WorkoutViewModel, type: WorkoutType) {
    val currentReps by viewModel.currentReps.collectAsState()
    val currentDuration by viewModel.currentDuration.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val status by viewModel.postureStatus.collectAsState()
    val stability by viewModel.stabilityScore.collectAsState()

    val statusColor = if (status == "Correct Orientation") Color(0xFF4CAF50) else FitCamRed

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Set $currentSet / ${viewModel.targetSets}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Cancel Button
            IconButton(onClick = { viewModel.resetToSetup() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Cancel", tint = FitCamRed)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(viewModel.getFormattedTitle(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = FitCamBlue)

        Spacer(modifier = Modifier.height(16.dp))

        // Posture Card
        Card(
            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, statusColor)
        ) {
            Text(status, color = statusColor, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1f))

        // BIG COUNTER
        Box(contentAlignment = Alignment.Center) {
            // Yellow Circle Background
            Surface(
                modifier = Modifier.size(280.dp),
                shape = RoundedCornerShape(100),
                color = FitCamYellow.copy(alpha = 0.3f),
                border = BorderStroke(4.dp, FitCamYellow)
            ) {}

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (type == WorkoutType.PLANK) {
                    Text(
                        "${currentDuration}s",
                        fontSize = 90.sp,
                        fontWeight = FontWeight.Black,
                        color = FitCamBlue
                    )
                    Text("/ ${viewModel.targetDuration}s", fontSize = 24.sp, color = FitCamBlue)
                } else {
                    Text(
                        "$currentReps",
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black,
                        color = FitCamBlue
                    )
                    Text("REPS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FitCamBlue)
                }
            }
        }

        if (type == WorkoutType.PLANK) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Stability: $stability%", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { stability / 100f },
                modifier = Modifier.fillMaxWidth(0.6f).height(12.dp),
                color = if(stability > 50) FitCamBlue else FitCamRed,
                trackColor = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// --- 3. REST VIEW ---
@Composable
fun RestView(viewModel: WorkoutViewModel) {
    val timeLeft by viewModel.restTimer.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(FitCamBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("REST TIME", color = FitCamYellow, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Text("$timeLeft", color = Color.White, fontSize = 140.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Get Ready for Next Set!", color = FitCamCream, fontSize = 20.sp)
        }
    }
}

// --- 4. FINISHED VIEW (SAVE vs DISCARD) ---
@Composable
fun FinishedView(viewModel: WorkoutViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = FitCamBlue,
            modifier = Modifier.size(120.dp)
        )

        Text(
            "WORKOUT\nCOMPLETE!",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black,
            color = FitCamBlue
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Great job! Do you want to save this progress?", color = Color.Gray)

        Spacer(modifier = Modifier.height(48.dp))

        // SAVE BUTTON
        Button(
            onClick = {
                viewModel.saveSessionToCloud()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE PROGRESS")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RETRY / DISCARD BUTTON
        OutlinedButton(
            onClick = { viewModel.resetToSetup() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(2.dp, FitCamRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FitCamRed)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("DISCARD & RETRY")
        }
    }
}

// --- REUSABLE COMPONENTS ---
@Composable
fun FunInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            // Only allow numeric input
            if (it.all { char -> char.isDigit() }) {
                onValueChange(it)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FitCamBlue,
            unfocusedBorderColor = FitCamBlue.copy(alpha = 0.5f),
            focusedLabelColor = FitCamBlue,
            cursorColor = FitCamRed
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun SuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Prevent clicking outside to close, force button click */ },
        containerColor = Color.White,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50), // Success Green
                modifier = Modifier.size(64.dp)
            )
        },
        title = {
            Text(
                text = "Progress Saved!",
                style = MaterialTheme.typography.headlineSmall,
                color = FitCamBlue,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Your workout data has been securely stored in the cloud.",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AWESOME", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}