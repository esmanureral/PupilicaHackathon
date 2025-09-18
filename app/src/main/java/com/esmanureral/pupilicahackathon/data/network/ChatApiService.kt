package com.esmanureral.pupilicahackathon.data.network

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

data class ChatResponse(
    val response: String? = null,
    val session_id: String? = null,
    val message: String? = null,
    val answer: String? = null,
    val reply: String? = null
)

interface ChatApiService {
    @FormUrlEncoded
    @POST("chat")
    suspend fun sendMessage(
        @Field("message") message: String,
        @Field("session_id") sessionId: String
    ): Response<ChatResponse>

}
