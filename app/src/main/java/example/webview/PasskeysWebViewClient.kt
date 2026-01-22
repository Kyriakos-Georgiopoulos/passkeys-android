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

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class PasskeysWebViewClient(
    private val allowedOrigin: String
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val origin = request.url.toString().toOrigin()
        val allowed = origin == allowedOrigin
        return !allowed
    }
}

fun String.toOrigin(): String? = try {
    val uri = Uri.parse(this)
    val scheme = uri.scheme ?: return null
    val host = uri.host ?: return null
    val port = if (uri.port == -1) null else uri.port
    buildString {
        append(scheme)
        append("://")
        append(host)
        if (port != null && port !in listOf(80, 443)) {
            append(":")
            append(port)
        }
    }
} catch (_: Throwable) {
    null
}
