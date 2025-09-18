package com.esmanureral.pupilicahackathon.data.model

data class ChatMessage(
    val id: String = "",
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
