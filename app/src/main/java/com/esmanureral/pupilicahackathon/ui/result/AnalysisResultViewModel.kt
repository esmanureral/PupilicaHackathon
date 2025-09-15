package com.esmanureral.pupilicahackathon.ui.result

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.esmanureral.pupilicahackathon.data.model.AnalysisResult
import com.esmanureral.pupilicahackathon.data.model.WeeklyPlanItem
import org.json.JSONObject

class AnalysisResultViewModel : ViewModel() {

    private val _analysisResult = MutableLiveData<AnalysisResult>()
    val analysisResult: LiveData<AnalysisResult> get() = _analysisResult

    private val _imageUri = MutableLiveData<String?>()
    val imageUri: LiveData<String?> get() = _imageUri

    fun initializeData(resultText: String, imageUri: String?) {
        _analysisResult.value = parseAnalysisResult(resultText)
        _imageUri.value = imageUri
    }

    private fun parseAnalysisResult(input: String): AnalysisResult {
        return try {
            if (input.trim().startsWith("{")) parseJsonResult(JSONObject(input))
            else parseTextResult(input)
        } catch (e: Exception) {
            defaultResult(input)
        }
    }

    private fun defaultResult(input: String) = AnalysisResult(
        summary = input,
        predictions = "",
        weeklyPlan = emptyList(),
        videoUrl = ""
    )

    private fun parseJsonResult(json: JSONObject): AnalysisResult {
        return AnalysisResult(
            summary = json.optString("dental_comment", ""),
            predictions = parsePredictionsFromJson(json),
            weeklyPlan = parseWeeklyPlanFromJson(json),
            videoUrl = json.optString("video_suggestion", "")
        )
    }

    private fun parsePredictionsFromJson(json: JSONObject): String {
        val array = json.optJSONArray("top_predictions") ?: return ""
        return List(array.length()) { i -> array.optString(i, "") }.joinToString("\n")
    }

    private fun parseWeeklyPlanFromJson(json: JSONObject): List<WeeklyPlanItem> {
        val array = json.optJSONArray("weekly_plan") ?: return emptyList()
        return List(array.length()) { i ->
            val item = array.optJSONObject(i)
            WeeklyPlanItem(
                day = item?.optString("day", "") ?: "",
                task = item?.optString("task", "") ?: ""
            )
        }
    }

    private fun parseTextResult(text: String): AnalysisResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        return AnalysisResult(
            summary = extractSummary(lines),
            predictions = extractPredictions(lines),
            weeklyPlan = extractWeeklyPlan(lines),
            videoUrl = extractVideoUrl(lines)
        )
    }

    private fun extractSummary(lines: List<String>): String {
        return lines.filter { it.contains("Çürük riskiniz") || it.contains("Düzenli bakımla") }
            .joinToString(" ")
    }

    private fun extractPredictions(lines: List<String>): String {
        return lines.filter { it.contains("Caries:") || it.contains("Hypodontia:") }
            .joinToString("\n")
    }

    private fun extractVideoUrl(lines: List<String>): String {
        return lines.firstOrNull { it.startsWith("https://") } ?: ""
    }

    private fun extractWeeklyPlan(lines: List<String>): List<WeeklyPlanItem> {
        val days = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
        val weeklyPlan = mutableListOf<WeeklyPlanItem>()

        var currentDay = ""
        var currentTask = ""

        for (line in lines) {
            when {
                days.any { line.contains(it) } -> {
                    if (currentDay.isNotEmpty() && currentTask.isNotEmpty()) {
                        weeklyPlan.add(WeeklyPlanItem(currentDay, currentTask.trim()))
                    }
                    currentDay = line
                    currentTask = ""
                }

                line.startsWith("https://") || line.contains("Caries:") || line.contains("Hypodontia:") ||
                        line.contains("Çürük riskiniz") || line.contains("Düzenli bakımla") -> {
                }

                else -> currentTask += "$line "
            }
        }

        if (currentDay.isNotEmpty() && currentTask.isNotEmpty()) {
            weeklyPlan.add(WeeklyPlanItem(currentDay, currentTask.trim()))
        }

        return weeklyPlan
    }
}