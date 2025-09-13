package com.esmanureral.pupilicahackathon.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.databinding.FragmentChatBinding
import com.esmanureral.pupilicahackathon.startPulseAnimation
import com.esmanureral.pupilicahackathon.stopPulseAnimation

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()

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
        initVoiceButton()
        observeViewModel()
    }

    private fun initViewModel() {
        viewModel.initSpeechRecognizer(requireContext())
        viewModel.checkPermissionStatus(requireContext())
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

    private fun observeViewModel() {
        binding.apply {
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