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
            when {
                input.trim().startsWith("{") && input.trim().endsWith("}") -> parseJsonResult(JSONObject(input))
                input.contains("'top_predictions'") || input.contains("\"top_predictions\"") -> parsePythonDictFormat(input)
                else -> parseTextResult(input)
            }
        } catch (e: Exception) {
            android.util.Log.e("AnalysisResultViewModel", "Error in parseAnalysisResult", e)
            defaultResult(input)
        }
    }

    private fun defaultResult(input: String) = AnalysisResult(
        summary = input,
        predictions = "",
        weeklyPlan = emptyList(),
        videoUrl = ""
    )

    private fun parsePythonDictFormat(input: String): AnalysisResult {
        val jsonString = input
            .replace("'", "\"")
            .replace("True", "true")
            .replace("False", "false")
            .replace("None", "null")
        return try {
            parseJsonResult(JSONObject(jsonString))
        } catch (e: Exception) {
            android.util.Log.e("AnalysisResultViewModel", "Error parsing Python dict", e)
            parseTextResult(input)
        }
    }

    private fun parseJsonResult(json: JSONObject): AnalysisResult {
        val dentalComment = cleanDentalComment(json.optString("dental_comment", ""))
        val predictions = parsePredictionsFromJson(json)
        val weeklyPlan = parseWeeklyPlanFromJson(json)
        val videoUrl = json.optString("video_suggestion", "")

        return AnalysisResult(dentalComment, predictions, weeklyPlan, videoUrl)
    }

    private fun cleanDentalComment(dentalComment: String): String {
        val detailedResultsIndex = dentalComment.indexOf("Detaylı Sonuçlar:")
        return if (detailedResultsIndex != -1) dentalComment.substring(0, detailedResultsIndex).trim() else dentalComment
    }

    private fun parsePredictionsFromJson(json: JSONObject): String {
        return try {
            json.optJSONArray("top_predictions")?.let { array ->
                val predictions = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val prediction = array.optString(i, "")
                    if (prediction.contains("%")) predictions.add(prediction)
                }
                predictions.joinToString("\n") { "• $it" }
            } ?: ""
        } catch (e: Exception) {
            android.util.Log.e("AnalysisResultViewModel", "Error parsing predictions", e)
            ""
        }
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
            summary = lines.joinToString(" "),
            predictions = "", // text formatında tahminleri atlayabiliriz
            weeklyPlan = extractWeeklyPlan(lines),
            videoUrl = "" // text içinde video linkleri yoksa boş bırak
        )
    }

    private fun extractWeeklyPlan(lines: List<String>): List<WeeklyPlanItem> {
        val days = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
        val weeklyPlan = mutableListOf<WeeklyPlanItem>()
        var currentDay = ""
        var currentTask = ""

        for (line in lines) {
            if (days.any { it == line }) { // sadece gün satırı
                if (currentDay.isNotEmpty()) {
                    weeklyPlan.add(WeeklyPlanItem(currentDay, currentTask.trim()))
                }
                currentDay = line
                currentTask = ""
            } else {
                currentTask += "$line "
            }
        }

        if (currentDay.isNotEmpty()) {
            weeklyPlan.add(WeeklyPlanItem(currentDay, currentTask.trim()))
        }

        return weeklyPlan
    }
}
