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

package example.passkeys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.auth_credentials.PasskeyError
import com.example.auth_credentials.PasskeyException
import com.example.auth_credentials.PasskeysManager
import example.passkeys.mvi.PasskeysEffect
import example.passkeys.mvi.PasskeysIntent
import example.passkeys.mvi.PasskeysState
import example.passkeys.mvi.PasskeysViewModel

@AndroidEntryPoint
class PasskeysActivity : ComponentActivity() {

    private val viewModel: PasskeysViewModel by viewModels()

    @Inject
    lateinit var passkeysManager: PasskeysManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state: PasskeysState by viewModel.state.collectAsStateWithLifecycle()

            val activity = this@PasskeysActivity

            LaunchedEffect(Unit) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is PasskeysEffect.LaunchCreate -> {
                            val result = passkeysManager.register(
                                activity = activity,
                                requestJson = effect.json.value
                            )
                            result
                                .onSuccess { response ->
                                    viewModel.onIntent(
                                        PasskeysIntent.RegistrationDone(response.value)
                                    )
                                }
                                .onFailure { throwable ->
                                    val error = (throwable as? PasskeyException)?.error
                                        ?: PasskeyError.OperationFailed(throwable)
                                    viewModel.onIntent(PasskeysIntent.Failed(error))
                                }
                        }

                        is PasskeysEffect.LaunchGet -> {
                            val result = passkeysManager.signIn(
                                activity = activity,
                                requestJson = effect.json.value
                            )
                            result
                                .onSuccess { response ->
                                    viewModel.onIntent(
                                        PasskeysIntent.AuthenticationDone(response.value)
                                    )
                                }
                                .onFailure { throwable ->
                                    val error = (throwable as? PasskeyException)?.error
                                        ?: PasskeyError.OperationFailed(throwable)
                                    viewModel.onIntent(PasskeysIntent.Failed(error))
                                }
                        }
                    }
                }
            }

            PasskeysScreen(
                state = state,
                onIntent = viewModel::onIntent
            )
        }
    }
}
