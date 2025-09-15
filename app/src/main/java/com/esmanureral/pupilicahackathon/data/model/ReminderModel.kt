package com.esmanureral.pupilicahackathon.data.model

import android.content.Context
import com.esmanureral.pupilicahackathon.R
import java.util.Locale

data class ReminderModel(
    val id: Int,
    val title: String,
    val description: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = listOf()
) {
    fun getTimeString(): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    fun getDaysString(context: Context): String {
        val dayNames = listOf(
            context.getString(R.string.day_sunday),
            context.getString(R.string.day_monday),
            context.getString(R.string.day_tuesday),
            context.getString(R.string.day_wednesday),
            context.getString(R.string.day_thursday),
            context.getString(R.string.day_friday),
            context.getString(R.string.day_saturday)
        )

        return if (daysOfWeek.isEmpty()) {
            context.getString(R.string.every_day)
        } else {
            daysOfWeek.map { dayNames[it] }.joinToString(", ")
        }
    }
}
