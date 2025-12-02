package com.example.fitcam.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Workout : Screen("workout", "Workout", Icons.Default.FitnessCenter)
    object Community : Screen("community", "Community", Icons.Default.Group)
    object AIChat : Screen("ai_chat", "AI Coach", Icons.Default.Chat)
    object History : Screen("history", "History", Icons.Default.List)
}