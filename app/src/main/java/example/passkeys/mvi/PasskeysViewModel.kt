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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth_credentials.PasskeyError
import com.example.data.repository.PasskeysRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class PasskeysViewModel @Inject constructor(
    private val repository: PasskeysRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PasskeysState())
    val state: StateFlow<PasskeysState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PasskeysEffect>()
    val effects: SharedFlow<PasskeysEffect> = _effects.asSharedFlow()

    fun onIntent(intent: PasskeysIntent) {
        when (intent) {
            is PasskeysIntent.BeginRegistration -> beginRegistration(intent.userId)
            is PasskeysIntent.BeginLogin -> beginLogin(intent.usernameOrEmail)
            is PasskeysIntent.RegistrationDone -> onRegistrationDone(intent.responseJson)
            is PasskeysIntent.AuthenticationDone -> onAuthenticationDone(intent.responseJson)
            is PasskeysIntent.Failed -> onFailed(intent.error)
        }
    }

    private fun beginRegistration(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val optionsJson = repository.getRegistrationOptions(userId)
                _effects.emit(PasskeysEffect.LaunchCreate(optionsJson))
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = PasskeyError.OperationFailed(t)
                    )
                }
            }
        }
    }

    private fun beginLogin(usernameOrEmail: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val optionsJson = repository.getAuthenticationOptions(usernameOrEmail)
                _effects.emit(PasskeysEffect.LaunchGet(optionsJson))
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = PasskeyError.OperationFailed(t)
                    )
                }
            }
        }
    }

    private fun onRegistrationDone(responseJson: String) {
        _state.update { it.copy(loading = false, error = null) }
    }

    private fun onAuthenticationDone(responseJson: String) {
        _state.update { it.copy(loading = false, error = null) }
    }

    private fun onFailed(error: PasskeyError) {
        _state.update { it.copy(loading = false, error = error) }
    }
}
