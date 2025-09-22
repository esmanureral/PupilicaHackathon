package com.esmanureral.pupilicahackathon.presentation.feature.badge

import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.domain.model.Badge
import com.esmanureral.pupilicahackathon.domain.model.BadgeCategory
import com.esmanureral.pupilicahackathon.domain.model.BadgeConfig
import com.esmanureral.pupilicahackathon.domain.model.toBadge

object GameBadgeList {

    fun getBadges(): List<Badge> {
        return getBadgeConfigs().map { it.toBadge() }
    }

    private fun getBadgeConfigs(): List<BadgeConfig> = listOf(
        BadgeConfig(
            id = "badge_milk_tooth",
            resourceId = R.drawable.badge1,
            nameResId = R.string.badge_milk_tooth,
            minValue = 1,
            maxValue = 9,
            category = BadgeCategory.BEGINNER
        ),
        BadgeConfig(
            id = "badge_foam_knight",
            resourceId = R.drawable.badge2,
            nameResId = R.string.badge_foam_knight,
            minValue = 10,
            maxValue = 24,
            category = BadgeCategory.INTERMEDIATE
        ),
        BadgeConfig(
            id = "badge_sugar_hunter",
            resourceId = R.drawable.badge3,
            nameResId = R.string.badge_sugar_hunter,
            minValue = 25,
            maxValue = 49,
            category = BadgeCategory.INTERMEDIATE
        ),
        BadgeConfig(
            id = "badge_plaque_protector",
            resourceId = R.drawable.badge4,
            nameResId = R.string.badge_plaque_protector,
            minValue = 50,
            maxValue = 74,
            category = BadgeCategory.ADVANCED
        ),
        BadgeConfig(
            id = "badge_tooth_fairy_ambassador",
            resourceId = R.drawable.badge5,
            nameResId = R.string.badge_tooth_fairy_ambassador,
            minValue = 75,
            maxValue = 99,
            category = BadgeCategory.ADVANCED
        ),
        BadgeConfig(
            id = "badge_sparkling_smile",
            resourceId = R.drawable.badge6,
            nameResId = R.string.badge_sparkling_smile,
            minValue = 100,
            maxValue = 100,
            category = BadgeCategory.EXPERT
        )
    )
}