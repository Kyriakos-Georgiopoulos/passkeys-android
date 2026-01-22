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

package example.webview.mvi

import com.example.auth_credentials.PasskeyError
import com.example.auth_credentials.PublicKeyResponseJson

internal sealed interface PasskeysWebViewIntent {
    data class JsRegister(
        val requestJson: String,
        val callbackId: String
    ) : PasskeysWebViewIntent

    data class JsSignIn(
        val requestJson: String,
        val callbackId: String
    ) : PasskeysWebViewIntent

    data class InvalidOrigin(
        val callbackId: String
    ) : PasskeysWebViewIntent

    data class RegistrationResult(
        val callbackId: String,
        val result: Result<PublicKeyResponseJson>
    ) : PasskeysWebViewIntent

    data class SignInResult(
        val callbackId: String,
        val result: Result<PublicKeyResponseJson>
    ) : PasskeysWebViewIntent
}

internal data class PasskeysWebViewState(
    val loading: Boolean = false,
    val lastError: PasskeyError? = null
)

internal sealed interface PasskeysWebViewEffect {
    data class PerformRegistration(
        val requestJson: String,
        val callbackId: String
    ) : PasskeysWebViewEffect

    data class PerformSignIn(
        val requestJson: String,
        val callbackId: String
    ) : PasskeysWebViewEffect

    data class SendSuccess(
        val callbackId: String,
        val responseJson: String
    ) : PasskeysWebViewEffect

    data class SendError(
        val callbackId: String,
        val error: PasskeyError,
        val message: String
    ) : PasskeysWebViewEffect
}