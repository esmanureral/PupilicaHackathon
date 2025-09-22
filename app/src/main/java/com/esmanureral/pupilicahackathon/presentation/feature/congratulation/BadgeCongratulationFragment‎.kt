package com.esmanureral.pupilicahackathon.presentation.feature.congratulation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.databinding.FragmentBadgeCongratulationBinding

class BadgeCongratulationFragment : Fragment() {

    private var _binding: FragmentBadgeCongratulationBinding? = null
    private val binding get() = _binding!!

    private val args: BadgeCongratulationFragmentArgs by navArgs()
    private val viewModel: BadgeCongratulationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBadgeCongratulationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBadgeImage()
        setupBadgeName()
        setupCongratulationText()
        startConfettiAnimation()
        setupContinueButton()
        setupViewBadgesButton()
    }

    private fun setupBadgeImage() {
        with(binding) {
            badgeImage.setImageResource(args.badgeResourceId)
        }
    }

    private fun setupBadgeName() {
        with(binding) {
            if (viewModel.shouldShowBadgeNameAsResource(args.badgeName)) {
                val badgeNameResId = args.badgeName.toInt()
                badgeNameText.text = getString(badgeNameResId)
            } else {
                badgeNameText.text = args.badgeName
            }
        }
    }

    private fun setupCongratulationText() {
        with(binding) {
            congratulationText.text = getString(R.string.congratulations_badge_unlocked)
            newBadgeText.text = getString(R.string.new_badge_unlocked)
        }
    }

    private fun startConfettiAnimation() {
        with(binding) {
            confettiAnimation.playAnimation()
        }
    }

    private fun setupContinueButton() {
        with(binding) {
            btnContinue.setOnClickListener {
                stopAnimationAndNavigateUp()
            }
        }
    }

    private fun setupViewBadgesButton() {
        with(binding) {
            btnViewBadges.setOnClickListener {
                stopAnimationAndNavigateToBadges()
            }
        }
    }

    private fun stopAnimationAndNavigateUp() {
        with(binding) {
            confettiAnimation.cancelAnimation()
            viewModel.stopAnimation()
        }
        findNavController().navigateUp()
    }

    private fun stopAnimationAndNavigateToBadges() {
        with(binding) {
            confettiAnimation.cancelAnimation()
            viewModel.stopAnimation()
        }
        findNavController().navigate(R.id.action_badgeCongratulationFragment_to_gameBadgeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}