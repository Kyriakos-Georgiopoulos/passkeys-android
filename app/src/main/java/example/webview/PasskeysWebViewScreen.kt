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

// example.passkeys.web.PasskeysWebViewScreen

import android.app.Activity
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth_credentials.PasskeyError
import com.example.auth_credentials.PasskeysManager
import example.webview.mvi.PasskeysWebViewEffect
import example.webview.mvi.PasskeysWebViewIntent

@Composable
internal fun PasskeysWebViewScreen(
    passkeysManager: PasskeysManager,
    allowedOrigin: String,
    viewModel: PasskeysWebViewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity

    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT

                    webViewClient = PasskeysWebViewClient(allowedOrigin)

                    addJavascriptInterface(
                        PasskeysJsBridge(
                            allowedOriginProvider = { url?.toOrigin() == allowedOrigin },
                            onIntent = viewModel::onIntent
                        ),
                        "AndroidPasskeys"
                    )

                    webView = this
                    loadUrl(allowedOrigin)
                }
            }
        )

        if (state.loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        state.lastError?.let { error ->
            Text(
                text = error.userMessageForWeb(),
                modifier = Modifier.align(Alignment.BottomCenter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    // Handle side-effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            val wv = webView ?: return@collect

            when (effect) {
                is PasskeysWebViewEffect.PerformRegistration -> {
                    val result = passkeysManager.register(
                        activity = activity,
                        requestJson = effect.requestJson
                    )
                    viewModel.onIntent(
                        PasskeysWebViewIntent.RegistrationResult(
                            callbackId = effect.callbackId,
                            result = result
                        )
                    )
                }

                is PasskeysWebViewEffect.PerformSignIn -> {
                    val result = passkeysManager.signIn(
                        activity = activity,
                        requestJson = effect.requestJson
                    )
                    viewModel.onIntent(
                        PasskeysWebViewIntent.SignInResult(
                            callbackId = effect.callbackId,
                            result = result
                        )
                    )
                }

                is PasskeysWebViewEffect.SendSuccess -> {
                    val safeJson = effect.responseJson.escapeForJsString()
                    val js = """
                        window.Passkeys && window.Passkeys.onSuccess &&
                        window.Passkeys.onSuccess("${effect.callbackId}", $safeJson);
                    """.trimIndent()
                    wv.evaluateJavascript(js, null)
                }

                is PasskeysWebViewEffect.SendError -> {
                    val code = effect.error.toJsCode()
                    val msg = effect.message.escapeForJsString()
                    val js = """
                        window.Passkeys && window.Passkeys.onError &&
                        window.Passkeys.onError("${effect.callbackId}", "$code", $msg);
                    """.trimIndent()
                    wv.evaluateJavascript(js, null)
                }
            }
        }
    }
}

private fun PasskeyError.toJsCode(): String = when (this) {
    PasskeyError.Unsupported -> "UNSUPPORTED"
    PasskeyError.NoCredentialAvailable -> "NO_CREDENTIAL"
    PasskeyError.ProviderUnavailable -> "PROVIDER_UNAVAILABLE"
    PasskeyError.CanceledByUser -> "CANCELED"
    is PasskeyError.OperationFailed -> "OPERATION_FAILED"
}

private fun String.escapeForJsString(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

private fun PasskeyError.userMessageForWeb(): String = when (this) {
    PasskeyError.Unsupported -> "Passkeys are not supported on this device."
    PasskeyError.NoCredentialAvailable -> "No passkey available for this request."
    PasskeyError.ProviderUnavailable -> "Credential provider unavailable."
    PasskeyError.CanceledByUser -> "You canceled the operation."
    is PasskeyError.OperationFailed -> "Passkey operation failed."
}
