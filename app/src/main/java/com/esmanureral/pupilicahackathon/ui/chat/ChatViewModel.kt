package com.esmanureral.pupilicahackathon.ui.chat

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
    
    private val chatApiService: ChatApiService = ApiClient.provideChatApi()
    private val sessionId = UUID.randomUUID().toString()

    private val _isListening = MutableLiveData<Boolean>()
    val isListeningLiveData: LiveData<Boolean> = _isListening

    private val _permissionGranted = MutableLiveData<Boolean>()
    val permissionGrantedLiveData: LiveData<Boolean> = _permissionGranted

    private val _recognizedText = MutableLiveData<String>()
    val recognizedTextLiveData: LiveData<String> = _recognizedText
    
    private val _formattedText = MutableLiveData<String>()
    val formattedTextLiveData: LiveData<String> = _formattedText
    
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

    fun onSpeechTextRecognized(text: String) {
        _recognizedText.value = text
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
            addErrorMessage(getNaturalErrorMessage("empty_message"))
            return
        }
        
        _isLoading.value = true
        sendMessage(trimmedMessage)
    }
    
    
    fun onRecognizedTextReceived(newText: String, currentText: String = "") {
        if (newText.isNotEmpty()) {
            val formattedText = if (currentText.isEmpty()) {
                newText
            } else {
                "$currentText $newText"
            }
            _formattedText.value = formattedText
        }
    }
    
    fun clearRecognizedText() {
        _recognizedText.value = ""
    }
    
    private fun addErrorMessage(text: String) {
        val errorMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = false
        )
        messagesList.add(errorMessage)
        _messages.value = messagesList.toList()
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
    
    private fun getNaturalErrorMessage(errorType: String, details: String = ""): String {
        return when (errorType) {
            "empty_message" -> "Mesajınız boş görünüyor. Bir şeyler yazabilir misiniz?"
            "session_error" -> "Bir sorun oluştu. Uygulamayı yeniden başlatmayı deneyebilir misiniz?"
            "invalid_response" -> "Sunucudan beklenmeyen bir yanıt geldi. Tekrar deneyebilir misiniz?"
            "no_response" -> "Sunucudan yanıt alamadım. Tekrar deneyebilir misiniz?"
            "server_error" -> "Sunucuda bir sorun oluştu ($details). Tekrar deneyebilir misiniz?"
            "network_error" -> "İnternet bağlantınızı kontrol edebilir misiniz?"
            "timeout_error" -> "Bağlantı zaman aşımına uğradı. Tekrar deneyebilir misiniz?"
            "connection_error" -> "Bağlantı sorunu yaşandı. Tekrar deneyebilir misiniz?"
            else -> "Beklenmeyen bir hata oluştu. Tekrar deneyebilir misiniz?"
        }
    }
    
    private fun sendMessage(message: String) {
        val validSessionId = sessionId.trim()
        
        if (validSessionId.isEmpty()) {
            addErrorMessage(getNaturalErrorMessage("session_error"))
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
                        } else {
                            addErrorMessage(getNaturalErrorMessage("invalid_response"))
                        }
                    } ?: run {
                        addErrorMessage(getNaturalErrorMessage("no_response"))
                    }
                } else {
                    val errorCode = response.code()
                    addErrorMessage(getNaturalErrorMessage("server_error", errorCode.toString()))
                }
            } catch (e: java.net.UnknownHostException) {
                addErrorMessage(getNaturalErrorMessage("network_error"))
            } catch (e: java.net.SocketTimeoutException) {
                addErrorMessage(getNaturalErrorMessage("timeout_error"))
            } catch (e: Exception) {
                addErrorMessage(getNaturalErrorMessage("connection_error"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}