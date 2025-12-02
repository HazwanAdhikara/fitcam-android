package com.example.fitcam.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.fitcam.ui.navigation.Screen
import com.example.fitcam.ui.screens.workout.WorkoutScreen
import com.example.fitcam.ui.screens.community.CommunityScreen
import com.example.fitcam.ui.screens.chat.ChatScreen
import com.example.fitcam.ui.screens.history.HistoryScreen

@Composable
fun FitCamApp() {
    val navController = rememberNavController()

    val items = listOf(
        Screen.Workout,
        Screen.Community,
        Screen.AIChat
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Workout.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Workout.route) { WorkoutScreen(navController) }
            composable(Screen.Community.route) { CommunityScreen() }
            composable(Screen.AIChat.route) { ChatScreen() }
            composable(Screen.Workout.route) { WorkoutScreen(navController) }
            composable(Screen.History.route) { HistoryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}