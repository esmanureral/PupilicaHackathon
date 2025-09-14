package com.esmanureral.pupilicahackathon.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.databinding.FragmentHomeBinding
import com.esmanureral.pupilicahackathon.showToast

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            viewModel.setCapturedImage(it)
            viewModel.processImage(it)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            requireContext().showToast(getString(R.string.camera_permission_required))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgChat.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToChatFragment())

        }
        binding.imgAI.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
        
        binding.btnSelectDate.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToReminderFragment())
        }
        
        binding.btnSelectTime.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToReminderFragment())
        }
        
        binding.btnSetReminder.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToReminderFragment())
        }

        viewModel.capturedImage.observe(viewLifecycleOwner) { bitmap ->
            bitmap?.let {
            }
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        takePictureLauncher.launch(null)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
