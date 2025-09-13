package com.esmanureral.pupilicahackathon.ui.chat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private var isListening = false
    private var isPermissionGranted = false

    private val _isListening = MutableLiveData<Boolean>()
    val isListeningLiveData: LiveData<Boolean> = _isListening

    private val _permissionGranted = MutableLiveData<Boolean>()
    val permissionGrantedLiveData: LiveData<Boolean> = _permissionGranted

    private val _recognizedText = MutableLiveData<String>()
    val recognizedTextLiveData: LiveData<String> = _recognizedText

    private val _voiceButtonColor = MutableLiveData<Int>()
    val voiceButtonColorLiveData: LiveData<Int> = _voiceButtonColor

    private val _voiceButtonScale = MutableLiveData<Float>()
    val voiceButtonScaleLiveData: LiveData<Float> = _voiceButtonScale

    private val _shouldStartPulseAnimation = MutableLiveData<Boolean>()
    val shouldStartPulseAnimationLiveData: LiveData<Boolean> = _shouldStartPulseAnimation

    private val _shouldStopPulseAnimation = MutableLiveData<Boolean>()
    val shouldStopPulseAnimationLiveData: LiveData<Boolean> = _shouldStopPulseAnimation

    fun initSpeechRecognizer(context: Context) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechIntent = createSpeechIntent()
        speechRecognizer.setRecognitionListener(createRecognitionListener())
    }

    private fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
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
        override fun onError(error: Int) = stopListening()
        override fun onResults(results: Bundle?) = handleResults(results)
        override fun onPartialResults(partialResults: Bundle?) =
            handlePartialResults(partialResults)

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun checkPermissionStatus(context: Context) {
        isPermissionGranted = checkAudioPermission(context)
        _permissionGranted.value = isPermissionGranted
        updateVoiceButtonState(false)
    }

    private fun checkAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionGranted() {
        isPermissionGranted = true
        _permissionGranted.value = true
        startListening()
    }

    fun onPermissionDenied() {
        isPermissionGranted = false
        _permissionGranted.value = false
        updateVoiceButtonState(false)
    }

    fun onVoiceButtonClicked(context: Context) {
        if (isPermissionGranted || checkAudioPermission(context)) {
            startListening()
        } else {
            _permissionGranted.value = false
        }
    }

    private fun startListening() {
        if (!isListening && ::speechRecognizer.isInitialized) {
            try {
                isListening = true
                _isListening.value = true
                speechRecognizer.startListening(speechIntent)
                updateVoiceButtonState(true)
            } catch (e: Exception) {
                isListening = false
                _isListening.value = false
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
        _isListening.value = false
        updateVoiceButtonState(false)
    }

    private fun handlePartialResults(partialResults: Bundle?) {
    }

    private fun handleResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val newText = matches[0]
            _recognizedText.value = newText
        }

        isListening = false
        _isListening.value = false
        updateVoiceButtonState(false)
    }

    private fun updateVoiceButtonState(isListening: Boolean) {
        val color = if (isListening) android.graphics.Color.RED else android.graphics.Color.GRAY
        _voiceButtonColor.value = color

        if (isListening) {
            _shouldStartPulseAnimation.value = true
        } else {
            _shouldStopPulseAnimation.value = true
        }
    }

    private fun updateVoiceButtonScale(rmsdB: Float) {
        val scale = 1.0f + (rmsdB / 100f)
        val clampedScale = scale.coerceIn(1.0f, 1.3f)
        _voiceButtonScale.value = clampedScale
    }

    fun getCurrentRecognizedText(): String? = _recognizedText.value

    fun clearRecognizedText() {
        _recognizedText.value = ""
    }

    fun stopListeningIfActive() {
        if (isListening) {
            stopListening()
        }
    }

    fun resetStartPulseAnimationFlag() {
        _shouldStartPulseAnimation.value = false
    }

    fun resetStopPulseAnimationFlag() {
        _shouldStopPulseAnimation.value = false
    }

    override fun onCleared() {
        super.onCleared()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}