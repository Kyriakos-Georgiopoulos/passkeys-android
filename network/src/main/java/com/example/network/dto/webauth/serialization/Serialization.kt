package com.example.network.dto.webauth.serialization

import com.example.network.dto.webauth.CreationOptions
import com.example.network.dto.webauth.RequestOptions
import kotlinx.serialization.json.Json

internal val DefaultJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    explicitNulls = false
}

fun CreationOptions.toJson(json: Json = DefaultJson): String =
    json.encodeToString(CreationOptions.serializer(), this)

fun RequestOptions.toJson(json: Json = DefaultJson): String =
    json.encodeToString(RequestOptions.serializer(), this)