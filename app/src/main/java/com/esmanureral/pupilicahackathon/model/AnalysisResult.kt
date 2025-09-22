package com.esmanureral.pupilicahackathon.model

import com.esmanureral.pupilicahackathon.domain.model.WeeklyPlanItem

data class AnalysisResult(
    val summary: String,
    val predictions: String,
    val weeklyPlan: List<WeeklyPlanItem>,
    val videoUrl: String,
)