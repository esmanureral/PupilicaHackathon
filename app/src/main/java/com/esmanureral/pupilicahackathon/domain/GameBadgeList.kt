package com.esmanureral.pupilicahackathon.domain

import android.content.Context
import com.esmanureral.pupilicahackathon.R

object GameBadgeList {
    fun getBadges(context: Context): List<Badge> = listOf(
        Badge(
            resourceId = R.drawable.badge1,
            name = context.getString(R.string.badge_milk_tooth),
            minValue = 1,
            maxValue = 9
        ),
        Badge(
            resourceId = R.drawable.badge2,
            name = context.getString(R.string.badge_foam_knight),
            minValue = 10,
            maxValue = 24
        ),
        Badge(
            resourceId = R.drawable.badge3,
            name = context.getString(R.string.badge_sugar_hunter),
            minValue = 25,
            maxValue = 49
        ),
        Badge(
            resourceId = R.drawable.badge4,
            name = context.getString(R.string.badge_plaque_protector),
            minValue = 50,
            maxValue = 74
        ),
        Badge(
            resourceId = R.drawable.badge5,
            name = context.getString(R.string.badge_tooth_fairy_ambassador),
            minValue = 75,
            maxValue = 99
        ),
        Badge(
            resourceId = R.drawable.badge6,
            name = context.getString(R.string.badge_sparkling_smile),
            minValue = 100,
            maxValue = 100
        )
    )

    fun badgeFor(context: Context, score: Int): Badge? {
        return getBadges(context).firstOrNull { score in it.minValue..it.maxValue }
    }
}


