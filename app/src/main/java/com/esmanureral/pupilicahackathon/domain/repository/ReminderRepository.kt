package com.esmanureral.pupilicahackathon.domain.repository

import com.esmanureral.pupilicahackathon.domain.model.ReminderModel

interface ReminderRepository {
    suspend fun saveReminder(reminder: ReminderModel): Result<Unit>
    suspend fun loadActiveReminders(): Result<List<ReminderModel>>
    suspend fun clearAllReminders(): Result<Unit>
}