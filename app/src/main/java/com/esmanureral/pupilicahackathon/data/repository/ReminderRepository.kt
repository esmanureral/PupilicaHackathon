package com.esmanureral.pupilicahackathon.data.repository

import com.esmanureral.pupilicahackathon.data.model.ReminderModel

interface ReminderRepository {
    suspend fun saveReminder(reminder: ReminderModel): Result<Unit>
    suspend fun loadActiveReminders(): Result<List<ReminderModel>>
    suspend fun clearAllReminders(): Result<Unit>
}