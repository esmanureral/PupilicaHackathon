package com.esmanureral.pupilicahackathon

import android.content.Context
import android.content.res.Resources
import android.widget.Toast

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()