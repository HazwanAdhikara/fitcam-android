package com.example.fitcam.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class WorkoutSession(
    val type: String = "",
    val totalReps: Int = 0,
    val totalDuration: Long = 0L,
    val setsCompleted: Int = 0,
    val stabilityAvg: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class WorkoutRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveSession(session: WorkoutSession) {
        try {
            db.collection("workouts").add(session).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getSessions(): List<WorkoutSession> {
        return try {
            val snapshot = db.collection("workouts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            // Convert Firestore documents to Objects
            snapshot.toObjects(WorkoutSession::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

