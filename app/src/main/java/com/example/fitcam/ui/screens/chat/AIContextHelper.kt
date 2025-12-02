package com.example.fitcam.ui.screens.chat

import com.example.fitcam.data.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AIContextHelper {

    fun buildSystemPrompt(history: List<WorkoutSession>): String {
        val historyText = if (history.isEmpty()) {
            "User has no workout history yet."
        } else {
            // Take last 10 sessions to save tokens
            history.take(10).joinToString("\n") { session ->
                formatSession(session)
            }
        }

        return """
            You are FitCam Coach, an energetic and professional personal trainer AI.
            
            USER WORKOUT HISTORY (Last 10 sessions):
            $historyText
            
            INSTRUCTIONS:
            1. Use the history above to give personalized advice.
            2. If the user's stability score is low (<50%), warn them about form.
            3. Be motivating but strict. Use the tone of a supportive gym bro.
            4. Keep answers short and concise (max 3 sentences usually).
            5. If asked about progress, summarize their recent stats.
        """.trimIndent()
    }

    private fun formatSession(s: WorkoutSession): String {
        val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(s.timestamp))
        return if (s.type == "PLANK") {
            "- $date: PLANK for ${s.totalDuration}s (Stability: ${s.stabilityAvg}%)"
        } else {
            "- $date: ${s.type} for ${s.totalReps} reps in ${s.setsCompleted} sets"
        }
    }
}