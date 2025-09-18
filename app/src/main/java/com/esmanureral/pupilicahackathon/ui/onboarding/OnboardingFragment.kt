package com.esmanureral.pupilicahackathon.ui.onboarding

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
import com.esmanureral.pupilicahackathon.dp

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by viewModels {
        OnboardingViewModelFactory(OnboardingPreferences(requireContext()))
    }

    private lateinit var pages: List<OnboardingPage>

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

        pages = createPages()
        setupViewPager()
        setupButtons()
        observeOnboardingCompletion()
    }

    private fun createPages(): List<OnboardingPage> = listOf(
        OnboardingPage(
            R.raw.tooth_analys,
            getString(R.string.onb_title_1),
            getString(R.string.onb_desc_1),
            requireContext().getColor(R.color.onb_yellow)
        ),
        OnboardingPage(
            R.raw.tooth_asistants,
            getString(R.string.onb_title_2),
            getString(R.string.onb_desc_2),
            requireContext().getColor(R.color.onb_blue)
        ),
        OnboardingPage(
            R.raw.tooth_brush,
            getString(R.string.onb_title_3),
            getString(R.string.onb_desc_3),
            requireContext().getColor(R.color.onb_pink)
        ),
        OnboardingPage(
            R.raw.game,
            getString(R.string.onb_title_4),
            getString(R.string.onb_desc_4),
            requireContext().getColor(R.color.onb_blue)
        )
    )

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingPagerAdapter(pages)
        renderDots(0)
        attachPageChangeListener()
    }

    private fun attachPageChangeListener() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageUI(position)
            }
        })
    }

    private fun updatePageUI(position: Int) {
        updateBackButtonVisibility(position)
        updateNextButtonText(position)
        renderDots(position)
    }

    private fun updateBackButtonVisibility(position: Int) {
        binding.btnBack.visibility = if (position == 0) View.GONE else View.VISIBLE
    }

    private fun updateNextButtonText(position: Int) {
        binding.btnNext.text =
            if (position == pages.lastIndex) getString(R.string.get_started) else getString(R.string.next)
    }

    private fun setupButtons() = with(binding) {
        btnSkip.setOnClickListener { completeOnboarding() }
        btnNext.setOnClickListener { onNextClicked() }
        btnBack.setOnClickListener { onBackClicked() }
    }

    private fun onNextClicked() {
        if (binding.viewPager.currentItem == pages.lastIndex) {
            completeOnboarding()
        } else {
            moveToNextPage()
        }
    }

    private fun onBackClicked() {
        if (binding.viewPager.currentItem > 0) {
            moveToPreviousPage()
        }
    }

    private fun moveToNextPage() {
        binding.viewPager.currentItem += 1
    }

    private fun moveToPreviousPage() {
        binding.viewPager.currentItem -= 1
    }

    private fun completeOnboarding() {
        viewModel.completeOnboarding()
    }

    private fun renderDots(selectedIndex: Int = 0) {
        val container = binding.dots
        container.removeAllViews()
        repeat(pages.size) { index -> container.addView(createDot(index == selectedIndex)) }
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