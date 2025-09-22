package com.esmanureral.pupilicahackathon.domain.model

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    @SerializedName("id") val id: String = "",
    @SerializedName("text") val text: String,
    @SerializedName("is_from_user") val isFromUser: Boolean,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)
