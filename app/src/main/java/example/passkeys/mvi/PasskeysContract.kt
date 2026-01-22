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

package example.passkeys.mvi

import com.example.auth_credentials.PasskeyError
import com.example.domain.model.AuthenticationOptionsJson
import com.example.domain.model.RegistrationOptionsJson

internal sealed interface PasskeysIntent {
    data class BeginRegistration(val userId: String) : PasskeysIntent
    data class BeginLogin(val usernameOrEmail: String) : PasskeysIntent
    data class RegistrationDone(val responseJson: String) : PasskeysIntent
    data class AuthenticationDone(val responseJson: String) : PasskeysIntent
    data class Failed(val error: PasskeyError) : PasskeysIntent
}

internal data class PasskeysState(
    val loading: Boolean = false,
    val error: PasskeyError? = null
)

internal sealed interface PasskeysEffect {
    data class LaunchCreate(val json: RegistrationOptionsJson) : PasskeysEffect
    data class LaunchGet(val json: AuthenticationOptionsJson) : PasskeysEffect
}