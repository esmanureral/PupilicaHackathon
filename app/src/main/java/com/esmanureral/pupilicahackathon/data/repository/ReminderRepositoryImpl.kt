package com.esmanureral.pupilicahackathon.data.repository

import android.content.Context
import com.esmanureral.pupilicahackathon.data.local.ReminderSharedPreferences
import com.esmanureral.pupilicahackathon.domain.model.ReminderModel
import com.esmanureral.pupilicahackathon.domain.repository.ReminderRepository

class ReminderRepositoryImpl(private val context: Context) : ReminderRepository {

    private val reminderPrefs = ReminderSharedPreferences(context)

    override suspend fun saveReminder(reminder: ReminderModel): Result<Unit> {
        return try {
            val currentReminders = reminderPrefs.loadActiveReminders().toMutableList()
            currentReminders.add(reminder)
            reminderPrefs.saveActiveReminders(currentReminders)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadActiveReminders(): Result<List<ReminderModel>> {
        return try {
            val reminders = reminderPrefs.loadActiveReminders()
            Result.success(reminders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllReminders(): Result<Unit> {
        return try {
            reminderPrefs.clearAllReminders()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}