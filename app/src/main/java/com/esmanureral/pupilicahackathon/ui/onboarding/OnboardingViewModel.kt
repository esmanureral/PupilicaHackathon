package com.esmanureral.pupilicahackathon.ui.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.esmanureral.pupilicahackathon.data.local.OnboardingPreferences

class OnboardingViewModel(
    private val prefs: OnboardingPreferences
) : ViewModel() {

    private val _onboardingCompleted = MutableLiveData<Boolean>()
    val onboardingCompleted: LiveData<Boolean> = _onboardingCompleted

    fun completeOnboarding() {
        prefs.isOnboardingCompleted = true
        _onboardingCompleted.value = true
    }
}
