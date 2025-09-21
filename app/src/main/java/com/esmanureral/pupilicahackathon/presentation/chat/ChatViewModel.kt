package com.esmanureral.pupilicahackathon.presentation.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.esmanureral.pupilicahackathon.data.model.ChatMessage
import com.esmanureral.pupilicahackathon.data.remote.ApiClient
import com.esmanureral.pupilicahackathon.data.remote.ChatApiService
import java.util.Locale
import java.util.UUID

enum class ChatErrorType(val stringResId: Int) {
    EMPTY_MESSAGE(com.esmanureral.pupilicahackathon.R.string.error_empty_message),
    SESSION_ERROR(com.esmanureral.pupilicahackathon.R.string.error_session_error),
    INVALID_RESPONSE(com.esmanureral.pupilicahackathon.R.string.error_invalid_response),
    NO_RESPONSE(com.esmanureral.pupilicahackathon.R.string.error_no_response),
    SERVER_ERROR(com.esmanureral.pupilicahackathon.R.string.error_server_error),
    NETWORK_ERROR(com.esmanureral.pupilicahackathon.R.string.error_network_error),
    TIMEOUT_ERROR(com.esmanureral.pupilicahackathon.R.string.error_timeout_error),
    CONNECTION_ERROR(com.esmanureral.pupilicahackathon.R.string.error_connection_error),
    UNKNOWN_ERROR(com.esmanureral.pupilicahackathon.R.string.error_unknown_error)
}

class ChatViewModel : ViewModel() {

    private val chatApiService: ChatApiService = ApiClient.provideChatApi()
    private val sessionId = UUID.randomUUID().toString()
    private var textToSpeech: TextToSpeech? = null
    private var isVoiceChatMode = false

    private val _isListening = MutableLiveData<Boolean>()
    val isListeningLiveData: LiveData<Boolean> = _isListening

    private val _permissionGranted = MutableLiveData<Boolean>()
    val permissionGrantedLiveData: LiveData<Boolean> = _permissionGranted

    private val _recognizedText = MutableLiveData<String>()
    val recognizedTextLiveData: LiveData<String> = _recognizedText

    private val _formattedText = MutableLiveData<String>()
    val formattedTextLiveData: LiveData<String> = _formattedText

    private val _partialText = MutableLiveData<String>()
    val partialTextLiveData: LiveData<String> = _partialText

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messagesLiveData: LiveData<List<ChatMessage>> = _messages

    private val _errorType = MutableLiveData<ChatErrorType?>()
    val errorTypeLiveData: LiveData<ChatErrorType?> = _errorType

    private val _errorDetails = MutableLiveData<String>()
    val errorDetailsLiveData: LiveData<String> = _errorDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoadingLiveData: LiveData<Boolean> = _isLoading

    private val messagesList = mutableListOf<ChatMessage>()

    fun addErrorMessageToChat(errorMessage: ChatMessage) {
        messagesList.add(errorMessage)
        _messages.value = messagesList.toList()
    }

    fun clearError() {
        _errorType.value = null
        _errorDetails.value = ""
    }

