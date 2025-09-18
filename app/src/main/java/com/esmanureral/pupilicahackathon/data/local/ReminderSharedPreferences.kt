package com.esmanureral.pupilicahackathon.data.local

import android.content.Context
import android.content.SharedPreferences
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.model.ReminderModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReminderSharedPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            context.getString(R.string.prefs_name),
            Context.MODE_PRIVATE
        )

    private val gson = Gson()
    private val activeRemindersKey = context.getString(R.string.prefs_active_reminders_key)

    fun saveActiveReminders(reminders: List<ReminderModel>) {
        try {
            val remindersJson = gson.toJson(reminders)
            prefs.edit().putString(activeRemindersKey, remindersJson).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadActiveReminders(): List<ReminderModel> {
        return try {
            val remindersJson = prefs.getString(activeRemindersKey, null)
            if (!remindersJson.isNullOrEmpty()) {
                val type = object : TypeToken<List<ReminderModel>>() {}.type
                gson.fromJson(remindersJson, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearAllReminders() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}