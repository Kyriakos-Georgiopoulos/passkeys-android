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

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Shows a web page that creates and uses passkeys with the standard
 * `navigator.credentials` API, the same code that runs in a browser.
 *
 * The page talks to the server itself. The app only switches WebAuthn on
 * for the WebView, so the request reaches the device's passkey provider.
 *
 * @param url Page to load. Navigation stays on this page's origin.
 * @param modifier Modifier applied to the root container.
 */
@Composable
internal fun PasskeysWebViewScreen(
    url: String,
    modifier: Modifier = Modifier
) {
    var passkeysEnabled by remember { mutableStateOf(true) }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT

                    webViewClient = PasskeysWebViewClient(url.toOrigin())

                    passkeysEnabled = enablePasskeys()

                    loadUrl(url)
                }
            }
        )

        if (!passkeysEnabled) {
            Text(
                text = "This WebView does not support passkeys.",
                modifier = Modifier.align(Alignment.BottomCenter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Switches WebAuthn on for pages in this WebView, on behalf of this app.
 *
 * WebAuthn is off in a WebView by default. The app must be linked to the
 * page's domain with Digital Asset Links for the request to be accepted.
 *
 * @return `true` if the installed WebView has the feature and kept the setting.
 */
private fun WebView.enablePasskeys(): Boolean {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) return false

    WebSettingsCompat.setWebAuthenticationSupport(
        settings,
        WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP
    )

    // The WebView can refuse the setting, so read it back.
    return WebSettingsCompat.getWebAuthenticationSupport(settings) ==
            WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP
}
