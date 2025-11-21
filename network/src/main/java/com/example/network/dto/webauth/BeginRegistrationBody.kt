package com.example.network.dto.webauth

import kotlinx.serialization.Serializable

@Serializable
data class BeginRegistrationBody(
    val userId: String
)
