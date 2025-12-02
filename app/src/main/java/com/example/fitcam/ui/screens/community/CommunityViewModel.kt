package com.example.fitcam.ui.screens.community

import androidx.lifecycle.ViewModel
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
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class CommunityViewModel : ViewModel() {
    private val repository = WorkoutRepository()

    // Data sesi terakhir untuk overlay
    private val _lastSession = MutableStateFlow<WorkoutSession?>(null)
    val lastSession: StateFlow<WorkoutSession?> = _lastSession.asStateFlow()

    init {
        fetchLastWorkout()
    }

    private fun fetchLastWorkout() {
        viewModelScope.launch {
            val sessions = repository.getSessions()
            if (sessions.isNotEmpty()) {
                // Ambil yang paling atas (terbaru)
                _lastSession.value = sessions.first()
            }
        }
    }

    // Helper untuk format teks overlay ala Strava
    fun getOverlayText(session: WorkoutSession): String {
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(session.timestamp))
        return if (session.type.contains("PLANK", true)) {
            "${session.type}\n${session.totalDuration} Seconds\nStability: ${session.stabilityAvg}%\n$date"
        } else {
            "${session.type}\n${session.totalReps} Reps\n${session.setsCompleted} Sets\n$date"
        }
    }

    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uploadStatus = MutableStateFlow("Idle") // Idle, Uploading, Success, Error
    val uploadStatus: StateFlow<String> = _uploadStatus.asStateFlow()

    fun uploadPost(bitmap: Bitmap, session: WorkoutSession) {
        _uploadStatus.value = "Uploading"

        val filename = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("community_posts/$filename")

        // 1. Convert Bitmap to ByteArray
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        // 2. Upload to Firebase Storage
        val uploadTask = storageRef.putBytes(data)

        uploadTask.addOnSuccessListener {
            // 3. Get Download URL
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                savePostToFirestore(uri.toString(), session)
            }
        }.addOnFailureListener {
            _uploadStatus.value = "Error: ${it.message}"
        }
    }

    private fun savePostToFirestore(imageUrl: String, session: WorkoutSession) {
        val postData = hashMapOf(
            "imageUrl" to imageUrl,
            "workoutType" to session.type,
            "stats" to getOverlayText(session),
            "timestamp" to System.currentTimeMillis()
            // Nanti bisa tambah userId, username, dll
        )

        db.collection("posts").add(postData)
            .addOnSuccessListener { _uploadStatus.value = "Success" }
            .addOnFailureListener { _uploadStatus.value = "Error DB: ${it.message}" }
    }
    
    fun resetStatus() {
        _uploadStatus.value = "Idle"
    }

}