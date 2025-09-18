package com.esmanureral.pupilicahackathon.data.local

import android.content.Context
import android.content.SharedPreferences
import com.esmanureral.pupilicahackathon.R

class OnboardingPreferences(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.pref_onboarding_name),
        Context.MODE_PRIVATE
    )

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_onboarding_completed), false)
        set(value) {
            prefs.edit().putBoolean(
                context.getString(R.string.pref_onboarding_completed),
                value
            ).apply()
        }
}
