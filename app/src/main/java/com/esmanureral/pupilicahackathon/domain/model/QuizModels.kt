package com.esmanureral.pupilicahackathon.domain.model

import com.google.gson.annotations.SerializedName

data class QuizResponse(
    val questions: List<QuizQuestion>
)

data class QuizQuestion(
    @SerializedName("id")
    val id: String,
    @SerializedName("question")
    val question: String,
    @SerializedName("options")
    val options: Map<String, String>,
    @SerializedName("correct_option")
    val correctOption: String,
    @SerializedName("explanation")
    val explanation: String? = null
)