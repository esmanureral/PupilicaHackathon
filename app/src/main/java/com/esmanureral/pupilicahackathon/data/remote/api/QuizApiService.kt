package com.esmanureral.pupilicahackathon.data.remote.api

import com.esmanureral.pupilicahackathon.domain.model.QuizResponse
import retrofit2.http.GET

interface QuizApiService {
    @GET("esmanureral/JSON/main/jsonverisi.json")
    suspend fun fetchQuiz(): QuizResponse
}