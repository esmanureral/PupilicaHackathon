package com.esmanureral.pupilicahackathon.data.model

import com.esmanureral.pupilicahackathon.R

enum class BadgeCategory(
    private val stringResId: Int
) {
    BEGINNER(R.string.badge_category_beginner),
    INTERMEDIATE(R.string.badge_category_intermediate),
    ADVANCED(R.string.badge_category_advanced),
    EXPERT(R.string.badge_category_expert);

    fun getDisplayName(context: android.content.Context): String {
        return context.getString(stringResId)
    }
}

fun String.toBadgeCategory(): BadgeCategory? {
    return try {
        BadgeCategory.valueOf(this.uppercase())
    } catch (e: IllegalArgumentException) {
        null
    }
}
