package com.example.network.dto.webauth

import kotlinx.serialization.Serializable

/**
 * PublicKeyCredentialRequestOptions (login/"get")
 *
 * Fields follow the WebAuthn spec. All are nullable to allow resilient parsing.
 *
 * Minimal JSON example:
 * {
 *   "publicKey": {
 *     "rpId": "example.com",
 *     "challenge": "q2w3e4r5t6y7u8i9o0p",
 *     "allowCredentials": [
 *       { "type": "public-key", "id": "AbCdEfGhIj" }
 *     ],
 *     "timeout": 60000,
 *     "userVerification": "preferred"
 *   }
 * }
 */

/**
 * Wrapper for WebAuthn "get" options.
 * Many APIs return an object like { "publicKey": { ... } } that you pass to Credential Manager.
 */
@Serializable
data class RequestOptions(
    /** WebAuthn PublicKeyCredentialRequestOptions. */
    val publicKey: RequestPublicKeyOptions? = null
)


/**
 * @property rpId Effective RP ID (domain, e.g., "example.com"); must match the credential’s RP.
 * @property challenge Fresh, random Base64URL challenge (server-generated, single-use).
 * @property allowCredentials Optional whitelist of allowed credential IDs (Base64URL).
 * @property timeout Optional timeout (ms).
 * @property userVerification "required" | "preferred" | "discouraged".
 */
@Serializable
data class RequestPublicKeyOptions(
    val rpId: String? = null,
    val challenge: String? = null,
    val allowCredentials: List<CredDescriptor?>? = null,
    val timeout: Int? = null,
    val userVerification: String? = null
)
