package com.esmanureral.pupilicahackathon.domain

import com.esmanureral.pupilicahackathon.R

object GameBadgeList {
    val badges: List<Badge> = listOf(
        Badge(
            resourceId = R.drawable.badge1,
            name = "Süt Dişi",
            minValue = 1,
            maxValue = 9
        ),
        Badge(
            resourceId = R.drawable.badge2,
            name = "Köpük Şövalyesi",
            minValue = 10,
            maxValue = 24
        ),
        Badge(
            resourceId = R.drawable.badge3,
            name = "Şeker Avcısı",
            minValue = 25,
            maxValue = 49
        ),
        Badge(
            resourceId = R.drawable.badge4,
            name = "Plak Koruyucusu",
            minValue = 50,
            maxValue = 74
        ),
        Badge(
            resourceId = R.drawable.badge5,
            name = "Diş Perisi Elçisi",
            minValue = 75,
            maxValue = 99
        ),
        Badge(
            resourceId = R.drawable.badge6,
            name = "Işıltılı Gülüş",
            minValue = 100,
            maxValue = 100
        )
    )

    fun badgeFor(score: Int): Badge? {
        return badges.firstOrNull { score in it.minValue..it.maxValue }
    }
}


