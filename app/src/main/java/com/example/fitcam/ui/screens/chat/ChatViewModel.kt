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

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(text = "Hello! I'm FitCam Coach. Ready to workout?", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(text = userText, isUser = true)
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val history = repository.getSessions()
                
                val systemInstruction = AIContextHelper.buildSystemPrompt(history)

                val fullPrompt = "$systemInstruction\n\nUSER QUESTION: $userText"
                
                val response = generativeModel.generateContent(fullPrompt)
                
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