package com.esmanureral.pupilicahackathon.ui.quiz

import com.esmanureral.pupilicahackathon.data.network.QuizQuestion

data class AnswerResult(
    val selectedKey: String?,
    val correctKey: String?,
    val isCorrect: Boolean
)

data class QuizState(
    val question: QuizQuestion?,
    val questionIndex: Int,
    val isLoading: Boolean,
    val score: Int,
    val answerResult: AnswerResult?,
    val isFinished: Boolean
)