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

package example.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth_credentials.PasskeyError
import com.example.auth_credentials.PublicKeyResponseJson
import com.example.auth_credentials.errorOrNull
import com.example.auth_credentials.foldRich
import dagger.hilt.android.lifecycle.HiltViewModel
import example.webview.mvi.PasskeysWebViewEffect
import example.webview.mvi.PasskeysWebViewIntent
import example.webview.mvi.PasskeysWebViewState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PasskeysWebViewViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(PasskeysWebViewState())
    val state: StateFlow<PasskeysWebViewState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PasskeysWebViewEffect>()
    val effects: SharedFlow<PasskeysWebViewEffect> = _effects.asSharedFlow()

    fun onIntent(intent: PasskeysWebViewIntent) {
        when (intent) {
            is PasskeysWebViewIntent.JsRegister -> {
                _state.update { it.copy(loading = true, lastError = null) }
                viewModelScope.launch {
                    _effects.emit(
                        PasskeysWebViewEffect.PerformRegistration(
                            requestJson = intent.requestJson,
                            callbackId = intent.callbackId
                        )
                    )
                }
            }

            is PasskeysWebViewIntent.JsSignIn -> {
                _state.update { it.copy(loading = true, lastError = null) }
                viewModelScope.launch {
                    _effects.emit(
                        PasskeysWebViewEffect.PerformSignIn(
                            requestJson = intent.requestJson,
                            callbackId = intent.callbackId
                        )
                    )
                }
            }

            is PasskeysWebViewIntent.InvalidOrigin -> {
                viewModelScope.launch {
                    _effects.emit(
                        PasskeysWebViewEffect.SendError(
                            callbackId = intent.callbackId,
                            error = PasskeyError.ProviderUnavailable,
                            message = "Origin not allowed"
                        )
                    )
                }
            }

            is PasskeysWebViewIntent.RegistrationResult -> {
                handleResult(
                    callbackId = intent.callbackId,
                    result = intent.result
                )
            }

            is PasskeysWebViewIntent.SignInResult -> {
                handleResult(
                    callbackId = intent.callbackId,
                    result = intent.result
                )
            }
        }
    }

    private fun handleResult(
        callbackId: String,
        result: Result<PublicKeyResponseJson>
    ) {
        val error = result.errorOrNull()
        _state.update { it.copy(loading = false, lastError = error) }

        viewModelScope.launch {
            result.foldRich(
                onSuccess = { response ->
                    _effects.emit(
                        PasskeysWebViewEffect.SendSuccess(
                            callbackId = callbackId,
                            responseJson = response.value
                        )
                    )
                },
                onError = { passkeyError ->
                    _effects.emit(
                        PasskeysWebViewEffect.SendError(
                            callbackId = callbackId,
                            error = passkeyError,
                            message = passkeyError.toString()
                        )
                    )
                }
            )
        }
    }
}
