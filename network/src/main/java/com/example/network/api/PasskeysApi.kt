package com.example.network.api

import com.example.network.dto.webauth.BeginLoginBody
import com.example.network.dto.webauth.BeginRegistrationBody
import com.example.network.dto.webauth.CreationOptions
import com.example.network.dto.webauth.RequestOptions
import retrofit2.http.Body
import retrofit2.http.POST

interface PasskeysApi {
    @POST("webauthn/registration/options")
    suspend fun beginRegistration(@Body body: BeginRegistrationBody): CreationOptions

    @POST("webauthn/authentication/options")
    suspend fun beginLogin(@Body body: BeginLoginBody): RequestOptions
}
