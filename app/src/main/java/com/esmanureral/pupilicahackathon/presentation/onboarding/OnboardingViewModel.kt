package com.esmanureral.pupilicahackathon.presentation.onboarding

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.local.OnboardingPreferences
import com.esmanureral.pupilicahackathon.data.model.OnboardingPage

class OnboardingViewModel(
    private val prefs: OnboardingPreferences
) : ViewModel() {

    private val _onboardingCompleted = MutableLiveData<Boolean>()
    val onboardingCompleted: LiveData<Boolean> = _onboardingCompleted

    private val _currentPage = MutableLiveData(0)
    val currentPage: LiveData<Int> = _currentPage

    private val _pages = MutableLiveData<List<OnboardingPage>>()
    val pages: LiveData<List<OnboardingPage>> = _pages

    fun createPages(context: Context) {
        _pages.value = listOf(
            OnboardingPage(
                R.raw.tooth_analys,
                context.getString(R.string.onb_title_1),
                context.getString(R.string.onb_desc_1),
                context.getColor(R.color.light_yellow)
            ),
            OnboardingPage(
                R.raw.tooth_asistants,
                context.getString(R.string.onb_title_2),
                context.getString(R.string.onb_desc_2),
                context.getColor(R.color.light_blue)
            ),
            OnboardingPage(
                R.raw.tooth_brush,
                context.getString(R.string.onb_title_3),
                context.getString(R.string.onb_desc_3),
                context.getColor(R.color.onb_pink)
            ),
            OnboardingPage(
                R.raw.game,
                context.getString(R.string.onb_title_4),
                context.getString(R.string.onb_desc_4),
                context.getColor(R.color.light_blue)
            )
        )
    }

    fun completeOnboarding() {
        prefs.isOnboardingCompleted = true
        _onboardingCompleted.value = true
    }

    fun nextPage() {
        val pages = _pages.value ?: return
        val current = _currentPage.value ?: 0
        if (current < pages.lastIndex) {
            _currentPage.value = current + 1
        }
    }

    fun previousPage() {
        val current = _currentPage.value ?: 0
        if (current > 0) {
            _currentPage.value = current - 1
        }
    }

    fun isFirstPage(): Boolean {
        return (_currentPage.value ?: 0) == 0
    }

    fun isLastPage(): Boolean {
        val pages = _pages.value ?: return false
        return (_currentPage.value ?: 0) == pages.lastIndex
    }

    fun shouldShowBackButton(): Boolean {
        return !isFirstPage()
    }

    fun getNextButtonText(context: Context): String {
        return if (isLastPage()) {
            context.getString(R.string.get_started)
        } else {
            context.getString(R.string.next)
        }
    }

    fun setCurrentPage(position: Int) {
        _currentPage.value = position
    }
}
