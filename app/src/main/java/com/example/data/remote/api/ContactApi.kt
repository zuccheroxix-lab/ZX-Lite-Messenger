package com.example.data.remote.api

import com.example.data.remote.model.ContactDiscoveryRequest
import com.example.data.remote.model.ContactDiscoveryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ContactApi {

    @POST("contacts/discover")
    suspend fun discoverContacts(
        @Body request: ContactDiscoveryRequest
    ): Response<ContactDiscoveryResponse>
}
