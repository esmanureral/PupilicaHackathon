package com.esmanureral.pupilicahackathon.presentation.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.remote.QuizRepository
import com.esmanureral.pupilicahackathon.data.local.QuizSharedPreferences
import com.esmanureral.pupilicahackathon.data.local.QuizPreferencesImpl
import com.esmanureral.pupilicahackathon.data.remote.ApiClient
import com.esmanureral.pupilicahackathon.databinding.FragmentQuizBinding
import com.esmanureral.pupilicahackathon.presentation.extensions.showToast

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by viewModels {
        QuizViewModelFactory(
            QuizRepository(
                apiService = ApiClient.provideApi(),
                preferences = QuizPreferencesImpl(QuizSharedPreferences(requireContext()))
            )
        )
    }

    private fun optionButtons(): List<Button> = with(binding) {
        listOf(btnOption1, btnOption2, btnOption3, btnOption4)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        observeUiState()
        setupBackButton()
        setupNextButton()
        setupBadgeButton()
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            updateUI(uiState)
        }
    }

    private fun updateUI(uiState: QuizState) = with(binding) {
        progressIndicator.isVisible = uiState.isLoading
        tvQuestionNumber.text = getString(R.string.tv_question, uiState.questionIndex)
        tvScore.text = getString(R.string.quiz_score, uiState.score)

        if (uiState.isFinished && !uiState.isLoading) {
            handleQuizCompleted()
            return@with
        }

        uiState.question?.let { question ->
            tvQuestion.text = question.question
            updateButtonStates(uiState.buttonStates)
        }

        optionButtons().forEach {
            it.visibility = if (uiState.showButtons) View.VISIBLE else View.GONE
        }
        btnNextQuestion.visibility = if (uiState.showNextButton) View.VISIBLE else View.GONE

        uiState.badgeAchievement?.let { achievement ->
            showBadgeCongratulation(achievement)
        }
    }

    private fun setupNextButton() {
        with(binding) {
            btnNextQuestion.setOnClickListener { viewModel.continueNext() }
        }
    }

    private fun setupBackButton() {
        with(binding) {
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun setupBadgeButton() {
        with(binding) {
            btnBadges.setOnClickListener {
                findNavController().navigate(QuizFragmentDirections.actionQuizFragmentToGameBadgeFragment())
            }
        }
    }

    private fun handleQuizCompleted() {
        with(binding) {
            requireContext().showToast(getString(R.string.quiz_completed))
            tvQuestion.text = getString(R.string.quiz_finished)
        }
    }

    private fun updateButtonStates(buttonStates: List<ButtonState>) {

        val buttons = optionButtons()

        buttonStates.forEachIndexed { index, buttonState ->
            if (index < buttons.size) {
                val button = buttons[index]
                button.text = buttonState.text
                button.isEnabled = buttonState.isEnabled
                button.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), buttonState.backgroundColor)
                )
                button.setOnClickListener { buttonState.onClick?.invoke() }
            }
        }
    }

    private fun showBadgeCongratulation(achievement: BadgeAchievement) {
        val action = QuizFragmentDirections.actionQuizFragmentToBadgeCongratulationFragment(
            badgeId = achievement.badgeId,
            badgeName = achievement.badgeName,
            badgeResourceId = achievement.badgeResourceId
        )
        findNavController().navigate(action)

        viewModel.clearBadgeAchievement()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}