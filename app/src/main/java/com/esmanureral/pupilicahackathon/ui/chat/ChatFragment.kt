package com.esmanureral.pupilicahackathon.ui.chat

import android.os.Bundle
import android.view.KeyEvent
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
import com.esmanureral.pupilicahackathon.startPulseAnimation
import com.esmanureral.pupilicahackathon.stopPulseAnimation

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
            -1 to getString(R.string.speech_general_error) // Default error message
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
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messageAdapter
        }
    }

    private fun initVoiceButton() {
        binding.btnVoice.setOnClickListener {
            if (viewModel.permissionGrantedLiveData.value == true) {
                speechRecognizerManager.onVoiceButtonClicked()
            } else {
                requestRecordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun initSendButton() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun initEditText() {
        setupEditorActionListener()
        setupEditTextClickListener()
    }

    private fun setupEditorActionListener() {
        binding.inputMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupEditTextClickListener() {
        binding.inputMessage.setOnClickListener {
            focusEditText()
            showKeyboard()
        }
    }

    private fun focusEditText() {
        binding.inputMessage.requestFocus()
    }

    private fun showKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.inputMessage, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun sendMessage() {
        val message = getMessageText()
        clearInputField()
        hideKeyboard()
        viewModel.onSendClicked(message)
    }

    private fun getMessageText(): String {
        return binding.inputMessage.text.toString()
    }

    private fun clearInputField() {
        binding.inputMessage.text?.clear()
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.inputMessage.windowToken, 0)
    }

    private fun observeViewModel() {
        observeMessages()
        observeLoadingState()
        observeRecognizedText()
        observePartialText()
        observeFormattedText()
        observeVoiceButtonStates()
    }

    private fun observeMessages() {
        viewModel.messagesLiveData.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages)
            scrollToLastMessage(messages)
        }
    }

    private fun scrollToLastMessage(messages: List<ChatMessage>) {
        if (messages.isNotEmpty()) {
            binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
        }
    }

    private fun observeLoadingState() {
        viewModel.isLoadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            updateSendButtonState(isLoading)
            updateInputFieldState(isLoading)
        }
    }

    private fun updateSendButtonState(isLoading: Boolean) {
        binding.btnSend.isEnabled = !isLoading
        binding.btnSend.alpha = if (isLoading) 0.5f else 1.0f
    }

    private fun updateInputFieldState(isLoading: Boolean) {
        binding.inputMessage.isEnabled = !isLoading
    }

    private fun observeRecognizedText() {
        viewModel.recognizedTextLiveData.observe(viewLifecycleOwner) { newText ->
            if (newText.isNotEmpty()) {
                val currentText = binding.inputMessage.text.toString()
                viewModel.onRecognizedTextReceived(newText, currentText)
                viewModel.clearRecognizedText()
                viewModel.clearPartialText()
            }
        }
    }

    private fun observePartialText() {
        viewModel.partialTextLiveData.observe(viewLifecycleOwner) { partialText ->
            if (partialText.isNotEmpty()) {
                binding.inputMessage.setText(partialText)
                binding.inputMessage.setSelection(partialText.length)
            }
        }
    }

    private fun observeFormattedText() {
        viewModel.formattedTextLiveData.observe(viewLifecycleOwner) { formattedText ->
            if (formattedText.isNotEmpty()) {
                binding.inputMessage.setText(formattedText)
                binding.inputMessage.setSelection(formattedText.length)
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
            binding.btnVoice.setColorFilter(color)
        }
    }

    private fun observeVoiceButtonScale() {
        viewModel.voiceButtonScaleLiveData.observe(viewLifecycleOwner) { scale ->
            binding.btnVoice.scaleX = scale
            binding.btnVoice.scaleY = scale
        }
    }

    private fun observeVoiceButtonAnimations() {
        viewModel.shouldStartPulseAnimationLiveData.observe(viewLifecycleOwner) { shouldStart ->
            if (shouldStart) {
                binding.btnVoice.startPulseAnimation()
                viewModel.resetStartPulseAnimationFlag()
            }
        }

        viewModel.shouldStopPulseAnimationLiveData.observe(viewLifecycleOwner) { shouldStop ->
            if (shouldStop) {
                binding.btnVoice.stopPulseAnimation()
                viewModel.resetStopPulseAnimationFlag()
            }
        }
    }


    override fun onPause() {
        super.onPause()
        speechRecognizerManager.stopListeningIfActive()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.btnVoice.stopPulseAnimation()
        speechRecognizerManager.destroy()
        _binding = null
    }
}