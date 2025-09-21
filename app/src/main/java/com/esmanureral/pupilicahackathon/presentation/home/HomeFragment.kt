package com.esmanureral.pupilicahackathon.presentation.home

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.repository.DentalFactsRepository
import com.esmanureral.pupilicahackathon.data.utils.CameraPermissionManager
import com.esmanureral.pupilicahackathon.databinding.FragmentHomeBinding
import com.esmanureral.pupilicahackathon.presentation.extensions.showToast
import com.esmanureral.pupilicahackathon.data.utils.NetworkUtils
import com.esmanureral.pupilicahackathon.presentation.home.adapter.DentalFactsPagerAdapter
import com.esmanureral.pupilicahackathon.utils.AnalysisCounter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var dentalFactsRepository: DentalFactsRepository
    
    private var autoScrollHandler: Handler? = null
    private var autoScrollRunnable: Runnable? = null
    private var currentPosition = 0
    private var factsCount = 0

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { handleCapturedImage(it, null) }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uriToBitmap(it)?.let { bmp -> handleCapturedImage(bmp, it.toString()) } }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera() else requireContext().showToast(getString(R.string.camera_permission_required))
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        FragmentHomeBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeRepository()
        setupClickListeners()
        setupAnalysisCounter()
        setupDentalFacts()
        observeViewModel()
    }

    private fun initializeRepository() {
        dentalFactsRepository = DentalFactsRepository(requireContext())
    }

    private fun setupClickListeners() {
        setupChatClickListener()
        setupAIClickListener()
        setupReminderClickListener()
        setupSearchClickListeners()
    }

    private fun setupChatClickListener() {
        with(binding) {
            imgChat.setOnClickListener { 
                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    navigateToChat()
                } else {
                    showNoInternetDialog()
                }
            }
        }
    }

    private fun setupAIClickListener() {
        with(binding) {
            imgAI.setOnClickListener { 
                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    if (viewModel.canPerformAnalysis(requireContext())) {
                        showSourceChooser()
                    } else {
                        showPremiumDialog()
                    }
                } else {
                    showNoInternetDialog()
                }
            }
        }
    }

    private fun setupReminderClickListener() {
        with(binding) {
            btnSetReminder.setOnClickListener {
                navigateToReminder()
            }
        }
    }

    private fun setupSearchClickListeners() {
        with(binding) {
            etSearch.setOnClickListener {
                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    navigateToChat()
                } else {
                    showNoInternetDialog()
                }
            }
            
            etSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    handleSearchSubmit()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun navigateToReminder() {
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToReminderFragment())
    }

    private fun handleSearchSubmit() {
        val searchText = binding.etSearch.text.toString().trim()
        if (searchText.isNotEmpty()) {
            navigateToChatWithMessage(searchText)
        }
    }
    
    private fun navigateToChatWithMessage(message: String) {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            viewModel.savePendingMessage(requireContext(), message)
            navigateToChat()
            clearSearchText()
        } else {
            showNoInternetDialog()
        }
    }

    private fun navigateToChat() {
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToChatFragment())
    }

    private fun clearSearchText() {
        with(binding) {
            etSearch.text?.clear()
        }
    }
    
    private fun setupDentalFacts() {
        val randomFacts = dentalFactsRepository.getRandomFacts(3)
        factsCount = randomFacts.size
        
        val adapter = DentalFactsPagerAdapter(requireActivity(), randomFacts)
        binding.vpDentalFacts.adapter = adapter
        
        setupPageIndicator(randomFacts.size)
        
        binding.vpDentalFacts.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                updatePageIndicator(position, randomFacts.size)
            }
        })
        
        binding.vpDentalFacts.setOnTouchCallback {
            stopAutoScroll()
            startAutoScroll()
        }
        startAutoScroll()
    }
    
    private fun setupPageIndicator(pageCount: Int) {
        binding.llPageIndicator.removeAllViews()
        
        for (i in 0 until pageCount) {
            val dot = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = if (i < pageCount - 1) 8 else 0
                }
                setImageResource(if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
            }
            binding.llPageIndicator.addView(dot)
        }
    }
    
    private fun updatePageIndicator(currentPosition: Int, pageCount: Int) {
        for (i in 0 until binding.llPageIndicator.childCount) {
            val dot = binding.llPageIndicator.getChildAt(i) as ImageView
            dot.setImageResource(if (i == currentPosition) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }
    
    private fun startAutoScroll() {
        stopAutoScroll()
        
        autoScrollHandler = Handler(Looper.getMainLooper())
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (factsCount > 1) {
                    currentPosition = (currentPosition + 1) % factsCount
                    binding.vpDentalFacts.currentItem = currentPosition
                    
                    autoScrollHandler?.postDelayed(this, 3000)
                }
            }
        }
        
        autoScrollHandler?.postDelayed(autoScrollRunnable!!, 3000)
    }
    
    private fun stopAutoScroll() {
        autoScrollRunnable?.let { runnable ->
            autoScrollHandler?.removeCallbacks(runnable)
        }
        autoScrollHandler = null
        autoScrollRunnable = null
    }
    
    private fun showNoInternetDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.no_internet_title))
            .setMessage(getString(R.string.no_internet_message))
            .setPositiveButton("Tamam") { dialog, _ -> 
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }
    
    private fun showPremiumDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.premium_title))
            .setMessage(getString(R.string.premium_message))
            .setPositiveButton(getString(R.string.premium_button)) { dialog, _ -> 
                dialog.dismiss()
            }
            .setNegativeButton("İptal") { dialog, _ -> 
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }
    
    private fun setupAnalysisCounter() {
        viewModel.updateAnalysisCounter(requireContext())
    }

    private fun observeViewModel() {
        observeAnalyzeResult()
        observeLoadingState()
        observeAnalysisCounterState()
    }

    private fun observeAnalyzeResult() {
        viewModel.analyzeResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                viewModel.incrementAnalysisCounter(requireContext())
                navigateToAnalysisResult(it)
            }
        }
    }

    private fun observeLoadingState() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            with(binding) {
                imgAI.apply { 
                    alpha = if (isLoading) 0.5f else 1f
                    isEnabled = !isLoading 
                }
                progressAI.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeAnalysisCounterState() {
        viewModel.analysisCounterState.observe(viewLifecycleOwner) { state ->
            with(binding) {
                if (state.showCounter) {
                    tvRemainingAnalyses.visibility = View.VISIBLE
                    tvRemainingAnalyses.text = getString(R.string.used_analyses, state.used)
                    tvPremiumRibbon.visibility = View.GONE
                } else {
                    tvRemainingAnalyses.visibility = View.GONE
                    tvPremiumRibbon.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun navigateToAnalysisResult(result: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToAnalysisResultFragment(
            result,
            viewModel.lastImageUri,
            result
        )
        try {
            findNavController().navigate(action)
        } catch (e: Exception) {
            requireContext().showToast(getString(R.string.error_no_time))
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
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() = takePictureLauncher.launch(null)
    private fun openGallery() = pickImageLauncher.launch(getString(R.string.image_mime_type))

    private fun showSourceChooser() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.photo_selection_title))
            .setMessage(getString(R.string.photo_selection_message))
            .setPositiveButton(getString(R.string.camera_option)) { _, _ ->
                checkCameraPermissionAndOpen()
            }
            .setNeutralButton(getString(R.string.gallery_option)) { _, _ ->
                openGallery()
            }
            .setNegativeButton(getString(R.string.cancel_option), null)
            .show()
    }

    private fun uriToBitmap(uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    requireContext().contentResolver,
                    uri
                )
            )
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }
    } catch (e: Exception) {
        requireContext().showToast(e.message ?: getString(R.string.error_no_time))
        null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
        _binding = null
    }
}
