package com.esmanureral.pupilicahackathon.ui.reminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.local.ReminderSharedPreferences
import com.esmanureral.pupilicahackathon.data.model.ReminderModel
import com.esmanureral.pupilicahackathon.service.ReminderNotificationService

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val reminderPrefs = ReminderSharedPreferences(application)
    private val notificationManager =
        application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _reminderSaved = MutableLiveData<Boolean>()
    val reminderSaved: LiveData<Boolean> = _reminderSaved

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _activeReminders = MutableLiveData<List<ReminderModel>>()
    val activeReminders: LiveData<List<ReminderModel>> = _activeReminders

    private val context = getApplication<Application>()

    init {
        createNotificationChannel()
        loadActiveReminders()
    }

    private fun createNotificationChannel() {
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

    fun saveReminder(reminder: ReminderModel) {
        val context = getApplication<Application>()
        try {
            val currentReminders = reminderPrefs.loadActiveReminders().toMutableList()
            currentReminders.add(reminder)
            reminderPrefs.saveActiveReminders(currentReminders)

            ReminderNotificationService.scheduleReminder(
                context = context,
                reminderId = reminder.id,
                title = reminder.title,
                description = reminder.description,
                hour = reminder.hour,
                minute = reminder.minute,
                daysOfWeek = reminder.daysOfWeek
            )

            _reminderSaved.value = true
            _activeReminders.value = currentReminders

        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = context.getString(R.string.error_save_reminder, e.message ?: "")
        }
    }

    fun loadActiveReminders() {
        try {
            _activeReminders.value = reminderPrefs.loadActiveReminders()
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = context.getString(R.string.error_load_reminder, e.message ?: "")
        }
    }

    fun clearAllReminders() {
        try {
            val existing = reminderPrefs.loadActiveReminders()
            existing.forEach { reminder ->
                ReminderNotificationService.cancelReminder(context, reminder.id)
            }
            reminderPrefs.clearAllReminders()
            notificationManager.cancelAll()
            _activeReminders.value = emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = context.getString(R.string.error_clear_all_reminder, e.message ?: "")
        }
    }

    companion object {
        private const val CHANNEL_ID = "dental_reminder_channel"
    }
}
