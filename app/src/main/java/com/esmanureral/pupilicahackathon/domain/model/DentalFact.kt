package com.esmanureral.pupilicahackathon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DentalFact(
    val id: Int,
    val title: String,
    val description: String,
    val iconResId: Int,
    val category: FactCategory
) : Parcelable
