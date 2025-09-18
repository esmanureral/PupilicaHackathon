package com.esmanureral.pupilicahackathon.ui.chat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.esmanureral.pupilicahackathon.R

class SpeechRecognizerManager(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit,
    private val onPartialTextRecognized: (String) -> Unit,
    private val onSpeechError: (String) -> Unit,
    private val onPermissionStatusChanged: (Boolean) -> Unit,
    private val onVoiceButtonStateChanged: (isListening: Boolean, color: Int, scale: Float) -> Unit,
    private val onAnimationStateChanged: (shouldStart: Boolean, shouldStop: Boolean) -> Unit
) {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private var isListening = false
    private var isPermissionGranted = false

    fun initialize() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechIntent = createSpeechIntent()
        speechRecognizer.setRecognitionListener(createRecognitionListener())
        checkPermissionStatus()
    }

    fun checkPermissionStatus() {
        isPermissionGranted = checkAudioPermission()
        onPermissionStatusChanged(isPermissionGranted)
        updateVoiceButtonState(false)
    }

    fun onPermissionGranted() {
        isPermissionGranted = true
        onPermissionStatusChanged(true)
        startListening()
    }

    fun onPermissionDenied() {
        isPermissionGranted = false
        onPermissionStatusChanged(false)
        updateVoiceButtonState(false)
    }

    fun onVoiceButtonClicked() {
        if (isPermissionGranted || checkAudioPermission()) {
            startListening()
        } else {
            onPermissionStatusChanged(false)
        }
    }

    fun stopListeningIfActive() {
        if (isListening) {
            stopListening()
        }
    }

    fun destroy() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }

    private fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, context.getString(R.string.speech_language_code))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) = updateVoiceButtonScale(rmsdB)
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() = stopListening()
        override fun onError(error: Int) = handleSpeechError(error)
        override fun onResults(results: Bundle?) = handleResults(results)
        override fun onPartialResults(partialResults: Bundle?) =
            handlePartialResults(partialResults)

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startListening() {
        if (!isListening && ::speechRecognizer.isInitialized) {
            try {
                isListening = true
                speechRecognizer.startListening(speechIntent)
                updateVoiceButtonState(true)
            } catch (e: Exception) {
                isListening = false
                updateVoiceButtonState(false)
            }
        }
    }

    private fun stopListening() {
        if (isListening && ::speechRecognizer.isInitialized) {
            try {
                speechRecognizer.stopListening()
            } catch (e: Exception) {
            }
        }
        isListening = false
        updateVoiceButtonState(false)
    }

    private fun handlePartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partialText = matches[0]
            if (partialText.isNotEmpty()) {
                onPartialTextRecognized(partialText)
            }
        }
    }
    
    private fun handleSpeechError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.speech_audio_error)
            SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.speech_client_error)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> context.getString(R.string.speech_permission_error)
            SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.speech_network_error)
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> context.getString(R.string.speech_network_timeout_error)
            SpeechRecognizer.ERROR_NO_MATCH -> context.getString(R.string.speech_no_match_error)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> context.getString(R.string.speech_busy_error)
            SpeechRecognizer.ERROR_SERVER -> context.getString(R.string.speech_server_error)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> context.getString(R.string.speech_timeout_error)
            else -> context.getString(R.string.speech_general_error)
        }
        
        onSpeechError(errorMessage)
        stopListening()
    }

    private fun handleResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val newText = matches[0]
            onTextRecognized(newText)
        }

        isListening = false
        updateVoiceButtonState(false)
    }

    private fun updateVoiceButtonState(isListening: Boolean) {
        val color = if (isListening) android.graphics.Color.RED else android.graphics.Color.GRAY
        val scale = 1.0f

        onVoiceButtonStateChanged(isListening, color, scale)

        if (isListening) {
            onAnimationStateChanged(true, false)
        } else {
            onAnimationStateChanged(false, true)
        }
    }

    private fun updateVoiceButtonScale(rmsdB: Float) {
        val scale = 1.0f + (rmsdB / 100f)
        val clampedScale = scale.coerceIn(1.0f, 1.3f)

        onVoiceButtonStateChanged(
            isListening,
            if (isListening) android.graphics.Color.RED else android.graphics.Color.GRAY,
            clampedScale
        )
    }
}
