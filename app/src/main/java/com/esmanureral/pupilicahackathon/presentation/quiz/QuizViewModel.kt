package com.esmanureral.pupilicahackathon.presentation.quiz

import android.util.Log
import androidx.lifecycle.*
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.remote.QuizRepository
import com.esmanureral.pupilicahackathon.data.model.QuizQuestion
import com.esmanureral.pupilicahackathon.presentation.badge.GameBadgeList
import kotlinx.coroutines.launch

class QuizViewModel(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(
        QuizState(
            question = null,
            questionIndex = repository.loadQuestionIndex(),
            isLoading = true,
            score = 0,
            answerResult = null,
            isFinished = false
        )
    )
    val uiState: LiveData<QuizState> get() = _uiState

    private val questionList = mutableListOf<QuizQuestion>()
    private val answeredQuestions = mutableListOf<String>()
    private val unlockedBadges = mutableSetOf<String>()

    init {
        fetchQuestions()
        loadUnlockedBadges()
    }

    private fun updateUiState(
        question: QuizQuestion? = null,
        questionIndex: Int? = null,
        isLoading: Boolean? = null,
        score: Int? = null,
        answerResult: AnswerResult? = null,
        isFinished: Boolean? = null
    ) {
        val currentState = _uiState.value ?: QuizState()
        val newState = currentState.copy(
            question = question ?: currentState.question,
            questionIndex = questionIndex ?: currentState.questionIndex,
            isLoading = isLoading ?: currentState.isLoading,
            score = score ?: currentState.score,
            answerResult = answerResult,
            isFinished = isFinished ?: currentState.isFinished
        )

        val badgeAchievement = if (score != null && score != currentState.score) {
            checkBadgeAchievement(newState.score, currentState.score)
        } else {
            currentState.badgeAchievement
        }

        _uiState.value = newState.copy(
            buttonStates = createButtonStates(newState),
            showButtons = !newState.isFinished,
            showNextButton = newState.answerResult != null && !newState.isFinished,
            badgeAchievement = badgeAchievement
        )
    }

    private fun fetchQuestions() {
        updateUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val response = repository.fetchQuiz()
                questionList.clear()
                questionList.addAll(response.questions)

                val savedAnswered = repository.loadAnsweredQuestions()
                answeredQuestions.clear()
                answeredQuestions.addAll(savedAnswered)

                val savedCurrentId = repository.loadCurrentQuestionId()
                val restored = questionList.find { it.id == savedCurrentId }
                val currentScore = repository.loadScore()

                if (restored != null) {
                    val savedSelected = repository.loadCurrentSelectedOption()
                    val answerResult = if (savedSelected != null) {
                        val isCorrect = savedSelected == restored.correctOption
                        AnswerResult(
                            selectedKey = savedSelected,
                            correctKey = restored.correctOption,
                            isCorrect = isCorrect
                        )
                    } else null

                    updateUiState(
                        question = restored,
                        score = currentScore,
                        answerResult = answerResult,
                        isLoading = false
                    )
                } else {
                    nextQuestion(updateIndex = false)
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error fetching quiz", e)
                updateUiState(isLoading = false)
            }
        }
    }

    private fun nextQuestion(updateIndex: Boolean = true) {
        val unasked = questionList.filterNot { answeredQuestions.contains(it.question) }
        if (unasked.isEmpty()) {
            updateUiState(
                question = null,
                answerResult = null,
                isFinished = true,
                isLoading = false
            )
            return
        }

        val next = unasked.random()
        repository.saveCurrentQuestionId(next.id)
        repository.saveCurrentSelectedOption(null)

        val newIndex = if (updateIndex) {
            val currentIndex = _uiState.value?.questionIndex ?: 1
            val newIndexValue = currentIndex + 1
            repository.saveQuestionIndex(newIndexValue)
            newIndexValue
        } else {
            _uiState.value?.questionIndex ?: 1
        }

        updateUiState(
            question = next,
            questionIndex = newIndex,
            answerResult = null,
            isLoading = false
        )
    }

    fun selectAnswer(selectedOption: String) {
        val question = _uiState.value?.question ?: return
        answeredQuestions.add(question.question)
        repository.saveAnsweredQuestions(answeredQuestions.toSet())

        val isCorrect = selectedOption == question.correctOption
        val newScore = if (isCorrect) {
            val currentScore = _uiState.value?.score ?: 0
            val updatedScore = currentScore + 1
            repository.saveScore(updatedScore)
            updatedScore
        } else {
            _uiState.value?.score ?: 0
        }

        val answerResult = AnswerResult(
            selectedKey = selectedOption,
            correctKey = question.correctOption,
            isCorrect = isCorrect
        )
        repository.saveCurrentSelectedOption(selectedOption)

        updateUiState(
            score = newScore,
            answerResult = answerResult,
            isLoading = false
        )
    }

    fun continueNext() {
        nextQuestion(updateIndex = true)
    }

    fun resetQuiz() {
        answeredQuestions.clear()
        repository.saveAnsweredQuestions(emptySet())
        repository.saveScore(0)
        repository.saveQuestionIndex(1)
        repository.saveCurrentQuestionId(null)

        updateUiState(
            score = 0,
            questionIndex = 1,
            isFinished = false
        )
        nextQuestion(updateIndex = false)
    }

    private fun createButtonStates(uiState: QuizState): List<ButtonState> {
        val question = uiState.question ?: return emptyList()
        val keys = question.options.keys.toList()

        return keys.mapIndexed { index, key ->
            val text = question.options[key] ?: ""
            val isEnabled = uiState.answerResult == null && !uiState.isFinished

            val backgroundColor = when {
                uiState.answerResult == null -> R.color.gray
                uiState.answerResult.selectedKey == key -> {
                    if (uiState.answerResult.isCorrect) R.color.green_light else R.color.red_light
                }

                uiState.answerResult.correctKey == key -> R.color.green_light
                else -> R.color.gray
            }

            ButtonState(
                text = text,
                isEnabled = isEnabled,
                backgroundColor = backgroundColor,
                onClick = if (isEnabled) {
                    { selectAnswer(key) }
                } else null
            )
        }
    }

    private fun checkBadgeAchievement(newScore: Int, oldScore: Int): BadgeAchievement? {
        if (newScore <= oldScore || newScore == 0) return null

        val badges = GameBadgeList.getBadges()

        val newlyUnlockedBadges = badges.filter { badge ->
            oldScore < badge.minValue &&
                    newScore == badge.minValue &&
                    newScore <= badge.maxValue &&
                    !unlockedBadges.contains(badge.id)
        }


        return newlyUnlockedBadges.firstOrNull()?.let { badge ->
            saveUnlockedBadge(badge.id)
            BadgeAchievement(
                badgeId = badge.id,
                badgeName = badge.nameResId.toString(),
                badgeResourceId = badge.resourceId
            )
        }
    }

    fun clearBadgeAchievement() {
        val currentState = _uiState.value ?: QuizState()
        _uiState.value = currentState.copy(badgeAchievement = null)
    }

    private fun loadUnlockedBadges() {
        val currentScore = repository.loadScore()
        val badges = GameBadgeList.getBadges()

        unlockedBadges.clear()
        badges.forEach { badge ->
            if (currentScore >= badge.minValue && currentScore <= badge.maxValue) {
                unlockedBadges.add(badge.id)
            }
        }
    }

    private fun saveUnlockedBadge(badgeId: String) {
        unlockedBadges.add(badgeId)
    }
}

class QuizViewModelFactory(
    private val repository: QuizRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuizViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}