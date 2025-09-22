package com.esmanureral.pupilicahackathon.presentation.feature.badge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esmanureral.pupilicahackathon.domain.repository.QuizRepository
import com.esmanureral.pupilicahackathon.domain.model.Badge
import com.esmanureral.pupilicahackathon.domain.model.BadgeWithState
import kotlinx.coroutines.launch

data class BadgeUiState(
    val badgesWithState: List<BadgeWithState> = emptyList(),
    val currentScore: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

class GameBadgeViewModel(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(BadgeUiState())
    val uiState: LiveData<BadgeUiState> = _uiState

    private var badgeList: List<Badge> = emptyList()

    init {
        loadBadgeData()
    }

    private fun loadBadgeData() {
        updateUiState(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val score = repository.loadScore()
                badgeList = GameBadgeList.getBadges()
                val badgesWithState = badgeList.map { it.toBadgeWithState(score) }

                updateUiState(
                    badgesWithState = badgesWithState,
                    currentScore = score,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                updateUiState(
                    badgesWithState = emptyList(),
                    currentScore = 0,
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun updateUiState(
        badgesWithState: List<BadgeWithState>? = null,
        currentScore: Int? = null,
        isLoading: Boolean? = null,
        error: String? = null
    ) {
        val currentState = _uiState.value ?: BadgeUiState()
        _uiState.value = currentState.copy(
            badgesWithState = badgesWithState ?: currentState.badgesWithState,
            currentScore = currentScore ?: currentState.currentScore,
            isLoading = isLoading ?: currentState.isLoading,
            error = error ?: currentState.error
        )
    }
}

private fun Badge.toBadgeWithState(score: Int): BadgeWithState {
    return BadgeWithState(
        badge = this,
        isUnlocked = isUnlocked(score)
    )
}

private fun Badge.isUnlocked(score: Int): Boolean {
    return score in minValue..maxValue
}