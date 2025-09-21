package com.esmanureral.pupilicahackathon.domain.model

import com.esmanureral.pupilicahackathon.data.model.WeeklyPlanItem

data class AnalysisResult(
    val summary: String,
    val predictions: String,
    val weeklyPlan: List<WeeklyPlanItem>,
    val videoUrl: String,
)