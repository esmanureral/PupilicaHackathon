package com.esmanureral.pupilicahackathon.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.esmanureral.pupilicahackathon.MainActivity
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.notification.NotificationHelper as CustomNotificationManager
import java.util.*

class ReminderNotificationService : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == context.getString(R.string.reminder_action)) {
            val reminderId = intent.getIntExtra(context.getString(R.string.reminder_extra_id), -1)

            val title = intent.getStringExtra(context.getString(R.string.reminder_extra_title))
                ?: context.getString(R.string.reminder_default_title)

            val description =
                intent.getStringExtra(context.getString(R.string.reminder_extra_description))
                    ?: context.getString(R.string.reminder_default_description)

            showNotification(context, reminderId, title, description)
        }
    }

    private fun showNotification(
        context: Context,
        reminderId: Int,
        title: String,
        description: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        CustomNotificationManager.createReminderChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(context, CustomNotificationManager.getReminderChannelId())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(description))
                .build()

        notificationManager.notify(reminderId, notification)
    }

    companion object {
        fun scheduleReminder(
            context: Context,
            reminderId: Int,
            title: String,
            description: String,
            hour: Int,
            minute: Int,
            daysOfWeek: List<Int>
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            daysOfWeek.forEach { dayOfWeek ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.DAY_OF_WEEK, dayOfWeek + 1)

                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                val intent = Intent(context, ReminderNotificationService::class.java).apply {
                    action = context.getString(R.string.reminder_action)
                    putExtra(context.getString(R.string.reminder_extra_id), reminderId + dayOfWeek)
                    putExtra(context.getString(R.string.reminder_extra_title), title)
                    putExtra(context.getString(R.string.reminder_extra_description), description)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminderId + dayOfWeek,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY * 7,
                    pendingIntent
                )
            }
        }

        fun cancelReminder(context: Context, reminderId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            for (day in 0..6) {
                val intent = Intent(context, ReminderNotificationService::class.java).apply {
                    action = context.getString(R.string.reminder_action)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminderId + day,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}