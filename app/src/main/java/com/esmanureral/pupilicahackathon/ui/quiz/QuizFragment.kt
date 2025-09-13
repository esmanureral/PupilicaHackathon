package com.esmanureral.pupilicahackathon.ui.quiz

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
import com.esmanureral.pupilicahackathon.data.QuizRepository
import com.esmanureral.pupilicahackathon.data.local.QuizSharedPreferences
import com.esmanureral.pupilicahackathon.data.network.ApiClient
import com.esmanureral.pupilicahackathon.data.network.QuizQuestion
import com.esmanureral.pupilicahackathon.databinding.FragmentQuizBinding
import com.esmanureral.pupilicahackathon.showToast

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels {
        QuizViewModelFactory(
            QuizRepository(
                apiService = ApiClient.provideApi(),
                prefs = QuizSharedPreferences(requireContext())
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
        observeErrors()
        setupNextButton()
        setOnClickListener()
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            showLoading(uiState.isLoading)
            showQuestionNumber(uiState.questionIndex)
            showScore(uiState.score)

            val question = uiState.question
            if (uiState.isFinished && !uiState.isLoading) {
                handleQuizCompleted()
                return@observe
            }

            question?.let {
                showQuestion(it)
                bindQuestionOptions(optionButtons(), it.options) { key ->
                    viewModel.selectAnswer(key)
                }
                uiState.answerResult?.let { result ->
                    showAnswerResult(optionButtons(), it.options.keys.toList(), result)
                }
            }
        }
    }

    private fun observeErrors() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setOnClickListener(){
        binding.btnBadges.setOnClickListener {
            findNavController().navigate(QuizFragmentDirections.actionQuizFragmentToGameBadgeFragment())

        }
    }
    private fun setupNextButton() {
        binding.btnNextQuestion.setOnClickListener { viewModel.continueNext() }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.isVisible = isLoading
    }

    private fun showQuestionNumber(index: Int) {
        binding.tvQuestionNumber.text = getString(R.string.tv_question, index)
    }

    private fun showScore(score: Int) {
        binding.tvScore.text = getString(R.string.quiz_score, score)
    }

    private fun showQuestion(question: QuizQuestion) {
        binding.tvQuestion.text = question.question
    }

    private fun handleQuizCompleted() = with(binding) {
        requireContext().showToast(getString(R.string.quiz_completed))
        tvQuestion.text = getString(R.string.quiz_finished)
        optionButtons().forEach { it.visibility = View.GONE }
        btnNextQuestion.visibility = View.GONE
    }

    private fun showError(error: String) {
        requireContext().showToast(error)
    }

    private fun bindQuestionOptions(
        buttons: List<Button>,
        options: Map<String, String>,
        onClickKey: (String) -> Unit
    ) {
        setOptionTexts(buttons, options)
        setOptionClickListeners(buttons, options, onClickKey)
    }

    private fun setOptionTexts(buttons: List<Button>, options: Map<String, String>) {
        val keys = options.keys.toList()
        buttons.forEachIndexed { index, button ->
            button.text = options[keys[index]]
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
            button.isEnabled = true
            button.visibility = View.VISIBLE
        }
    }

    private fun setOptionClickListeners(
        buttons: List<Button>,
        options: Map<String, String>,
        onClickKey: (String) -> Unit
    ) {
        val keys = options.keys.toList()
        buttons.forEachIndexed { index, button ->
            button.setOnClickListener { onClickKey(keys[index]) }
        }
    }

    private fun showAnswerResult(
        buttons: List<Button>,
        keys: List<String>,
        result: AnswerResult
    ) {
        buttons.forEach { it.isEnabled = false }
        val selectedIdx = keys.indexOf(result.selectedKey)
        val correctIdx = keys.indexOf(result.correctKey)

        if (selectedIdx in buttons.indices) {
            buttons[selectedIdx].setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (result.isCorrect) R.color.green_light else R.color.red_light
                )
            )
        }
        if (correctIdx in buttons.indices) {
            buttons[correctIdx].setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.green_light)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
