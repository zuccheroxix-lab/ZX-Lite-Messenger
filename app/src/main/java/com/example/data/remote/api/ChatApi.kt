package com.example.data.remote.api

import com.example.data.remote.model.ConversationDto
import com.example.data.remote.model.CreateDirectConversationRequest
import com.example.data.remote.model.MessageDto
import com.example.data.remote.model.MessagesResponse
import com.example.data.remote.model.SendMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface ChatApi {

    @GET("conversations")
    suspend fun getConversations(): Response<List<ConversationDto>>

    @POST("conversations/direct")
    suspend fun createDirectConversation(
        @Body request: CreateDirectConversationRequest
    ): Response<ConversationDto>

    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<MessagesResponse>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body request: SendMessageRequest
    ): Response<MessageDto>

    @POST("conversations/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: String
    ): Response<Unit>
}
