package com.esmanureral.pupilicahackathon.domain

import com.esmanureral.pupilicahackathon.R

object GameBadgeList {
    val badges: List<Badge> = listOf(
        Badge(
            resourceId = R.drawable.badges,
            name = "Cavity Crusader",
            minValue = Int.MIN_VALUE,
            maxValue = -50
        ),
        Badge(
            resourceId = R.drawable.badges,
            name = "Plaque Pirate",
            minValue = -49,
            maxValue = -1
        ),
        Badge(
            resourceId = R.drawable.badges,
            name = "Fearless Flosser",
            minValue = 0,
            maxValue = 4
        ),
        Badge(
            resourceId = R.drawable.badges,
            name = "Molar Master",
            minValue = 5,
            maxValue = 9
        ),
        Badge(
            resourceId = R.drawable.badges,
            name = "Enamel Emperor",
            minValue = 10,
            maxValue = 15
        )
    )

    fun badgeFor(score: Int): Badge? {
        return badges.firstOrNull { score in it.minValue..it.maxValue }
    }
}


