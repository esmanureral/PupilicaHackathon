package com.esmanureral.pupilicahackathon.ui.quiz

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esmanureral.pupilicahackathon.data.QuizRepository
import com.esmanureral.pupilicahackathon.data.network.QuizQuestion
import kotlinx.coroutines.launch

class QuizViewModel(
    private val repository: QuizRepository
) : ViewModel() {

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

    private val _questionIndex = MutableLiveData(repository.loadQuestionIndex())
    val questionIndex: LiveData<Int> get() = _questionIndex

    private val _answerResult = MutableLiveData<AnswerResult?>(null)
    val answerResult: LiveData<AnswerResult?> get() = _answerResult

    private val _uiState = MutableLiveData(
        QuizState(
            question = null,
            questionIndex = _questionIndex.value ?: 1,
            isLoading = true,
            score = 0,
            answerResult = null,
            isFinished = false
        )
    )
    val uiState: LiveData<QuizState> get() = _uiState

    private val questionList = mutableListOf<QuizQuestion>()
    private val answeredQuestions = mutableListOf<String>()

    init {
        fetchQuestions()
    }

    private fun fetchQuestions() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.fetchQuiz()
                questionList.clear()
                questionList.addAll(response.questions)
                _isLoading.value = false

                val savedAnswered = repository.loadAnsweredQuestions()
                answeredQuestions.clear()
                answeredQuestions.addAll(savedAnswered)

                val savedCurrentId = repository.loadCurrentQuestionId()
                val restored = questionList.find { it.id == savedCurrentId }
                _score.value = repository.loadScore()
                if (restored != null) {
                    _currentQuestion.value = restored
                    val savedSelected = repository.loadCurrentSelectedOption()
                    if (savedSelected != null) {
                        val isCorrect = savedSelected == restored.correctOption
                        _answerResult.value = AnswerResult(
                            selectedKey = savedSelected,
                            correctKey = restored.correctOption,
                            isCorrect = isCorrect
                        )
                    } else {
                        _answerResult.value = null
                    }
                    updateUiState()
                } else {
                    nextQuestion(updateIndex = false)
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error fetching quiz", e)
                _isLoading.value = false
                _error.value = e.localizedMessage
                updateUiState()
            }
        }
    }

    private fun nextQuestion(updateIndex: Boolean = true) {
        val unasked = questionList.filterNot { answeredQuestions.contains(it.question) }
        if (unasked.isEmpty()) {
            _currentQuestion.value = null
            _answerResult.value = null
            updateUiState(isQuizFinished = true, isLoadingOverride = false)
            return
        }

        val next = unasked.random()
        _currentQuestion.value = next
        repository.saveCurrentQuestionId(next.id)
        _answerResult.value = null
        repository.saveCurrentSelectedOption(null)

        if (updateIndex) {
            val newIndex = (_questionIndex.value ?: 1) + 1
            _questionIndex.value = newIndex
            repository.saveQuestionIndex(newIndex)
        }

        updateUiState(isLoadingOverride = false)
    }

    fun selectAnswer(selectedOption: String) {
        val question = _currentQuestion.value ?: return
        answeredQuestions.add(question.question)
        repository.saveAnsweredQuestions(answeredQuestions.toSet())

        val isCorrect = selectedOption == question.correctOption
        if (isCorrect) {
            val newScore = (_score.value ?: 0) + 1
            _score.value = newScore
            repository.saveScore(newScore)
            val currentList = _correctAnswers.value.orEmpty().toMutableList()
            currentList.add(question)
            _correctAnswers.value = currentList
        }

        _answerResult.value = AnswerResult(
            selectedKey = selectedOption,
            correctKey = question.correctOption,
            isCorrect = isCorrect
        )
        repository.saveCurrentSelectedOption(selectedOption)

        updateUiState(isLoadingOverride = false)
    }

    fun continueNext() {
        nextQuestion(updateIndex = true)
    }

    fun resetQuiz() {
        answeredQuestions.clear()
        repository.saveAnsweredQuestions(emptySet())
        _correctAnswers.value = emptyList()
        _score.value = 0
        repository.saveScore(0)
        _questionIndex.value = 1
        repository.saveQuestionIndex(1)
        repository.saveCurrentQuestionId(null)
        nextQuestion(updateIndex = false)
    }

    private fun updateUiState(
        isQuizFinished: Boolean? = null,
        isLoadingOverride: Boolean? = null
    ) {
        val finished =
            isQuizFinished ?: (_currentQuestion.value == null && (questionList.isNotEmpty()))
        _uiState.value = QuizState(
            question = _currentQuestion.value,
            questionIndex = _questionIndex.value ?: 1,
            isLoading = isLoadingOverride ?: (_isLoading.value ?: false),
            score = _score.value ?: 0,
            answerResult = _answerResult.value,
            isFinished = finished
        )
    }
}

class QuizViewModelFactory(
    private val repository: QuizRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuizViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}