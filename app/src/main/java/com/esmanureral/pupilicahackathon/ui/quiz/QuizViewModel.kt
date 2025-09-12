package com.esmanureral.pupilicahackathon.ui.quiz

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esmanureral.pupilicahackathon.data.network.ApiClient
import com.esmanureral.pupilicahackathon.data.network.QuizQuestion
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {

    private val apiService = ApiClient.provideApi()

    private val _currentQuestion = MutableLiveData<QuizQuestion?>()
    val currentQuestion: LiveData<QuizQuestion?> get() = _currentQuestion

    private val _correctAnswers = MutableLiveData<List<QuizQuestion>>(emptyList())
    val correctAnswers: LiveData<List<QuizQuestion>> get() = _correctAnswers

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> get() = _score

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val questionList = mutableListOf<QuizQuestion>()
    private val answeredQuestions = mutableListOf<String>()

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.fetchQuiz()
                questionList.addAll(response.questions)
                nextQuestion()
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error fetching quiz", e)
                _error.value = e.localizedMessage
            }
        }
    }

    fun nextQuestion() {
        val unasked = questionList.filterNot { answeredQuestions.contains(it.question) }
        if (unasked.isEmpty()) {
            _currentQuestion.value = null
            return
        }

        val next = unasked.random()
        _currentQuestion.value = next
    }

    fun submitAnswer(selectedOption: String) {
        val question = _currentQuestion.value ?: return
        answeredQuestions.add(question.question)

        if (selectedOption == question.correctOption) {
            _score.value = (_score.value ?: 0) + 1
            val currentList = _correctAnswers.value.orEmpty().toMutableList()
            currentList.add(question)
            _correctAnswers.value = currentList
        } else {
            _score.value = (_score.value ?: 0) - 1
        }

        nextQuestion()
    }

    fun resetQuiz() {
        answeredQuestions.clear()
        _correctAnswers.value = emptyList()
        _score.value = 0
        nextQuestion()
    }
}