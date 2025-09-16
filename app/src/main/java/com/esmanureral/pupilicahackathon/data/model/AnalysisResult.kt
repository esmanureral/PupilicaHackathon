package com.esmanureral.pupilicahackathon.data.model

data class AnalysisResult(
    val summary: String,
    val predictions: String,
    val weeklyPlan: List<WeeklyPlanItem>,
    val videoUrl: String,
)