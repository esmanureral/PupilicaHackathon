package com.esmanureral.pupilicahackathon.domain.usecase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.domain.model.ReminderModel
import com.esmanureral.pupilicahackathon.infrastructure.service.ReminderNotificationService

class NotificationUseCase(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(reminder: ReminderModel) {
        ReminderNotificationService.scheduleReminder(
            context = context,
            reminderId = reminder.id,
            title = reminder.title,
            description = reminder.description,
            hour = reminder.hour,
            minute = reminder.minute,
            daysOfWeek = reminder.daysOfWeek
        )
    }

    fun cancelReminder(reminderId: Int) {
        ReminderNotificationService.cancelReminder(context, reminderId)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    companion object {
        private const val CHANNEL_ID = "dental_reminder_channel"
    }
}
