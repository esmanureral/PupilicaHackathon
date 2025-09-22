package com.esmanureral.pupilicahackathon.data.remote.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AnalyzeApiService {
    @Multipart
    @POST("analyze")
    suspend fun analyzeImage(
        @Part("user_id") userId: RequestBody,
        @Part("image_b64") imageBase64: RequestBody
    ): ResponseBody
}