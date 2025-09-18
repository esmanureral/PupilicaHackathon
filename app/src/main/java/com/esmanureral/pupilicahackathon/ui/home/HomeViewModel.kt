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
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
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
    val analyzeResult: LiveData<String?> = _analyzeResult

    private val _rawJsonResult = MutableLiveData<String>()
    val rawJsonResult: LiveData<String> = _rawJsonResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    var lastImageUri: String? = null

    fun setCapturedImage(bitmap: Bitmap) {
        _capturedImage.value = bitmap
    }

    fun processImage(bitmap: Bitmap) {
        _isLoading.value = true
        _analyzeResult.value = null

        viewModelScope.launch {
            _analyzeResult.value = try {
                uploadBitmapForAnalysis(bitmap)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "API hatası: ${e.message}", e)
                "Hata: ${e.message}"
            }.also { _rawJsonResult.value = it }

            _isLoading.value = false
        }
    }

    private suspend fun uploadBitmapForAnalysis(bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            var lastException: Exception? = null

            repeat(MAX_RETRIES) { attempt ->
                try {
                    val resized = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
                    val imageB64 = bitmapToBase64(resized)
                    val service = ApiClient.provideAnalyzeApi()

                    val response: ResponseBody = service.analyzeImage(
                        "demo_user".toRequestBody("text/plain".toMediaType()),
                        imageB64.toRequestBody("text/plain".toMediaType())
                    )

                    return@withContext response.string().ifBlank { "Sunucudan boş yanıt geldi" }

                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.e(
                        "HomeViewModel",
                        "Deneme ${attempt + 1} hatası: ${e.message}",
                        e
                    )
                    if (attempt < MAX_RETRIES - 1) delay((attempt + 1) * RETRY_DELAY_BASE)
                }
            }

            val errorMsg = "API Hatası: ${lastException?.message ?: "Bilinmeyen hata"}"
            android.util.Log.e("HomeViewModel", "Tüm denemeler başarısız: $errorMsg")
            return@withContext errorMsg
        }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        }
    }

    private fun resizeBitmap(source: Bitmap, maxSize: Int): Bitmap {
        val (width, height) = source.width to source.height
        if (width <= maxSize && height <= maxSize) return source

        val aspect = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (aspect >= 1f) {
            maxSize to (maxSize / aspect).toInt()
        } else {
            (maxSize * aspect).toInt() to maxSize
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    // JSONArray için Kotlin-style extension
    private inline fun JSONArray.forEach(action: (Any?) -> Unit) {
        for (i in 0 until length()) action(get(i))
    }
}
