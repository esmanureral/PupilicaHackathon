package com.esmanureral.pupilicahackathon.presentation.feature.reminder

import android.content.Context
import com.esmanureral.pupilicahackathon.infrastructure.notification.NotificationHelper

class ReminderSystemManager(private val context: Context) {

    fun initialize() {
        setupNotificationChannel()
    }

    private fun setupNotificationChannel() {
        try {
            NotificationHelper.createReminderChannel(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}