package com.esmanureral.pupilicahackathon.data.network

import retrofit2.http.GET

interface QuizApiService {
    @GET("esmanureral/JSON/refs/heads/main/jsonverisi.json")
    suspend fun fetchQuiz(): QuizResponse
}