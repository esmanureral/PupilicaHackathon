package com.esmanureral.pupilicahackathon.ui.chat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.esmanureral.pupilicahackathon.data.model.ChatMessage
import com.esmanureral.pupilicahackathon.data.network.ApiClient
import com.esmanureral.pupilicahackathon.data.network.ChatApiService
import java.util.UUID

class ChatViewModel : ViewModel() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private var isListening = false
    private var isPermissionGranted = false
    
    private val chatApiService: ChatApiService = ApiClient.provideChatApi()
    private val sessionId = UUID.randomUUID().toString()

    private val _isListening = MutableLiveData<Boolean>()
    val isListeningLiveData: LiveData<Boolean> = _isListening

    private val _permissionGranted = MutableLiveData<Boolean>()
    val permissionGrantedLiveData: LiveData<Boolean> = _permissionGranted

    private val _recognizedText = MutableLiveData<String>()
    val recognizedTextLiveData: LiveData<String> = _recognizedText
    
    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messagesLiveData: LiveData<List<ChatMessage>> = _messages
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoadingLiveData: LiveData<Boolean> = _isLoading
    
    private val messagesList = mutableListOf<ChatMessage>()
    
    init {
        val welcomeMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "Merhaba! Ben AI danışmanınızım. Size nasıl yardımcı olabilirim?",
            isFromUser = false
        )
        messagesList.add(welcomeMessage)
        _messages.value = messagesList
    }

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

    fun sendMessage(message: String) {
        val trimmedMessage = message.trim()
        val validSessionId = sessionId.trim()
        
        if (trimmedMessage.isEmpty()) {
            val errorMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Mesaj boş olamaz. Lütfen bir mesaj yazın.",
                isFromUser = false
            )
            messagesList.add(errorMessage)
            _messages.value = messagesList.toList()
            return
        }
        
        if (validSessionId.isEmpty()) {
            val errorMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Session ID hatası. Lütfen uygulamayı yeniden başlatın.",
                isFromUser = false
            )
            messagesList.add(errorMessage)
            _messages.value = messagesList.toList()
            return
        }
        
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = trimmedMessage,
            isFromUser = true
        )
        messagesList.add(userMessage)
        _messages.value = messagesList.toList()
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val response = chatApiService.sendMessage(trimmedMessage, validSessionId)
                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    
                    chatResponse?.let {
                        val responseText = it.response ?: it.message ?: it.answer ?: it.reply
                        
                        if (!responseText.isNullOrEmpty()) {
                            val botMessage = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = responseText!!,
                                isFromUser = false
                            )
                            messagesList.add(botMessage)
                            _messages.value = messagesList.toList()
                        } else {
                            val errorMessage = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = "Sunucudan geçersiz yanıt alındı. Lütfen tekrar deneyin.",
                                isFromUser = false
                            )
                            messagesList.add(errorMessage)
                            _messages.value = messagesList.toList()
                        }
                    } ?: run {
                        // Response body null ise
                        val errorMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "Sunucudan yanıt alınamadı. Lütfen tekrar deneyin.",
                            isFromUser = false
                        )
                        messagesList.add(errorMessage)
                        _messages.value = messagesList.toList()
                    }
                } else {
                    val errorCode = response.code()
                    val errorMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "Sunucu hatası ($errorCode). Lütfen tekrar deneyin.",
                        isFromUser = false
                    )
                    messagesList.add(errorMessage)
                    _messages.value = messagesList.toList()
                }
            } catch (e: java.net.UnknownHostException) {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Sunucu bulunamadı. Lütfen internet bağlantınızı kontrol edin.",
                    isFromUser = false
                )
                messagesList.add(errorMessage)
                _messages.value = messagesList.toList()
            } catch (e: java.net.SocketTimeoutException) {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Bağlantı zaman aşımına uğradı. Lütfen tekrar deneyin.",
                    isFromUser = false
                )
                messagesList.add(errorMessage)
                _messages.value = messagesList.toList()
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Bağlantı hatası: ${e.message}",
                    isFromUser = false
                )
                messagesList.add(errorMessage)
                _messages.value = messagesList.toList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}