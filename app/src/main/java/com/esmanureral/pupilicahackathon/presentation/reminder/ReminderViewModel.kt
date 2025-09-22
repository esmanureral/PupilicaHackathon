package com.esmanureral.pupilicahackathon.presentation.reminder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esmanureral.pupilicahackathon.data.model.ReminderModel
import com.esmanureral.pupilicahackathon.data.repository.ReminderRepository
import com.esmanureral.pupilicahackathon.domain.usecase.NotificationUseCase
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val reminderRepository: ReminderRepository,
    private val notificationUseCase: NotificationUseCase
) : ViewModel() {

    private val _reminderSaved = MutableLiveData<Boolean>()
    val reminderSaved: LiveData<Boolean> = _reminderSaved

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _activeReminders = MutableLiveData<List<ReminderModel>>()
    val activeReminders: LiveData<List<ReminderModel>> = _activeReminders

    init {
        notificationUseCase.createNotificationChannel()
        loadActiveReminders()
    }

    fun saveReminder(reminder: ReminderModel) {
        viewModelScope.launch {
            reminderRepository.saveReminder(reminder)
                .onSuccess {
                    notificationUseCase.scheduleReminder(reminder)
                    _reminderSaved.value = true
                    loadActiveReminders()
                }
                .onFailure { exception ->
                    _errorMessage.value = "Error saving reminder: ${exception.message}"
                }
        }
    }

    fun loadActiveReminders() {
        viewModelScope.launch {
            reminderRepository.loadActiveReminders()
                .onSuccess { reminders ->
                    _activeReminders.value = reminders
                }
                .onFailure { exception ->
                    _errorMessage.value = "Error loading reminders: ${exception.message}"
                }
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            reminderRepository.loadActiveReminders()
                .onSuccess { existingReminders ->
                    existingReminders.forEach { reminder ->
                        notificationUseCase.cancelReminder(reminder.id)
                    }
                    reminderRepository.clearAllReminders()
                        .onSuccess {
                            notificationUseCase.cancelAllNotifications()
                            _activeReminders.value = emptyList()
                        }
                        .onFailure { exception ->
                            _errorMessage.value = "Error clearing reminders: ${exception.message}"
                        }
                }
                .onFailure { exception ->
                    _errorMessage.value =
                        "Error loading reminders for clearing: ${exception.message}"
                }
        }
    }
}