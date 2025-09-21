package com.esmanureral.pupilicahackathon.data.remote

import com.esmanureral.pupilicahackathon.data.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ChatApiService {
    @FormUrlEncoded
    @POST("chat")
    suspend fun sendMessage(
        @Field("message") message: String,
        @Field("session_id") sessionId: String
    ): Response<ChatResponse>

}
