package com.example.data.remote.api

import com.example.data.remote.model.ApiResponse
import com.example.data.remote.model.FriendDto
import com.example.data.remote.model.IncomingFriendRequestDto
import com.example.data.remote.model.OutgoingFriendRequestDto
import com.example.data.remote.model.RequestIdPayload
import com.example.data.remote.model.SendFriendRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface FriendApi {

    @POST("friends/request")
    suspend fun sendFriendRequest(
        @Body request: SendFriendRequest
    ): Response<Map<String, Any>>

    @POST("friends/accept")
    suspend fun acceptFriendRequest(
        @Body request: RequestIdPayload
    ): Response<Map<String, Any>>

    @POST("friends/decline")
    suspend fun declineFriendRequest(
        @Body request: RequestIdPayload
    ): Response<Map<String, Any>>

    @POST("friends/cancel")
    suspend fun cancelFriendRequest(
        @Body request: RequestIdPayload
    ): Response<Map<String, Any>>

    @GET("friends")
    suspend fun getFriends(): Response<List<FriendDto>>

    @GET("friends/requests/incoming")
    suspend fun getIncomingRequests(): Response<List<IncomingFriendRequestDto>>

    @GET("friends/requests/outgoing")
    suspend fun getOutgoingRequests(): Response<List<OutgoingFriendRequestDto>>

    @DELETE("friends/{id}")
    suspend fun unfriend(
        @Path("id") friendUserId: String
    ): Response<Map<String, Any>>
}
