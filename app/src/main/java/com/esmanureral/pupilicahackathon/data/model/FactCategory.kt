package com.esmanureral.pupilicahackathon.data.model

import android.os.Parcelable
import com.esmanureral.pupilicahackathon.R
import kotlinx.parcelize.Parcelize

@Parcelize
enum class FactCategory(val stringResId: Int) : Parcelable {
    BRUSHING(R.string.category_brushing),
    CLEANING(R.string.category_cleaning),
    PROTECTION(R.string.category_protection),
    NUTRITION(R.string.category_nutrition),
    CHECKUP(R.string.category_checkup),
    ROUTINE(R.string.category_routine),
    MAINTENANCE(R.string.category_maintenance),
    HEALTH(R.string.category_health),
    SPECIAL_CONDITIONS(R.string.category_special_conditions),
    CHILD_CARE(R.string.category_child_care),
    STRESS(R.string.category_stress),
    HARMFUL_HABITS(R.string.category_harmful_habits),
    AESTHETICS(R.string.category_aesthetics)
}