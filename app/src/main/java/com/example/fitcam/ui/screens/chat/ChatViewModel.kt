package com.example.fitcam.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitcam.BuildConfig
import com.example.fitcam.data.WorkoutRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel : ViewModel() {

    private val repository = WorkoutRepository()

    // Gemini Model Setup
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // UI State
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(text = "Hello! I'm FitCam Coach. Ready to workout?", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // 1. Add User Message immediately
        val userMsg = ChatMessage(text = userText, isUser = true)
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 2. Fetch History from Firebase
                val history = repository.getSessions()
                
                // 3. Build the "System Prompt" with context
                val systemInstruction = AIContextHelper.buildSystemPrompt(history)
                
                // 4. Send to Gemini
                // We combine system prompt + user question into one block for the API
                val fullPrompt = "$systemInstruction\n\nUSER QUESTION: $userText"
                
                val response = generativeModel.generateContent(fullPrompt)
                
                // 5. Add AI Response
                val aiText = response.text ?: "I'm having trouble thinking right now. Try again."
                val aiMsg = ChatMessage(text = aiText, isUser = false)
                
                _messages.value = _messages.value + aiMsg

            } catch (e: Exception) {
                e.printStackTrace()
                _messages.value = _messages.value + ChatMessage(text = "Error: ${e.localizedMessage}", isUser = false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}