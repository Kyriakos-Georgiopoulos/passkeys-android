/*
 * Copyright 2025 Kyriakos Georgiopoulos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.auth_credentials

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class PasskeysManagerImpl @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context
) : PasskeysManager {

    /**
     * Implementation of [PasskeysManager] to handle Passkey registration and sign-in.
     *
     * ### What changed in Android 14 (API 34)?
     * - **Prior to Android 14 (API 28 - 33):** Passkey support is provided exclusively through
     * Google Play Services. This means Google Password Manager is the only provider that
     * can store and sync passkeys natively on these devices.
     *
     * - **Android 14 and above:** Android introduced the `android.credentials` system framework API.
     * This opens up the ecosystem, allowing users to select compatible **third-party password
     * managers** (like 1Password, Bitwarden, Dashlane, etc.) as their default passkey provider
     * alongside or instead of Google Password Manager.
     */
    private val isAvailable by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // Passkeys strictly require Android 9 (API 28) or higher.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            false
        } else {
            // Available if Android 14+ (native framework support)
            // OR if Play Services is installed (Android 9-13 backport)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || isPlayServicesAvailable(applicationContext)
        }
    }

    @SuppressLint("PublicKeyCredential")
    override suspend fun register(
        activity: Activity,
        requestJson: String
    ): Result<PublicKeyResponseJson> {
        if (!isAvailable) {
            return Result.failure(PasskeyException(PasskeyError.Unsupported))
        }

        return runCatching {
            val credentialManager = CredentialManager.create(activity)
            val request = CreatePublicKeyCredentialRequest(requestJson)
            val response = credentialManager.createCredential(activity, request)
            val created = response as? CreatePublicKeyCredentialResponse
                ?: error("Unexpected response type: ${response::class.qualifiedName}")
            PublicKeyResponseJson(created.registrationResponseJson)
        }.mapToDomainError(::toPasskeyException)
    }

    override suspend fun signIn(
        activity: Activity,
        requestJson: String
    ): Result<PublicKeyResponseJson> {
        if (!isAvailable) {
            return Result.failure(PasskeyException(PasskeyError.Unsupported))
        }
        return runCatching {
            val credentialManager = CredentialManager.create(activity)
            val option = GetPublicKeyCredentialOption(requestJson)
            val response =
                credentialManager.getCredential(activity, GetCredentialRequest(listOf(option)))
            val credential = response.credential as? PublicKeyCredential
                ?: error("Unexpected credential type: ${response.credential::class.qualifiedName}")
            PublicKeyResponseJson(credential.authenticationResponseJson)
        }.mapToDomainError(::toPasskeyException)
    }
}
