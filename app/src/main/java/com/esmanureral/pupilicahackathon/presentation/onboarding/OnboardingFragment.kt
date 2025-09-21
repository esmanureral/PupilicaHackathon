package com.esmanureral.pupilicahackathon.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.local.OnboardingPreferences
import com.esmanureral.pupilicahackathon.data.model.OnboardingPage
import com.esmanureral.pupilicahackathon.databinding.FragmentOnboardingBinding
import com.esmanureral.pupilicahackathon.presentation.extensions.dp

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by viewModels {
        OnboardingViewModelFactory(OnboardingPreferences(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializePages()
        setupViewPager()
        setupButtons()
        observeViewModel()
    }

    private fun initializePages() {
        viewModel.createPages(requireContext())
    }

    private fun setupViewPager() {
        viewModel.pages.observe(viewLifecycleOwner) { pages ->
            binding.viewPager.adapter = OnboardingPagerAdapter(pages)
            renderDots(0)
        }
        attachPageChangeListener()
    }

    private fun attachPageChangeListener() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setCurrentPage(position)
            }
        })
    }

    private fun observeViewModel() {
        observeCurrentPage()
        observeOnboardingCompletion()
    }

    private fun observeCurrentPage() {
        viewModel.currentPage.observe(viewLifecycleOwner) { position ->
            updateBackButtonVisibility(position)
            updateNextButtonText(position)
            renderDots(position)
        }
    }

    private fun updateBackButtonVisibility(position: Int) {
        with(binding) {
            btnBack.visibility = if (viewModel.shouldShowBackButton()) View.VISIBLE else View.GONE
        }
    }

    private fun updateNextButtonText(position: Int) {
        with(binding) {
            btnNext.text = viewModel.getNextButtonText(requireContext())
        }
    }

    private fun setupButtons() = with(binding) {
        btnSkip.setOnClickListener { completeOnboarding() }
        btnNext.setOnClickListener { handleNextClick() }
        btnBack.setOnClickListener { handleBackClick() }
    }

    private fun handleNextClick() {
        if (viewModel.isLastPage()) {
            completeOnboarding()
        } else {
            moveToNextPage()
        }
    }

    private fun handleBackClick() {
        if (!viewModel.isFirstPage()) {
            moveToPreviousPage()
        }
    }

    private fun moveToNextPage() {
        with(binding) {
            viewPager.currentItem += 1
        }
    }

    private fun moveToPreviousPage() {
        with(binding) {
            viewPager.currentItem -= 1
        }
    }

    private fun completeOnboarding() {
        viewModel.completeOnboarding()
    }

    private fun renderDots(selectedIndex: Int = 0) {
        val pages = viewModel.pages.value ?: return
        with(binding) {
            dots.removeAllViews()
            repeat(pages.size) { index ->
                dots.addView(createDot(index == selectedIndex))
            }
        }
    }

    private fun createDot(isSelected: Boolean): View {
        val dot = View(requireContext())
        val size = if (isSelected) 8 else 6
        val params = LinearLayout.LayoutParams(size.dp, size.dp).apply {
            leftMargin = 6.dp
            rightMargin = 6.dp
        }
        dot.layoutParams = params
        dot.setBackgroundResource(
            if (isSelected) R.drawable.onb_dot_active else R.drawable.onb_dot_inactive
        )
        return dot
    }

    private fun observeOnboardingCompletion() {
        viewModel.onboardingCompleted.observe(viewLifecycleOwner) { completed ->
            if (completed) navigateToHome()
        }
    }

    private fun navigateToHome() {
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.onboardingFragment, true)
            .build()
        findNavController().navigate(R.id.homeFragment, null, options)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}