package com.esmanureral.pupilicahackathon.data.model

import com.google.gson.annotations.SerializedName

data class WeeklyPlanItem(
    @SerializedName("day") val day: String,
    @SerializedName("task") val task: String
)