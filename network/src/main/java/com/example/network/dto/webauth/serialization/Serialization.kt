package com.example.network.dto.webauth.serialization

import com.example.network.dto.webauth.CreationOptions
import com.example.network.dto.webauth.RequestOptions
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

internal val DefaultJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    explicitNulls = false
}

fun CreationOptions.toJson(json: Json = DefaultJson): String =
    json.encodeToString(CreationOptions.serializer(), this)

fun RequestOptions.toJson(json: Json = DefaultJson): String =
    json.encodeToString(RequestOptions.serializer(), this)

/** Wraps a JSON string as a request body. The string is sent as it is, without being parsed. */
fun String.toJsonBody(): RequestBody =
    toRequestBody("application/json".toMediaType())