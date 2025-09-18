package com.esmanureral.pupilicahackathon

import android.animation.AnimatorSet
import android.view.View

fun View.stopPulseAnimation() {
    (tag as? AnimatorSet)?.cancel()
    scaleX = 1f
    scaleY = 1f
}