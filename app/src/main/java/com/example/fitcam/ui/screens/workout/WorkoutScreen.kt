package com.example.fitcam.ui.screens.workout

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fitcam.ui.navigation.Screen
import com.example.fitcam.ui.theme.*

@Composable
fun WorkoutScreen(navController: NavController, viewModel: WorkoutViewModel = viewModel()) {
    val state by viewModel.workoutState.collectAsState()
    val type by viewModel.currentType.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.initVoice(isGranted)
    }

    // Request on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Box(modifier = Modifier.fillMaxSize().background(FitCamCream)) {
        when (state) {
            WorkoutState.SETUP -> SetupView(viewModel, type, navController)
            WorkoutState.ACTIVE, WorkoutState.PAUSED -> ActiveView(viewModel, type, state)
            WorkoutState.RESTING -> RestView(viewModel)
            WorkoutState.FINISHED -> FinishedView(viewModel, isStopped = false)
            WorkoutState.STOPPED -> FinishedView(viewModel, isStopped = true)
        }

        if (saveStatus == SaveStatus.SUCCESS) {
            SuccessDialog(onDismiss = { viewModel.resetToSetup() })
        }
        if (saveStatus == SaveStatus.SAVING) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FitCamYellow)
            }
        }
    }
}

@Composable
fun SetupView(viewModel: WorkoutViewModel, type: WorkoutType, navController: NavController) {

    var setsInput by remember { mutableStateOf(viewModel.targetSets.toString()) }
    var repsInput by remember { mutableStateOf(viewModel.targetReps.toString()) }
    var durationInput by remember { mutableStateOf(viewModel.targetDuration.toString()) }
    var restInput by remember { mutableStateOf(viewModel.restTimeSeconds.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("FITCAM TRAINER", style = MaterialTheme.typography.headlineLarge, color = FitCamBlue, fontWeight = FontWeight.Black)
            IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                Icon(Icons.Default.History, "History", tint = FitCamBlue, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            WorkoutType.values().forEach { t ->
                val isSelected = type == t
                Card(
                    onClick = { viewModel.setType(t) },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) FitCamYellow else Color.White),
                    border = BorderStroke(2.dp, FitCamBlue)
                ) {
                    Text(t.name.replace("_", " "), Modifier.padding(12.dp, 8.dp), color = FitCamBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Customize Your Session", fontWeight = FontWeight.Bold, color = FitCamBlue)
        Spacer(modifier = Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FunInput(
                value = setsInput,
                onValueChange = {
                    setsInput = it
                    viewModel.targetSets = it.toIntOrNull() ?: 3
                },
                label = "Sets",
                modifier = Modifier.weight(1f)
            )
            FunInput(
                value = restInput,
                onValueChange = {
                    restInput = it
                    viewModel.restTimeSeconds = it.toIntOrNull() ?: 30
                },
                label = "Rest (s)",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (type == WorkoutType.PLANK) {
            FunInput(
                value = durationInput,
                onValueChange = {
                    durationInput = it
                    viewModel.targetDuration = it.toLongOrNull() ?: 30L
                },
                label = "Duration (s)",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            FunInput(
                value = repsInput,
                onValueChange = {
                    repsInput = it
                    viewModel.targetReps = it.toIntOrNull() ?: 10
                },
                label = "Target Reps",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Microphone Hint
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mic, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Voice: Start, Stop, Pause", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.startWorkout()
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue)
        ) {
            Text("START WORKOUT", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActiveView(viewModel: WorkoutViewModel, type: WorkoutType, state: WorkoutState) {
    val currentReps by viewModel.currentReps.collectAsState()
    val currentDuration by viewModel.currentDuration.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val status by viewModel.postureStatus.collectAsState()
    val stability by viewModel.stabilityScore.collectAsState()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR (Set Info & Timer)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Set $currentSet / ${viewModel.targetSets}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(color = FitCamBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(8.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = FitCamBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(formatTime(currentDuration), color = FitCamBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(viewModel.getFormattedTitle(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = FitCamBlue)
            Spacer(modifier = Modifier.height(16.dp))

            val statusColor = if (status.contains("Correct") || status.contains("PAUSED")) Color(0xFF4CAF50) else FitCamRed
            Card(colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)), border = BorderStroke(1.dp, statusColor)) {
                Text(status, color = statusColor, modifier = Modifier.padding(16.dp, 8.dp), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // MAIN COUNTER
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(280.dp),
                    shape = RoundedCornerShape(100),
                    color = FitCamYellow.copy(alpha = 0.3f),
                    border = BorderStroke(4.dp, FitCamYellow)
                ) {}
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (type == WorkoutType.PLANK) {
                        Text("${currentDuration}s", fontSize = 90.sp, fontWeight = FontWeight.Black, color = FitCamBlue)
                        Text("/ ${viewModel.targetDuration}s", fontSize = 24.sp, color = FitCamBlue)
                    } else {
                        Text("$currentReps", fontSize = 120.sp, fontWeight = FontWeight.Black, color = FitCamBlue)
                        Text("REPS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FitCamBlue)
                    }
                }
            }

            if (type == WorkoutType.PLANK) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Stability: $stability%", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { stability / 100f }, modifier = Modifier.fillMaxWidth(0.6f).height(12.dp), color = if(stability > 50) FitCamBlue else FitCamRed)
            }
            Spacer(modifier = Modifier.weight(1f))

            // MICROPHONE STATUS
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, null, tint = if(state == WorkoutState.ACTIVE) FitCamRed else Color.Gray, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if(state == WorkoutState.ACTIVE) "Listening..." else "Mic Paused", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MANUAL CONTROL BUTTONS
            if (state == WorkoutState.ACTIVE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // STOP Button
                    Button(
                        onClick = { viewModel.stopWorkout() },
                        colors = ButtonDefaults.buttonColors(containerColor = FitCamRed),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STOP")
                    }

                    // PAUSE Button
                    Button(
                        onClick = { viewModel.pauseWorkout() },
                        colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PAUSE")
                    }
                }
            }
        }

        if (state == WorkoutState.PAUSED) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.PauseCircle, null, tint = FitCamYellow, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("WORKOUT PAUSED", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("Take a breathe first..", color = Color.Gray)

                    Spacer(modifier = Modifier.height(48.dp))

                    // RESUME BUTTON
                    Button(
                        onClick = { viewModel.resumeWorkout() },
                        colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue),
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESUME SESSION", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // STOP BUTTON (In Pause Menu)
                    OutlinedButton(
                        onClick = { viewModel.stopWorkout() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FitCamRed),
                        border = BorderStroke(2.dp, FitCamRed),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.Stop, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("END WORKOUT")
                    }
                }
            }
        }
    }
}

@Composable
fun RestView(viewModel: WorkoutViewModel) {
    val timeLeft by viewModel.restTimer.collectAsState()
    Box(Modifier.fillMaxSize().background(FitCamBlue), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("REST TIME", color = FitCamYellow, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Text("$timeLeft", color = Color.White, fontSize = 140.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Get Ready for Next Set!", color = FitCamCream, fontSize = 20.sp)
        }
    }
}

@Composable
fun FinishedView(viewModel: WorkoutViewModel, isStopped: Boolean) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {

        val icon = if (isStopped) Icons.Default.PriorityHigh else Icons.Default.CheckCircle
        val titleText = if (isStopped) "YOU'RE ALMOST\nDONE!" else "WORKOUT\nCOMPLETE!"
        val subText = if (isStopped) "Keep going! Don't give up!" else "Great job! Do you want to save this progress?"
        val iconColor = if (isStopped) FitCamYellow else FitCamBlue

        Icon(icon, null, tint = iconColor, modifier = Modifier.size(120.dp))

        Text(titleText, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, color = FitCamBlue)
        Spacer(modifier = Modifier.height(16.dp))
        Text(subText, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { viewModel.saveSessionToCloud() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE PROGRESS")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { viewModel.resetToSetup() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(2.dp, FitCamRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FitCamRed)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(modifier = Modifier.width(8.dp))
            // Text logic
            val discardText = if (isStopped) "DISCARD & RETRY" else "DISCARD"
            Text(discardText)
        }
    }
}

@Composable
fun SuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp)) },
        title = { Text("Progress Saved!", style = MaterialTheme.typography.headlineSmall, color = FitCamBlue, fontWeight = FontWeight.Bold) },
        text = { Text("Your workout data has been securely stored in the cloud.", textAlign = TextAlign.Center, color = Color.Gray) },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue), modifier = Modifier.fillMaxWidth()) {
                Text("AWESOME", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun FunInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FitCamBlue, unfocusedBorderColor = FitCamBlue.copy(alpha = 0.5f), focusedLabelColor = FitCamBlue, cursorColor = FitCamRed),
        shape = RoundedCornerShape(12.dp)
    )
}

fun formatTime(seconds: Long): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}