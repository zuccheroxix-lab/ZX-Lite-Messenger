package com.example.data.remote.api

import com.example.data.remote.model.BlockUserPayload
import com.example.data.remote.model.BlockedUserDto
import com.example.data.remote.model.FcmTokenPayload
import com.example.data.remote.model.PrivacySettingsDto
import com.example.data.remote.model.UpdateProfileRequest
import com.example.data.remote.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface UserApi {

    @GET("users/search")
    suspend fun searchUsers(
        @Query("q") query: String
    ): Response<List<UserDto>>

    @GET("users/profile")
    suspend fun getMyProfile(): Response<UserDto>

    @PUT("users/profile")
    suspend fun updateMyProfile(
        @Body request: UpdateProfileRequest
    ): Response<Map<String, Any>>

    @GET("users/privacy")
    suspend fun getPrivacySettings(): Response<PrivacySettingsDto>

    @PUT("users/privacy")
    suspend fun updatePrivacySettings(
        @Body request: PrivacySettingsDto
    ): Response<PrivacySettingsDto>

    @POST("users/block")
    suspend fun blockUser(
        @Body request: BlockUserPayload
    ): Response<Map<String, Any>>

    @POST("users/unblock")
    suspend fun unblockUser(
        @Body request: BlockUserPayload
    ): Response<Map<String, Any>>

    @GET("users/blocked")
    suspend fun getBlockedUsers(): Response<List<BlockedUserDto>>

    @POST("users/fcm-token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenPayload
    ): Response<Map<String, Any>>

    @DELETE("users/fcm-token")
    suspend fun clearFcmToken(
        @Query("deviceId") deviceId: String? = null
    ): Response<Map<String, Any>>
}
