package com.esmanureral.pupilicahackathon.data.remote

import com.esmanureral.pupilicahackathon.data.model.QuizResponse
import retrofit2.http.GET

interface QuizApiService {
    @GET("esmanureral/JSON/main/jsonverisi.json")
    suspend fun fetchQuiz(): QuizResponse
}