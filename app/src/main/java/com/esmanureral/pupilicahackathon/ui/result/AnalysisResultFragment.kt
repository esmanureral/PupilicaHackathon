package com.esmanureral.pupilicahackathon.ui.result

import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
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

class AnalysisResultFragment : Fragment() {

    private var _binding: FragmentAnalysisResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalysisResultViewModel by viewModels()

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
                binding.ivAnalyzed.setImageBitmap(bitmap)
            } catch (_: Exception) {
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
