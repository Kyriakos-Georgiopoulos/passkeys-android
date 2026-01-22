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

import android.webkit.JavascriptInterface
import example.webview.mvi.PasskeysWebViewIntent

internal class PasskeysJsBridge(
    private val allowedOriginProvider: () -> Boolean,
    private val onIntent: (PasskeysWebViewIntent) -> Unit
) {

    @JavascriptInterface
    fun register(requestJson: String, callbackId: String) {
        if (!allowedOriginProvider()) {
            onIntent(PasskeysWebViewIntent.InvalidOrigin(callbackId))
            return
        }

        onIntent(
            PasskeysWebViewIntent.JsRegister(
                requestJson = requestJson,
                callbackId = callbackId
            )
        )
    }

    @JavascriptInterface
    fun signIn(requestJson: String, callbackId: String) {
        if (!allowedOriginProvider()) {
            onIntent(PasskeysWebViewIntent.InvalidOrigin(callbackId))
            return
        }

        onIntent(
            PasskeysWebViewIntent.JsSignIn(
                requestJson = requestJson,
                callbackId = callbackId
            )
        )
    }
}

