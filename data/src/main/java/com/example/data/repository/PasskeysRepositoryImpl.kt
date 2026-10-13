package com.example.data.repository


import com.example.domain.model.AuthenticationOptionsJson
import com.example.domain.repository.PasskeysRepository
import com.example.domain.model.RegistrationOptionsJson
import com.example.network.api.PasskeysApi
import com.example.network.dto.webauth.BeginLoginBody
import com.example.network.dto.webauth.BeginRegistrationBody
import com.example.network.dto.webauth.serialization.toJson
import com.example.network.dto.webauth.serialization.toJsonBody
import com.example.network.dto.webauth.validation.sanitized
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PasskeysRepositoryImpl @Inject constructor(
    private val api: PasskeysApi
) : PasskeysRepository {

    override suspend fun getRegistrationOptions(userId: String): RegistrationOptionsJson {
        val parsed = api.beginRegistration(BeginRegistrationBody(userId))
        val json = parsed.sanitized().toJson()
        return RegistrationOptionsJson(json)
    }

    override suspend fun verifyRegistration(responseJson: String) {
        api.finishRegistration(responseJson.toJsonBody())
    }

    override suspend fun getAuthenticationOptions(usernameOrEmail: String): AuthenticationOptionsJson {
        val parsed = api.beginLogin(BeginLoginBody(usernameOrEmail))
        val json = parsed.sanitized().toJson()
        return AuthenticationOptionsJson(json)
    }

    override suspend fun verifyAuthentication(responseJson: String) {
        api.finishLogin(responseJson.toJsonBody())
    }
}
