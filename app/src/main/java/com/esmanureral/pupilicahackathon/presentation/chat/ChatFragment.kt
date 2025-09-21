package com.esmanureral.pupilicahackathon.presentation.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.speech.SpeechRecognizer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.model.ChatMessage
import com.esmanureral.pupilicahackathon.databinding.FragmentChatBinding
import com.esmanureral.pupilicahackathon.presentation.extensions.startPulseAnimation
import com.esmanureral.pupilicahackathon.presentation.extensions.stopPulseAnimation
import com.esmanureral.pupilicahackathon.service.SpeechRecognizerManager

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: ChatMessageAdapter
    private lateinit var speechRecognizerManager: SpeechRecognizerManager

    private val requestRecordAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) speechRecognizerManager.onPermissionGranted()
            else speechRecognizerManager.onPermissionDenied()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initRecyclerView()
        initVoiceButton()
        initSendButton()
        initEditText()
        observeViewModel()
        binding.btnArrow.setOnClickListener {
            findNavController().popBackStack()
        }
        addWelcomeMessage()
        checkPendingMessage()
    }

    private fun addWelcomeMessage() {
        val welcomeText = getString(R.string.chat_welcome_message)
        viewModel.addWelcomeMessage(welcomeText)
    }

    private fun getErrorMessage(errorType: ChatErrorType, details: String = ""): String {
        return when (errorType) {
            ChatErrorType.EMPTY_MESSAGE -> getString(R.string.error_empty_message)
            ChatErrorType.SESSION_ERROR -> getString(R.string.error_session_error)
            ChatErrorType.INVALID_RESPONSE -> getString(R.string.error_invalid_response)
            ChatErrorType.NO_RESPONSE -> getString(R.string.error_no_response)
            ChatErrorType.SERVER_ERROR -> getString(R.string.error_server_error, details)
            ChatErrorType.NETWORK_ERROR -> getString(R.string.error_network_error)
            ChatErrorType.TIMEOUT_ERROR -> getString(R.string.error_timeout_error)
            ChatErrorType.CONNECTION_ERROR -> getString(R.string.error_connection_error)
            ChatErrorType.UNKNOWN_ERROR -> getString(R.string.error_unknown_error)
        }
    }

    private fun checkPendingMessage() {
        val pendingMessage = viewModel.checkPendingMessage(requireContext())
        if (!pendingMessage.isNullOrEmpty()) {
            viewModel.sendPendingMessage(pendingMessage)
        }
    }

    private fun initViewModel() {
        initSpeechRecognizerManager()
    }

    private fun initSpeechRecognizerManager() {
        val languageCode = getString(R.string.speech_language_code)
        val errorMessages = mapOf(
            SpeechRecognizer.ERROR_AUDIO to getString(R.string.speech_audio_error),
            SpeechRecognizer.ERROR_CLIENT to getString(R.string.speech_client_error),
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to getString(R.string.speech_permission_error),
            SpeechRecognizer.ERROR_NETWORK to getString(R.string.speech_network_error),
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT to getString(R.string.speech_network_timeout_error),
            SpeechRecognizer.ERROR_NO_MATCH to getString(R.string.speech_no_match_error),
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY to getString(R.string.speech_busy_error),
            SpeechRecognizer.ERROR_SERVER to getString(R.string.speech_server_error),
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT to getString(R.string.speech_timeout_error),
            -1 to getString(R.string.speech_general_error)
        )

        speechRecognizerManager = SpeechRecognizerManager(
            context = requireContext(),
            languageCode = languageCode,
            errorMessages = errorMessages,
            onTextRecognized = { text -> viewModel.onSpeechTextRecognized(text) },
            onPartialTextRecognized = { text -> viewModel.onSpeechPartialTextRecognized(text) },
            onSpeechError = { errorMessage -> viewModel.onSpeechError(errorMessage) },
            onPermissionStatusChanged = { isGranted ->
                viewModel.onSpeechPermissionStatusChanged(
                    isGranted
                )
            },
            onVoiceButtonStateChanged = { isListening, color, scale ->
                viewModel.onSpeechVoiceButtonStateChanged(isListening, color, scale)
            },
            onAnimationStateChanged = { shouldStart, shouldStop ->
                viewModel.onSpeechAnimationStateChanged(shouldStart, shouldStop)
            }
        )
        speechRecognizerManager.initialize()
    }

    private fun initRecyclerView() {
        messageAdapter = ChatMessageAdapter()
        with(binding) {
            recyclerViewMessages.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = messageAdapter
            }
        }
    }

    private fun initVoiceButton() {
        with(binding) {
            btnVoice.setOnClickListener {
                if (viewModel.permissionGrantedLiveData.value == true) {
                    speechRecognizerManager.onVoiceButtonClicked()
                } else {
                    requestRecordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    private fun initSendButton() {
        with(binding) {
            btnSend.setOnClickListener {
                sendMessage()
            }
        }
    }

    private fun initEditText() {
        setupEditorActionListener()
        setupEditTextClickListener()
    }

    private fun setupEditorActionListener() {
        with(binding) {
            inputMessage.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupEditTextClickListener() {
        with(binding) {
            inputMessage.setOnClickListener {
                focusEditText()
                showKeyboard()
            }
        }
    }

    private fun focusEditText() {
        with(binding) {
            inputMessage.requestFocus()
        }
    }

    private fun showKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        with(binding) {
            inputMethodManager.showSoftInput(inputMessage, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun sendMessage() {
        val message = getMessageText()
        if (viewModel.validateMessage(message)) {
            clearInputField()
            hideKeyboard()
            viewModel.onSendClicked(message)
        }
    }

    private fun getMessageText(): String {
        return with(binding) {
            val currentText = inputMessage.text.toString()
            viewModel.getMessageText(currentText)
        }
    }

    private fun clearInputField() {
        with(binding) {
            inputMessage.text?.clear()
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        with(binding) {
            inputMethodManager.hideSoftInputFromWindow(inputMessage.windowToken, 0)
        }
    }

    private fun observeViewModel() {
        observeMessages()
        observeLoadingState()
        observeRecognizedText()
        observePartialText()
        observeFormattedText()
        observeVoiceButtonStates()
        observeErrors()
    }

    private fun observeMessages() {
        viewModel.messagesLiveData.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages)
            scrollToLastMessage(messages)
        }
    }

    private fun scrollToLastMessage(messages: List<ChatMessage>) {
        if (messages.isNotEmpty()) {
            with(binding) {
                recyclerViewMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun observeLoadingState() {
        viewModel.isLoadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            updateSendButtonState(isLoading)
            updateInputFieldState(isLoading)
        }
    }

    private fun updateSendButtonState(isLoading: Boolean) {
        with(binding) {
            btnSend.isEnabled = viewModel.shouldEnableSendButton()
            btnSend.alpha = if (isLoading) 0.5f else 1.0f
        }
    }

    private fun updateInputFieldState(isLoading: Boolean) {
        with(binding) {
            inputMessage.isEnabled = viewModel.shouldShowKeyboard()
        }
    }

    private fun observeRecognizedText() {
        viewModel.recognizedTextLiveData.observe(viewLifecycleOwner) { newText ->
            if (newText.isNotEmpty()) {
                viewModel.onRecognizedTextReceived(newText)
                viewModel.clearRecognizedText()
                viewModel.clearPartialText()
            }
        }
    }

    private fun observePartialText() {
        viewModel.partialTextLiveData.observe(viewLifecycleOwner) { partialText ->
            if (partialText.isNotEmpty()) {
                with(binding) {
                    inputMessage.setText(partialText)
                    inputMessage.setSelection(partialText.length)
                }
            }
        }
    }

    private fun observeFormattedText() {
        viewModel.formattedTextLiveData.observe(viewLifecycleOwner) { formattedText ->
            if (formattedText.isNotEmpty()) {
                with(binding) {
                    inputMessage.setText(formattedText)
                    inputMessage.setSelection(formattedText.length)
                }
            }
        }
    }

    private fun observeVoiceButtonStates() {
        observeVoiceButtonColor()
        observeVoiceButtonScale()
        observeVoiceButtonAnimations()
    }

    private fun observeVoiceButtonColor() {
        viewModel.voiceButtonColorLiveData.observe(viewLifecycleOwner) { color ->
            with(binding) {
                btnVoice.setColorFilter(color)
            }
        }
    }

    private fun observeVoiceButtonScale() {
        viewModel.voiceButtonScaleLiveData.observe(viewLifecycleOwner) { scale ->
            with(binding) {
                btnVoice.scaleX = scale
                btnVoice.scaleY = scale
            }
        }
    }

    private fun observeVoiceButtonAnimations() {
        viewModel.shouldStartPulseAnimationLiveData.observe(viewLifecycleOwner) { shouldStart ->
            if (shouldStart) {
                with(binding) {
                    btnVoice.startPulseAnimation()
                }
                viewModel.resetStartPulseAnimationFlag()
            }
        }

        viewModel.shouldStopPulseAnimationLiveData.observe(viewLifecycleOwner) { shouldStop ->
            if (shouldStop) {
                with(binding) {
                    btnVoice.stopPulseAnimation()
                }
                viewModel.resetStopPulseAnimationFlag()
            }
        }
    }

    private fun observeErrors() {
        viewModel.errorTypeLiveData.observe(viewLifecycleOwner) { errorType ->
            errorType?.let {
                val details = viewModel.errorDetailsLiveData.value ?: ""
                val errorMessage = getErrorMessage(it, details)
                addErrorMessageToChat(errorMessage)
                viewModel.clearError()
            }
        }
    }

    private fun addErrorMessageToChat(errorText: String) {
        val errorMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = errorText,
            isFromUser = false
        )
        viewModel.addErrorMessageToChat(errorMessage)
    }

    override fun onPause() {
        super.onPause()
        speechRecognizerManager.stopListeningIfActive()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        with(binding) {
            btnVoice.stopPulseAnimation()
        }
        speechRecognizerManager.destroy()
        _binding = null
    }
}