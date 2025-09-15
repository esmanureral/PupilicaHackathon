package com.esmanureral.pupilicahackathon.ui.home

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.camera.CameraPermissionManager
import com.esmanureral.pupilicahackathon.databinding.FragmentHomeBinding
import com.esmanureral.pupilicahackathon.showToast

class HomeFragment : Fragment() {

	private var _binding: FragmentHomeBinding? = null
	private val binding get() = _binding!!
	private val viewModel: HomeViewModel by viewModels()

	private val takePictureLauncher = registerForActivityResult(
		ActivityResultContracts.TakePicturePreview()
	) { bitmap ->
		bitmap?.let { handleCapturedImage(it, null) }
	}

	private val pickImageLauncher = registerForActivityResult(
		ActivityResultContracts.GetContent()
	) { uri ->
		uri?.let {
			val bitmap = uriToBitmap(it)
			bitmap?.let { bmp -> handleCapturedImage(bmp, it.toString()) }
		}
	}

	private val requestPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted ->
		if (isGranted) openCamera()
		else requireContext().showToast(getString(R.string.camera_permission_required))
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

		setupClickListeners()
		observeViewModel()
	}

	private fun setupClickListeners() {
		binding.imgChat.setOnClickListener {
			findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToChatFragment())
		}

		binding.imgAI.setOnClickListener { showSourceChooser() }

		val reminderClickListener = View.OnClickListener {
			findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToReminderFragment())
		}

		binding.btnSelectDate.setOnClickListener(reminderClickListener)
		binding.btnSelectTime.setOnClickListener(reminderClickListener)
		binding.btnSetReminder.setOnClickListener(reminderClickListener)
	}

	private fun observeViewModel() {
		viewModel.capturedImage.observe(viewLifecycleOwner) { bitmap ->
			// Eğer UI güncellemesi gerekirse burada yapabilirsiniz
		}

		viewModel.analyzeResult.observe(viewLifecycleOwner) { result ->
			result?.let {
				val imageUri = viewModel.lastImageUri
				try {
					val action = HomeFragmentDirections.actionHomeFragmentToAnalysisResultFragment(
						it,
						imageUri,
						it
					)
					findNavController().navigate(action)
				} catch (e: Exception) {
					requireContext().showToast(getString(R.string.error_no_time))
				}
			}
		}

		viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
			binding.imgAI.apply {
				alpha = if (isLoading) 0.5f else 1.0f
				isEnabled = !isLoading
			}
		}
	}

	private fun handleCapturedImage(bitmap: Bitmap, uri: String?) {
		viewModel.lastImageUri = uri
		viewModel.setCapturedImage(bitmap)
		viewModel.processImage(bitmap)
	}

	private fun checkCameraPermissionAndOpen() {
		if (CameraPermissionManager.hasCameraPermission(requireContext())) {
			openCamera()
		} else {
			requestPermissionLauncher.launch(Manifest.permission.CAMERA)
		}
	}

	private fun openCamera() = takePictureLauncher.launch(null)
	private fun openGallery() = pickImageLauncher.launch("image/*")

	private fun showSourceChooser() {
		val options = arrayOf(getString(R.string.take_photo), getString(R.string.choose_from_gallery))
		AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.select_image_source))
			.setItems(options) { _, which ->
				when (which) {
					0 -> checkCameraPermissionAndOpen()
					1 -> openGallery()
				}
			}
			.show()
	}

	private fun uriToBitmap(uri: Uri): Bitmap? {
		return try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
				ImageDecoder.decodeBitmap(source)
			} else {
				@Suppress("DEPRECATION")
				MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
			}
		} catch (e: Exception) {
			requireContext().showToast(e.message ?: getString(R.string.error_no_time))
			null
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}