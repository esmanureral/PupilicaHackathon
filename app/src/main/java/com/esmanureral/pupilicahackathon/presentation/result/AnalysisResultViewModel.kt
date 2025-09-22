package com.esmanureral.pupilicahackathon.presentation.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esmanureral.pupilicahackathon.domain.model.AnalysisResult
import com.esmanureral.pupilicahackathon.data.model.WeeklyPlanItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisResultViewModel : ViewModel() {

    private val _analysisResult = MutableLiveData<AnalysisResult>()
    val analysisResult: LiveData<AnalysisResult> get() = _analysisResult

    private val _imageUri = MutableLiveData<String?>()
    val imageUri: LiveData<String?> get() = _imageUri

    private val _loadedBitmap = MutableLiveData<Bitmap?>()
    val loadedBitmap: LiveData<Bitmap?> get() = _loadedBitmap

    private val _shareFileUri = MutableLiveData<Uri?>()
    val shareFileUri: LiveData<Uri?> get() = _shareFileUri

    fun initializeData(resultText: String, imageUri: String?) {
        _analysisResult.value = parseAnalysisResult(resultText)
        _imageUri.value = imageUri
    }

    private fun parseAnalysisResult(input: String): AnalysisResult {
        return try {
            when {
                input.trim().startsWith("{") && input.trim().endsWith("}") -> parseJsonResult(
                    JSONObject(input)
                )

                input.contains("'top_predictions'") || input.contains("\"top_predictions\"") -> parsePythonDictFormat(
                    input
                )

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
        return if (detailedResultsIndex != -1) dentalComment.substring(0, detailedResultsIndex)
            .trim() else dentalComment
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
            predictions = "",
            weeklyPlan = extractWeeklyPlan(lines),
            videoUrl = ""
        )
    }

    private fun extractWeeklyPlan(lines: List<String>): List<WeeklyPlanItem> {
        val days = listOf(
            "Pazartesi",
            "Salı",
            "Çarşamba",
            "Perşembe",
            "Cuma",
            "Cumartesi",
            "Pazar"
        )
        val weeklyPlan = mutableListOf<WeeklyPlanItem>()
        var currentDay = ""
        var currentTask = ""

        for (line in lines) {
            if (days.any { it == line }) {
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


    fun loadBitmapFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromUriInternal(context, uri)
                }
                _loadedBitmap.value = bitmap
            } catch (e: Exception) {
                android.util.Log.e("AnalysisResultViewModel", "Error loading bitmap", e)
                _loadedBitmap.value = null
            }
        }
    }

    private fun loadBitmapFromUriInternal(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            android.util.Log.e("AnalysisResultViewModel", "Error loading bitmap from URI", e)
            null
        }
    }

    fun prepareShareData(context: Context) {
        viewModelScope.launch {
            try {
                val imageUriString = _imageUri.value

                if (!imageUriString.isNullOrBlank()) {
                    val uri = Uri.parse(imageUriString)
                    val bitmap = withContext(Dispatchers.IO) {
                        loadBitmapFromUriInternal(context, uri)
                    }

                    bitmap?.let { bmp ->
                        val fileUri = withContext(Dispatchers.IO) {
                            saveBitmapToFile(context, bmp)
                        }
                        _shareFileUri.value = fileUri
                    }
                } else {
                    _shareFileUri.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("AnalysisResultViewModel", "Error preparing share data", e)
                _shareFileUri.value = null
            }
        }
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "dental_analysis_$timestamp.jpg"
            val file = File(context.cacheDir, fileName)

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            android.util.Log.e("AnalysisResultViewModel", "Error saving bitmap", e)
            null
        }
    }
}

class AnalysisResultViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisResultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalysisResultViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}