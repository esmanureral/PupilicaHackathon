package com.esmanureral.pupilicahackathon.ui.home

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.*
import com.esmanureral.pupilicahackathon.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class HomeViewModel : ViewModel() {

    companion object {
        private const val MAX_IMAGE_SIZE = 800
        private const val IMAGE_QUALITY = 80
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_BASE = 2000L
    }

    private val _capturedImage = MutableLiveData<Bitmap?>()
    val capturedImage: LiveData<Bitmap?> = _capturedImage

    private val _analyzeResult = MutableLiveData<String?>()
    val analyzeResult: MutableLiveData<String?> = _analyzeResult

    private val _rawJsonResult = MutableLiveData<String>()
    val rawJsonResult: LiveData<String> = _rawJsonResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    var lastImageUri: String? = null

    fun setCapturedImage(bitmap: Bitmap) {
        _capturedImage.value = bitmap
    }

    fun processImage(bitmap: Bitmap) {
        _isLoading.value = true
        _analyzeResult.value = null

        viewModelScope.launch {
            try {
                val result = uploadBitmapForAnalysis(bitmap)
                _analyzeResult.value = result
                _rawJsonResult.value = extractRawJson(result)
            } catch (e: Exception) {
                _analyzeResult.value = "Hata: ${e.message}"
                _rawJsonResult.value = ""
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadBitmapForAnalysis(bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            var lastException: Exception? = null

            for (attempt in 1..MAX_RETRIES) {
                try {
                    val resized = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
                    val imageB64 = bitmapToBase64(resized)
                    val service = ApiClient.provideAnalyzeApi()
                    val response: ResponseBody = service.analyzeImage(
                        userIdPart("demo_user"),
                        imagePart(imageB64)
                    )
                    val body = response.string()
                    return@withContext if (body.isNullOrBlank()) "Sunucudan boş yanıt geldi" else formatJsonToText(
                        body
                    )
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < MAX_RETRIES) delay(attempt * RETRY_DELAY_BASE)
                }
            }
            "API Hatası: ${lastException?.message ?: "Bilinmeyen hata"}"
        }

    private fun userIdPart(userId: String): RequestBody =
        RequestBody.create("text/plain".toMediaType(), userId)

    private fun imagePart(imageB64: String): RequestBody =
        RequestBody.create("text/plain".toMediaType(), imageB64)

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun resizeBitmap(source: Bitmap, maxSize: Int): Bitmap {
        val (width, height) = source.width to source.height
        if (width <= maxSize && height <= maxSize) return source

        val aspect = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (aspect >= 1f) {
            newWidth = maxSize
            newHeight = (maxSize / aspect).toInt()
        } else {
            newWidth = (maxSize * aspect).toInt()
            newHeight = maxSize
        }
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun formatJsonToText(json: String): String {
        return try {
            val excludedKeys = setOf("user_id", "image_b64")
            val values = mutableListOf<String>()

            if (json.trim().startsWith("[")) {
                JSONArray(json).forEach { collectValues(it, excludedKeys, values) }
            } else {
                collectValues(JSONObject(json), excludedKeys, values)
            }

            values.ifEmpty { listOf(json) }.joinToString("\n")
        } catch (_: Exception) {
            json
        }
    }

    private fun collectValues(any: Any?, excludedKeys: Set<String>, out: MutableList<String>) {
        when (any) {
            null -> return
            is JSONObject -> any.keys().forEach { key ->
                if (key !in excludedKeys) collectValues(any.get(key), excludedKeys, out)
            }

            is JSONArray -> (0 until any.length()).forEach { i ->
                collectValues(
                    any.get(i),
                    excludedKeys,
                    out
                )
            }

            else -> any.toString().takeIf { it.isNotBlank() }?.let { out.add(it) }
        }
    }

    private fun extractRawJson(result: String): String {
        return try {
            JSONObject(result).toString()
        } catch (_: Exception) {
            result
        }
    }
}

// Extension function for JSONArray iteration
private inline fun JSONArray.forEach(action: (Any?) -> Unit) {
    for (i in 0 until length()) action(get(i))
}
