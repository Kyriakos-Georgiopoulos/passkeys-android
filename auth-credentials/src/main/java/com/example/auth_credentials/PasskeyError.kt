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

sealed interface PasskeyError {
    /** Device/profile doesn’t support Credential Manager or no providers available. */
    data object Unsupported : PasskeyError

    /** No suitable credential/provider for the given request/options. */
    data object NoCredentialAvailable : PasskeyError

    /** Provider is misconfigured / temporarily unavailable. */
    data object ProviderUnavailable : PasskeyError

    /** User intentionally canceled the flow. */
    data object CanceledByUser : PasskeyError

    /** Any other failure. Keep the cause around for logs/diagnostics. */
    data class OperationFailed(val cause: Throwable? = null) : PasskeyError
}

/**
 * A Throwable wrapper that carries a PasskeyError for use with Kotlin Result.
 * Never throw it across your app boundary; *return* Result instead.
 */
class PasskeyException(val error: PasskeyError) : RuntimeException(
    when (error) {
        PasskeyError.Unsupported -> "Passkeys unsupported on this device/profile"
        PasskeyError.NoCredentialAvailable -> "No passkey available for this request"
        PasskeyError.ProviderUnavailable -> "Credential provider unavailable/misconfigured"
        PasskeyError.CanceledByUser -> "Operation canceled by user"
        is PasskeyError.OperationFailed -> "Passkey operation failed: ${error.cause?.message.orEmpty()}"
    },
    (error as? PasskeyError.OperationFailed)?.cause
)

inline fun <R> Result<PublicKeyResponseJson>.foldRich(
    onSuccess: (PublicKeyResponseJson) -> R,
    onError: (PasskeyError) -> R
): R = fold(onSuccess) { throwable ->
    val pe = throwable as? PasskeyException
    onError(pe?.error ?: PasskeyError.OperationFailed(throwable))
}

fun Result<PublicKeyResponseJson>.errorOrNull(): PasskeyError? =
    exceptionOrNull()?.let { (it as? PasskeyException)?.error ?: PasskeyError.OperationFailed(it) }