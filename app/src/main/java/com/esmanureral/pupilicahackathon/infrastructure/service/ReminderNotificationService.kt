package com.esmanureral.pupilicahackathon.infrastructure.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.util.Log
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.presentation.main.MainActivity
import com.esmanureral.pupilicahackathon.infrastructure.notification.NotificationHelper as CustomNotificationManager
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

            val hour = intent.getIntExtra(EXTRA_HOUR, -1)
            val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
            val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
            if (hour in 0..23 && minute in 0..59 && dayOfWeek in 0..6) {
                scheduleSingleExact(
                    context,
                    reminderId,
                    title,
                    description,
                    hour,
                    minute,
                    dayOfWeek
                )
            }
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
                .setSmallIcon(R.drawable.notifications)
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
        private const val EXTRA_HOUR = "extra_hour"
        private const val EXTRA_MINUTE = "extra_minute"
        private const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
        fun scheduleReminder(
            context: Context,
            reminderId: Int,
            title: String,
            description: String,
            hour: Int,
            minute: Int,
            daysOfWeek: List<Int>
        ) {
            daysOfWeek.forEach { dayOfWeek ->
                scheduleSingleExact(
                    context,
                    reminderId + dayOfWeek,
                    title,
                    description,
                    hour,
                    minute,
                    dayOfWeek
                )
            }
        }

        private fun scheduleSingleExact(
            context: Context,
            requestCode: Int,
            title: String,
            description: String,
            hour: Int,
            minute: Int,
            dayOfWeek: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val triggerAt = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, dayOfWeek + 1)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }.timeInMillis

            val intent = Intent(context, ReminderNotificationService::class.java).apply {
                action = context.getString(R.string.reminder_action)
                putExtra(context.getString(R.string.reminder_extra_id), requestCode)
                putExtra(context.getString(R.string.reminder_extra_title), title)
                putExtra(context.getString(R.string.reminder_extra_description), description)
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
                putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val canExact =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else true

            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                val showIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
                Log.w(
                    "ReminderService",
                    "Exact izni yok. setAlarmClock ile planlandı rc=$requestCode"
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