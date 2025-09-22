package com.esmanureral.pupilicahackathon.domain.model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("response") val response: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("answer") val answer: String? = null,
    @SerializedName("reply") val reply: String? = null
)