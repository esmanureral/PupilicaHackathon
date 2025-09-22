package com.esmanureral.pupilicahackathon.presentation.feature.badge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esmanureral.pupilicahackathon.domain.repository.QuizRepository

class GameBadgeViewModelFactory(
    private val repository: QuizRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameBadgeViewModel::class.java)) {
            return GameBadgeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}