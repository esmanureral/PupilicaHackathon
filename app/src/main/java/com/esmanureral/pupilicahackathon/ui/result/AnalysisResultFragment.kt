package com.esmanureral.pupilicahackathon.ui.result

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.model.AnalysisResult
import com.esmanureral.pupilicahackathon.databinding.FragmentAnalysisResultBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisResultFragment : Fragment() {

    private var _binding: FragmentAnalysisResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalysisResultViewModel by viewModels { AnalysisResultViewModelFactory() }
    private lateinit var weeklyPlanAdapter: WeeklyPlanAdapter

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

        initializeAdapter()
        initArgs()
        setupBackButton()
        setupShareButton()
        observeAnalysisResult()
        observeImage()
        observeLoadedBitmap()
        observeShareData()
    }

    private fun initializeAdapter() {
        weeklyPlanAdapter = WeeklyPlanAdapter()
        with(binding) {
            rvPlan.adapter = weeklyPlanAdapter
        }
    }

    private fun initArgs() {
        val args = AnalysisResultFragmentArgs.fromBundle(requireArguments())
        val resultText = args.resultText
        viewModel.initializeData(resultText, args.imageUri)
    }

    private fun setupBackButton() {
        setupBackPressedCallback()
        setupBackClickListener()
    }

    private fun setupBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goToHome()
            }
        )
    }

    private fun setupBackClickListener() {
        with(binding) {
            ivBack.setOnClickListener { goToHome() }
        }
    }

    private fun setupShareButton() {
        with(binding) {
            btnShareWithDoctor.setOnClickListener {
                viewModel.prepareShareData(requireContext())
            }
        }
    }

    private fun observeShareData() {
        viewModel.shareFileUri.observe(viewLifecycleOwner) { fileUri ->
            shareIfReady(fileUri)
        }
    }

    private fun shareIfReady(fileUri: Uri?) {
        if (fileUri != null) {
            shareAnalysisResults(fileUri)
        }
    }

    private fun shareAnalysisResults(fileUri: Uri) {
        val result = viewModel.analysisResult.value ?: return
        val shareContent = generateShareContent(result)
        val shareIntent = createShareIntent(shareContent, fileUri)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with_doctor)))
    }

    private fun generateShareContent(result: AnalysisResult): String {
        val dateFormat = SimpleDateFormat(getString(R.string.date_format), Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        return buildString {
            appendLine(getString(R.string.share_content_title))
            appendLine(getString(R.string.share_content_date_prefix, currentDate))
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
                    appendLine(getString(R.string.share_content_plan_item, plan.day, plan.task))
                }
                appendLine()
            }

            appendLine(getString(R.string.share_content_note))
        }
    }

    private fun createShareIntent(content: String, fileUri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun goToHome() {
        findNavController().navigate(
            R.id.action_analysisResultFragment_to_homeFragment
        )
    }

    private fun observeAnalysisResult() {
        viewModel.analysisResult.observe(viewLifecycleOwner) { result ->
            updateSummary(result)
            updatePredictions(result)
            updateWeeklyPlan(result)
            updateVideoButton(result)
        }
    }

    private fun updateSummary(result: AnalysisResult) {
        with(binding) {
            tvSummary.text = if (result.summary.isBlank())
                getString(R.string.result_not_found)
            else
                result.summary
        }
    }

    private fun updatePredictions(result: AnalysisResult) {
        with(binding) {
            tvPredictions.text = result.predictions
        }
    }

    private fun updateWeeklyPlan(result: AnalysisResult) {
        with(binding) {
            tvPlanTitle.visibility = if (result.weeklyPlan.isNotEmpty()) View.VISIBLE else View.GONE
            if (result.weeklyPlan.isNotEmpty()) {
                tvPlanTitle.text = getString(R.string.weekly_care_plan)
            }
        }
        weeklyPlanAdapter.updateData(result.weeklyPlan)
    }

    private fun updateVideoButton(result: AnalysisResult) {
        with(binding) {
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

    private fun observeImage() {
        viewModel.imageUri.observe(viewLifecycleOwner) { uri ->
            loadImageIfExists(uri)
        }
    }

    private fun loadImageIfExists(uri: String?) {
        if (!uri.isNullOrBlank()) {
            val parsedUri = Uri.parse(uri)
            viewModel.loadBitmapFromUri(requireContext(), parsedUri)
        }
    }

    private fun observeLoadedBitmap() {
        viewModel.loadedBitmap.observe(viewLifecycleOwner) { bitmap ->
            setImageToView(bitmap)
        }
    }

    private fun setImageToView(bitmap: android.graphics.Bitmap?) {
        with(binding) {
            if (bitmap != null) {
                ivAnalyzed.setImageBitmap(bitmap)
                ivAnalyzed.visibility = View.VISIBLE
            } else {
                ivAnalyzed.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
