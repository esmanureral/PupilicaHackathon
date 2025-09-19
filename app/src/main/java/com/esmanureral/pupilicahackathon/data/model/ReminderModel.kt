package com.esmanureral.pupilicahackathon.data.model

data class ReminderModel(
    val id: Int,
    val title: String,
    val description: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = listOf()
)