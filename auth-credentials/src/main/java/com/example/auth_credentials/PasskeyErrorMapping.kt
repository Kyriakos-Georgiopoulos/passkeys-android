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

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException

/**
 * Converts a [Result] that may contain any throwable into a [Result] that contains a
 * domain-specific [PasskeyException] on failure.
 *
 * Why:
 * - Keeps call sites concise: business logic can stay in terms of Result<T>.
 * - Centralizes translation from platform/library exceptions to your domain.
 *
 * Behavior:
 * - Success values are passed through unchanged.
 * - Failures are transformed using the provided [map] function.
 */
internal inline fun <T> Result<T>.mapToDomainError(
    crossinline map: (Throwable) -> PasskeyException
): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { t -> Result.failure(map(t)) }
)

/**
 * Maps underlying Credential Manager / WebAuthn exceptions to a stable, app-level
 * [PasskeyException] with a [PasskeyError] reason suitable for UI and analytics.
 *
 * Ordering matters:
 * - Specific DOM/WebAuthn exceptions are handled first to preserve their detailed cause.
 * - Then user-initiated cancellations.
 * - Then absence/misconfiguration conditions.
 * - Finally, generic Create/Get failures.
 * - As a last resort, if the capability is not available on this device/runtime,
 *   report [PasskeyError.Unsupported]; otherwise surface a generic operation failure.
 *
 * @param throwable The original exception thrown by the credential flow.
 */
internal fun toPasskeyException(
    throwable: Throwable,
): PasskeyException = PasskeyException(
    when (throwable) {
        // WebAuthn/DOM-level failures (e.g., RP ID mismatch, client data issues, user verification):
        // Keep these FIRST so they are not swallowed by broader Create/Get branches.
        is CreatePublicKeyCredentialDomException,
        is GetPublicKeyCredentialDomException -> PasskeyError.OperationFailed(throwable)

        // The user explicitly dismissed or backed out of the system UI.
        is CreateCredentialCancellationException,
        is GetCredentialCancellationException -> PasskeyError.CanceledByUser

        // No credential was found for the account/device (nothing to select or authenticate with).
        is NoCredentialException -> PasskeyError.NoCredentialAvailable

        // Provider is not configured or environment is incompatible/outdated
        // (e.g., missing Play Services, unsupported provider).
        is CreateCredentialProviderConfigurationException,
        is GetCredentialProviderConfigurationException -> PasskeyError.ProviderUnavailable

        // Generic failures originating from the Create/Get flows that don't carry DOM details.
        is CreateCredentialException,
        is GetCredentialException -> PasskeyError.OperationFailed(throwable)

        // Fallback:
        // Report a generic operation failure and retain the original cause.
        else ->  PasskeyError.OperationFailed(throwable)
    }
)