    fun addWelcomeMessage(welcomeText: String) {
        val welcomeMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = welcomeText,
            isFromUser = false
        )
        messagesList.add(welcomeMessage)
        _messages.value = messagesList.toList()
    }

    private val _voiceButtonColor = MutableLiveData<Int>()
    val voiceButtonColorLiveData: LiveData<Int> = _voiceButtonColor

    private val _voiceButtonScale = MutableLiveData<Float>()
    val voiceButtonScaleLiveData: LiveData<Float> = _voiceButtonScale

    private val _shouldStartPulseAnimation = MutableLiveData<Boolean>()
    val shouldStartPulseAnimationLiveData: LiveData<Boolean> = _shouldStartPulseAnimation

    private val _shouldStopPulseAnimation = MutableLiveData<Boolean>()
    val shouldStopPulseAnimationLiveData: LiveData<Boolean> = _shouldStopPulseAnimation

    fun onSpeechTextRecognized(text: String) {
        _recognizedText.value = text
    }

    fun onSpeechPartialTextRecognized(text: String) {
        _partialText.value = text
    }

    fun onSpeechError(errorMessage: String) {
        addErrorMessage(ChatErrorType.UNKNOWN_ERROR, errorMessage)
    }

    fun onSpeechPermissionStatusChanged(isGranted: Boolean) {
        _permissionGranted.value = isGranted
    }

    fun onSpeechVoiceButtonStateChanged(isListening: Boolean, color: Int, scale: Float) {
        _isListening.value = isListening
        _voiceButtonColor.value = color
        _voiceButtonScale.value = scale
    }

    fun onSpeechAnimationStateChanged(shouldStart: Boolean, shouldStop: Boolean) {
        if (shouldStart) {
            _shouldStartPulseAnimation.value = true
        }
        if (shouldStop) {
            _shouldStopPulseAnimation.value = true
        }
    }

    fun resetStartPulseAnimationFlag() {
        _shouldStartPulseAnimation.value = false
    }

    fun resetStopPulseAnimationFlag() {
        _shouldStopPulseAnimation.value = false
    }

    fun onSendClicked(message: String) {
        val trimmedMessage = message.trim()

        if (trimmedMessage.isEmpty()) {
            addErrorMessage(ChatErrorType.EMPTY_MESSAGE)
            return
        }

        _isLoading.value = true
        sendMessage(trimmedMessage)
    }

    fun checkPendingMessage(context: android.content.Context): String? {
        val sharedPref =
            context.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)
        val pendingMessage = sharedPref.getString("pending_message", null)

        if (!pendingMessage.isNullOrEmpty()) {
            clearPendingMessage(context)
            return pendingMessage
        }
        return null
    }

    private fun clearPendingMessage(context: android.content.Context) {
        val sharedPref =
            context.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)
        sharedPref.edit().remove("pending_message").apply()
    }

    fun sendPendingMessage(message: String) {
        onSendClicked(message)
    }

    fun getMessageText(currentText: String): String {
        return currentText.trim()
    }

    fun validateMessage(message: String): Boolean {
        return message.trim().isNotEmpty()
    }

    fun shouldShowKeyboard(): Boolean {
        return !isLoadingLiveData.value!! ?: false
    }

    fun shouldEnableSendButton(): Boolean {
        return !isLoadingLiveData.value!! ?: false
    }


    fun onRecognizedTextReceived(newText: String, currentText: String = "") {
        if (newText.isNotEmpty()) {
            _formattedText.value = newText
        }
    }

    fun clearRecognizedText() {
        _recognizedText.value = ""
    }

    fun clearPartialText() {
        _partialText.value = ""
    }

    private fun addErrorMessage(errorType: ChatErrorType, details: String = "") {
        _errorType.value = errorType
        _errorDetails.value = details
    }

    private fun addUserMessage(text: String) {
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = true
        )
        messagesList.add(userMessage)
        _messages.value = messagesList.toList()
    }

    private fun addBotMessage(text: String) {
        val botMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = false
        )
        messagesList.add(botMessage)
        _messages.value = messagesList.toList()
    }

    private fun sendMessage(message: String) {
        val validSessionId = sessionId.trim()

        if (validSessionId.isEmpty()) {
            addErrorMessage(ChatErrorType.SESSION_ERROR)
            _isLoading.value = false
            return
        }

        addUserMessage(message)

        viewModelScope.launch {
            try {
                val response = chatApiService.sendMessage(message, validSessionId)
                if (response.isSuccessful) {
                    val chatResponse = response.body()

                    chatResponse?.let {
                        val responseText = it.response ?: it.message ?: it.answer ?: it.reply

                        if (!responseText.isNullOrEmpty()) {
                            addBotMessage(responseText!!)
                            speakBotResponse(responseText!!)
                        } else {
                            addErrorMessage(ChatErrorType.INVALID_RESPONSE)
                        }
                    } ?: run {
                        addErrorMessage(ChatErrorType.NO_RESPONSE)
                    }
                } else {
                    val errorCode = response.code()
                    addErrorMessage(ChatErrorType.SERVER_ERROR, errorCode.toString())
                }
            } catch (e: java.net.UnknownHostException) {
                addErrorMessage(ChatErrorType.NETWORK_ERROR)
            } catch (e: java.net.SocketTimeoutException) {
                addErrorMessage(ChatErrorType.TIMEOUT_ERROR)
            } catch (e: Exception) {
                addErrorMessage(ChatErrorType.CONNECTION_ERROR)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Sesli Asistan Fonksiyonları
    fun initializeTextToSpeech(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("tr", "TR")
            }
        }
    }

    // 1. Sadece sesli oku
    fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // 2. Tam otomatik sesli sohbet
    fun startVoiceChat() {
        isVoiceChatMode = true
        // Sesli sohbet modunu başlat
    }

    fun stopVoiceChat() {
        isVoiceChatMode = false
        textToSpeech?.stop()
    }

    fun isVoiceChatActive(): Boolean {
        return isVoiceChatMode
    }

    // Bot mesajını sesli okuma
    private fun speakBotResponse(text: String) {
        if (isVoiceChatMode) {
            speakText(text)
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
    }
}