package com.example.data.remote

import com.example.core.preferences.UserPreferencesManager
import com.example.core.security.SecureSessionManager
import com.example.data.remote.api.AuthApi
import com.example.data.remote.api.ChatApi
import com.example.data.remote.api.ContactApi
import com.example.data.remote.api.FriendApi
import com.example.data.remote.api.UpdateApi
import com.example.data.remote.api.UserApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class NetworkClient(
    private val sessionManager: SecureSessionManager,
    private val preferencesManager: UserPreferencesManager
) {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = sessionManager.getAccessToken()
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
        } else {
            original.newBuilder()
                .header("Accept", "application/json")
                .build()
        }
        val response = chain.proceed(request)
        if (response.code == 401 && !original.url.encodedPath.contains("/auth/")) {
            // Token expired or invalid
        }
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Redact authorization tokens to avoid logging sensitive data
        redactHeader("Authorization")
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private fun getBaseUrlWithTrailingSlash(): String {
        var base = preferencesManager.getServerUrl().trim()
        if (!base.endsWith("/")) {
            base += "/"
        }
        return base
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(getBaseUrlWithTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val authApi: AuthApi get() = getRetrofit().create(AuthApi::class.java)
    val userApi: UserApi get() = getRetrofit().create(UserApi::class.java)
    val chatApi: ChatApi get() = getRetrofit().create(ChatApi::class.java)
    val friendApi: FriendApi get() = getRetrofit().create(FriendApi::class.java)
    val contactApi: ContactApi get() = getRetrofit().create(ContactApi::class.java)
    val updateApi: UpdateApi get() = getRetrofit().create(UpdateApi::class.java)
}
