package com.example.fitcam.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceCommandManager(context: Context, private val onCommand: (String) -> Unit) {

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    private var isListening = false

    fun startListening() {
        if (isListening) return
        try {
            isListening = true
            speechRecognizer.setRecognitionListener(listener)
            speechRecognizer.startListening(speechIntent)
            Log.d("VoiceCommand", "Started Listening")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        isListening = false
        try {
            speechRecognizer.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            speechRecognizer.destroy()
        } catch (e: Exception) { }
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val command = matches[0].lowercase()
                Log.d("VoiceCommand", "Heard: $command")

                // Simple keyword matching
                if (command.contains("stop") || command.contains("finish")) {
                    onCommand("STOP")
                } else if (command.contains("pause") || command.contains("wait") || command.contains("hold")) {
                    onCommand("PAUSE")
                } else if (command.contains("start") || command.contains("go") || command.contains("resume")) {
                    onCommand("START")
                }
            }
            // Restart listening loop
            restartListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Optional: Handle immediate words for faster response
        }

        override fun onError(error: Int) {
            // Restart on error (usually "No match" or "Speech timeout")
            if (isListening) restartListening()
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun restartListening() {
        if (isListening) {
            try {
                speechRecognizer.startListening(speechIntent)
            } catch (e: Exception) {}
        }
    }
}