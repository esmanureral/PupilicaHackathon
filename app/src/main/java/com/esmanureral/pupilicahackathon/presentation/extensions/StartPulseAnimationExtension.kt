package com.esmanureral.pupilicahackathon.presentation.extensions

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

fun View.startPulseAnimation() {
    stopPulseAnimation()

    val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.2f, 1f).apply {
        duration = 1000
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.RESTART
    }
    val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.2f, 1f).apply {
        duration = 1000
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.RESTART
    }

    AnimatorSet().apply {
        playTogether(scaleX, scaleY)
        start()
        this@startPulseAnimation.tag = this
    }
}