package com.esmanureral.pupilicahackathon.domain.model

import com.google.gson.annotations.SerializedName

data class WeeklyPlanItem(
    @SerializedName("day") val day: String,
    @SerializedName("task") val task: String
)