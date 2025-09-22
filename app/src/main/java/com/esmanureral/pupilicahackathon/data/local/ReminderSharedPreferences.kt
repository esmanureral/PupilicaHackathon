package com.esmanureral.pupilicahackathon.data.local

import android.content.Context
import android.content.SharedPreferences
import com.esmanureral.pupilicahackathon.domain.model.ReminderModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class ReminderSharedPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    fun saveActiveReminders(reminders: List<ReminderModel>) {
        val remindersJson = gson.toJson(reminders)
        prefs.edit { putString(ACTIVE_REMINDERS_KEY, remindersJson) }
    }

    fun loadActiveReminders(): List<ReminderModel> {
        val remindersJson = prefs.getString(ACTIVE_REMINDERS_KEY, null)
        return if (!remindersJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<ReminderModel>>() {}.type
            gson.fromJson(remindersJson, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun clearAllReminders() {
        prefs.edit { remove(ACTIVE_REMINDERS_KEY) }
    }

    private companion object {
        private const val PREFS_NAME = "reminder_prefs"
        private const val ACTIVE_REMINDERS_KEY = "active_reminders"
    }
}