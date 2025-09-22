package com.esmanureral.pupilicahackathon.presentation.feature.reminder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esmanureral.pupilicahackathon.domain.repository.ReminderRepository
import com.esmanureral.pupilicahackathon.data.repository.ReminderRepositoryImpl
import com.esmanureral.pupilicahackathon.domain.usecase.NotificationUseCase

class ReminderViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(context)
    }

    private val notificationUseCase: NotificationUseCase by lazy {
        NotificationUseCase(context)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            return ReminderViewModel(reminderRepository, notificationUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}