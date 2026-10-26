package com.example.data.remote.api

import com.example.data.remote.model.CreateStatusRequest
import com.example.data.remote.model.UserStatusDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UpdateApi {

    @GET("updates")
    suspend fun getUpdates(): Response<List<UserStatusDto>>

    @POST("updates")
    suspend fun postUpdate(
        @Body request: CreateStatusRequest
    ): Response<UserStatusDto>
}
