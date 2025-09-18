package com.esmanureral.pupilicahackathon.ui.chat

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.databinding.FragmentChatBinding
import com.esmanureral.pupilicahackathon.startPulseAnimation
import com.esmanureral.pupilicahackathon.stopPulseAnimation

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: ChatMessageAdapter

    private val requestRecordAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.onPermissionGranted()
            else viewModel.onPermissionDenied()
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
        viewModel.initSpeechRecognizer(requireContext())
        viewModel.checkPermissionStatus(requireContext())
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
                viewModel.onVoiceButtonClicked(requireContext())
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
        binding.inputMessage.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
        
        binding.inputMessage.setOnClickListener {
            binding.inputMessage.requestFocus()
            val inputMethodManager = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(binding.inputMessage, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    private fun sendMessage() {
        val message = binding.inputMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            binding.inputMessage.text?.clear()
            
            val inputMethodManager = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding.inputMessage.windowToken, 0)
            
            viewModel.sendMessage(message)
        }
    }

    private fun observeViewModel() {
        binding.apply {
            // Mesajları gözlemle
            viewModel.messagesLiveData.observe(viewLifecycleOwner) { messages ->
                messageAdapter.submitList(messages)
                // Son mesaja scroll yap
                if (messages.isNotEmpty()) {
                    recyclerViewMessages.scrollToPosition(messages.size - 1)
                }
            }

            // Loading durumunu gözlemle
            viewModel.isLoadingLiveData.observe(viewLifecycleOwner) { isLoading ->
                btnSend.isEnabled = !isLoading
                inputMessage.isEnabled = !isLoading
                if (isLoading) {
                    btnSend.alpha = 0.5f
                } else {
                    btnSend.alpha = 1.0f
                }
            }

            viewModel.recognizedTextLiveData.observe(viewLifecycleOwner) { newText ->
                if (newText.isNotEmpty()) {
                    appendRecognizedText(newText)
                    viewModel.clearRecognizedText()
                }
            }

            viewModel.voiceButtonColorLiveData.observe(viewLifecycleOwner) { color ->
                btnVoice.setColorFilter(color)
            }

            viewModel.voiceButtonScaleLiveData.observe(viewLifecycleOwner) { scale ->
                btnVoice.scaleX = scale
                btnVoice.scaleY = scale
            }

            viewModel.shouldStartPulseAnimationLiveData.observe(viewLifecycleOwner) { shouldStart ->
                if (shouldStart) {
                    btnVoice.startPulseAnimation()
                    viewModel.resetStartPulseAnimationFlag()
                }
            }

            viewModel.shouldStopPulseAnimationLiveData.observe(viewLifecycleOwner) { shouldStop ->
                if (shouldStop) {
                    btnVoice.stopPulseAnimation()
                    viewModel.resetStopPulseAnimationFlag()
                }
            }
        }
    }

    private fun appendRecognizedText(newText: String) {
        binding.apply {
            val currentText = inputMessage.text.toString()
            val updatedText = if (currentText.isEmpty()) {
                newText
            } else {
                requireContext().getString(R.string.recognized_text_format, currentText, newText)
            }
            inputMessage.setText(updatedText)
            inputMessage.setSelection(updatedText.length)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopListeningIfActive()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.btnVoice.stopPulseAnimation()
        _binding = null
    }
}