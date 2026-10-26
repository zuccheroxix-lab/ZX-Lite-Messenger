package com.example.data.remote.api

import com.example.data.remote.model.AuthResponse
import com.example.data.remote.model.LoginRequest
import com.example.data.remote.model.RefreshRequest
import com.example.data.remote.model.RegisterRequest
import com.example.data.remote.model.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
