package com.esmanureral.pupilicahackathon.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.local.QuizSharedPreferences
import com.esmanureral.pupilicahackathon.databinding.FragmentQuizBinding

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private lateinit var prefs: QuizSharedPreferences
    private var correctAnswer: String? = null
    private var questionIndex = 1

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

        prefs = QuizSharedPreferences(requireContext())
        questionIndex = prefs.loadQuestionIndex()
        binding.tvQuestionNumber.text = getString(R.string.tv_question, questionIndex)

        setupButtons()
    }

    private fun setupButtons() {
        val optionButtons = with(binding) {
            listOf(btnOption1, btnOption2, btnOption3, btnOption4)
        }

        fun checkAnswer(selectedButton: Button, selectedOption: String) {
            val question = viewModel.currentQuestion.value ?: return
            correctAnswer = question.correctOption

            if (selectedOption == correctAnswer) {
                selectedButton.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.correct_answer)
                )
            } else {
                selectedButton.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.wrong_answer)
                )
                optionButtons.find { it.text == question.options[correctAnswer] }
                    ?.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.correct_answer)
                    )
            }
            optionButtons.forEach { it.isEnabled = false }


            binding.btnNextQuestion.setOnClickListener {
                viewModel.submitAnswer(selectedOption)
                questionIndex++
                binding.tvQuestionNumber.text = getString(R.string.tv_question, questionIndex)
                prefs.saveQuestionIndex(questionIndex)

            }
        }

        fun setOptions(options: Map<String, String>) {
            val keys = options.keys.toList()
            for (i in optionButtons.indices) {
                if (i < keys.size) {
                    optionButtons[i].text = options[keys[i]]
                    optionButtons[i].visibility = View.VISIBLE
                    optionButtons[i].isEnabled = true
                    optionButtons[i].setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.icon_color)
                    )
                    optionButtons[i].setOnClickListener {
                        checkAnswer(optionButtons[i], keys[i])
                    }
                } else {
                    optionButtons[i].visibility = View.GONE
                }
            }
        }

        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            binding.progressIndicator.isVisible = false

            if (question != null) {
                binding.tvQuestion.text = question.question
                setOptions(question.options)
                optionButtons.forEach { it.visibility = View.VISIBLE }
            } else {
                Toast.makeText(requireContext(), "Quiz tamamlandı!", Toast.LENGTH_SHORT).show()
                binding.tvQuestion.text = "Quiz bitti!"
                optionButtons.forEach { it.visibility = View.GONE }
            }
        }


        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnNextQuestion.setOnClickListener {
            viewModel.nextQuestion()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}