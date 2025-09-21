package com.esmanureral.pupilicahackathon.presentation.quiz

import com.esmanureral.pupilicahackathon.data.model.QuizQuestion

data class AnswerResult(
    val selectedKey: String?,
    val correctKey: String?,
    val isCorrect: Boolean
)

data class QuizState(
    val question: QuizQuestion? = null,
    val questionIndex: Int = 1,
    val isLoading: Boolean = false,
    val score: Int = 0,
    val answerResult: AnswerResult? = null,
    val isFinished: Boolean = false,
    val buttonStates: List<ButtonState> = emptyList(),
    val showButtons: Boolean = true,
    val showNextButton: Boolean = true,
    val badgeAchievement: BadgeAchievement? = null
)

data class BadgeAchievement(
    val badgeId: String,
    val badgeName: String,
    val badgeResourceId: Int
)

data class ButtonState(
    val text: String,
    val isEnabled: Boolean,
    val backgroundColor: Int, // Color resource ID
    val onClick: (() -> Unit)? = null
)