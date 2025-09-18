package com.esmanureral.pupilicahackathon.ui.result

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.databinding.FragmentAnalysisResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisResultFragment : Fragment() {

    private var _binding: FragmentAnalysisResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalysisResultViewModel by viewModels { AnalysisResultViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initArgs()
        setupBackButton()
        setupShareButton()
        observeAnalysisResult()
        observeImage()
    }

    private fun initArgs() {
        val args = AnalysisResultFragmentArgs.fromBundle(requireArguments())
        val resultText = args.resultText
        viewModel.initializeData(resultText, args.imageUri)
    }

    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goToHome()
            }
        )
        binding.ivBack.setOnClickListener { goToHome() }
    }

    private fun setupShareButton() {
        binding.btnShareWithDoctor.setOnClickListener {
            shareAnalysisResults()
        }
    }

    private fun goToHome() {
        findNavController().navigate(
            R.id.action_analysisResultFragment_to_homeFragment
        )
    }

    private fun observeAnalysisResult() {
        viewModel.analysisResult.observe(viewLifecycleOwner) { result ->
            with(binding) {
                tvSummary.text =
                    if (result.summary.isBlank()) getString(R.string.result_not_found)
                    else result.summary

                tvPredictions.text = result.predictions

                tvPlanTitle.visibility =
                    if (result.weeklyPlan.isNotEmpty()) View.VISIBLE else View.GONE
                if (result.weeklyPlan.isNotEmpty()) {
                    tvPlanTitle.text = getString(R.string.weekly_care_plan)
                }

                rvPlan.adapter = WeeklyPlanAdapter(result.weeklyPlan)

                if (result.videoUrl.isNotBlank()) {
                    btnWatchVideo.visibility = View.VISIBLE
                    btnWatchVideo.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.videoUrl)))
                    }
                } else {
                    btnWatchVideo.visibility = View.GONE
                }
            }
        }
    }


    private fun observeImage() {
        viewModel.imageUri.observe(viewLifecycleOwner) { uri ->
            loadImage(uri)
        }
    }

    private fun loadImage(imageUri: String?) {
        if (imageUri.isNullOrBlank()) return
        val uri = Uri.parse(imageUri)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { loadBitmapFromUri(uri) }
                bitmap?.let { binding.ivAnalyzed.setImageBitmap(it) }
            } catch (_: Exception) {
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
        } catch (e: Exception) {
            android.util.Log.e("AnalysisResultFragment", "Error loading bitmap from URI", e)
            null
        }
    }

    private fun shareAnalysisResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val analysisResult = viewModel.analysisResult.value ?: return@launch
                val imageUri = viewModel.imageUri.value
                
                val shareContent = createShareContent(analysisResult)
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
                    putExtra(Intent.EXTRA_TEXT, shareContent)
                    
                    if (!imageUri.isNullOrBlank()) {
                        val uri = Uri.parse(imageUri)
                        val bitmap = withContext(Dispatchers.IO) { 
                            try {
                                loadBitmapFromUri(uri)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        bitmap?.let { bmp ->
                            val imageFile = saveBitmapToFile(bmp)
                            imageFile?.let { file ->
                                val fileUri = FileProvider.getUriForFile(
                                    requireContext(),
                                    "${requireContext().packageName}.fileprovider",
                                    file
                                )
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                type = "image/*"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }
                    }
                }
                
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with_doctor)))
                
            } catch (e: Exception) {
                // Handle error silently or show a toast
                android.util.Log.e("AnalysisResultFragment", "Error sharing results", e)
            }
        }
    }

    private fun createShareContent(result: com.esmanureral.pupilicahackathon.data.model.AnalysisResult): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        val content = StringBuilder().apply {
            appendLine(getString(R.string.share_content_title))
            appendLine("Tarih: $currentDate")
            appendLine()
            
            if (result.summary.isNotBlank()) {
                appendLine(getString(R.string.share_content_summary))
                appendLine(result.summary)
                appendLine()
            }
            
            if (result.predictions.isNotBlank()) {
                appendLine(getString(R.string.share_content_predictions))
                appendLine(result.predictions)
                appendLine()
            }
            
            if (result.weeklyPlan.isNotEmpty()) {
                appendLine(getString(R.string.share_content_plan))
                result.weeklyPlan.forEach { plan ->
                    appendLine("• ${plan.day}: ${plan.task}")
                }
                appendLine()
            }
            
            appendLine(getString(R.string.share_content_note))
        }
        
        return content.toString()
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "dental_analysis_$timestamp.jpg"
            val file = File(requireContext().cacheDir, fileName)
            
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            file
        } catch (e: Exception) {
            android.util.Log.e("AnalysisResultFragment", "Error saving bitmap", e)
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
