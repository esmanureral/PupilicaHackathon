package com.esmanureral.pupilicahackathon.presentation.congratulation

import androidx.lifecycle.ViewModel

class BadgeCongratulationViewModel : ViewModel() {

    private var _isAnimationPlaying = true
    val isAnimationPlaying: Boolean get() = _isAnimationPlaying

    fun stopAnimation() {
        _isAnimationPlaying = false
    }

    fun resolveBadgeName(badgeNameArg: String): String {
        return try {
            val badgeNameResId = badgeNameArg.toInt()
            badgeNameArg
        } catch (e: NumberFormatException) {
            badgeNameArg
        }
    }

    fun shouldShowBadgeNameAsResource(badgeNameArg: String): Boolean {
        return try {
            badgeNameArg.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }
}