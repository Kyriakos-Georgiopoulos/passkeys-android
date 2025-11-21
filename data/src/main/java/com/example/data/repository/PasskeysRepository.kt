package com.example.data.repository

import com.example.domain.model.AuthenticationOptionsJson
import com.example.domain.model.RegistrationOptionsJson

/**
 * Domain contract. Implemented in :data.
 * Domain uses primitives only (no DTOs) for inputs.
 */
interface PasskeysRepository {
    suspend fun getRegistrationOptions(userId: String): RegistrationOptionsJson
    suspend fun getAuthenticationOptions(usernameOrEmail: String): AuthenticationOptionsJson
}