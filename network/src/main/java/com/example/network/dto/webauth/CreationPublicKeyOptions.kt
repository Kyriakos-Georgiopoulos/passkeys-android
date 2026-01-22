package com.example.network.dto.webauth

import kotlinx.serialization.Serializable

/**
 * # WebAuthn "create" (registration) JSON examples
 *
 * This reflects the payload returned by your backend during the
 * **begin-registration** step of passkey creation. The structure matches:
 *
 *   PublicKeyCredentialCreationOptions → { "publicKey": { ... } }
 *
 * Example:
 * ```json
 * {
 *   "publicKey": {
 *     "rp": { "id": "example.com", "name": "Example, Inc." },
 *     "user": {
 *       "id": "dXNlci0xMjM",                     // opaque Base64URL user handle
 *       "name": "kyriakos@example.com",          // username/email shown in selector
 *       "displayName": "Kyriakos Georgiopoulos"  // human-friendly display name
 *     },
 *     "challenge": "N4v4kS8m8T9rA7dI9rOZqQ",      // single-use Base64URL challenge
 *     "pubKeyCredParams": [
 *       { "type": "public-key", "alg": -7 }      // ES256 (required for passkeys)
 *     ],
 *     "authenticatorSelection": {
 *       "residentKey": "required",               // discoverable (passkey) credential
 *       "userVerification": "required",          // biometric/PIN
 *       "authenticatorAttachment": "platform"    // on-device authenticator
 *     },
 *     "attestation": "none"
 *   }
 * }
 * ```
 */

/**
 * Wrapper for WebAuthn registration options.
 *
 * Many WebAuthn servers return `{ "publicKey": { ... } }`.
 * The contained object maps directly to the options passed into
 * Credential Manager's `CreatePublicKeyCredentialRequest`.
 */
@Serializable
data class CreationOptions(
    /** WebAuthn PublicKeyCredentialCreationOptions. */
    val publicKey: CreationPublicKeyOptions? = null
)

/**
 * Relying Party (RP) information.
 *
 * - **id**: Effective domain for the RP (e.g., "example.com"). Must match the origin
 *   your server uses when generating WebAuthn challenges.
 * - **name**: Human-readable name shown in the platform account selector.
 */
@Serializable
data class Rp(
    val id: String? = null,
    val name: String? = null
)

/**
 * Information about the user account for which the passkey is created.
 *
 * - **id**: Stable, opaque, Base64URL-encoded user handle. Must not contain PII.
 * - **name**: User-recognizable identifier (email/username) shown in the UI.
 * - **displayName**: Human-friendly label used for account selection screens.
 */
@Serializable
data class User(
    val id: String? = null,
    val name: String? = null,
    val displayName: String? = null
)

/**
 * Public-key credential parameters used to define acceptable algorithms.
 *
 * - **type**: Always `"public-key"` for WebAuthn.
 * - **alg**: COSE algorithm identifier (e.g., -7 = ES256).
 *
 * ES256 (alg = -7) is required for passkey support.
 */
@Serializable
data class Param(
    val type: String? = null,
    val alg: Int? = null
)

/**
 * Describes an existing credential that the authenticator may use or exclude.
 *
 * Used in:
 * - **Registration (`excludeCredentials`)** → to prevent creating a credential that already exists.
 * - **Authentication (`allowCredentials`)** → to specify which credentials are allowed.
 *
 * @property type Always `"public-key"` for WebAuthn credentials.
 * @property id Base64URL-encoded credential ID (unique identifier of the credential).
 * @property transports Optional list of allowed authenticator transports for this credential.
 * Can help the client identify the right authenticator.
 * Possible values include:
 * - `"usb"` → USB security keys
 * - `"nfc"` → NFC-based authenticators
 * - `"ble"` → Bluetooth Low Energy
 * - `"hybrid"` → Hybrid transport (platform + phone-based)
 * - `"internal"` → Platform authenticator (e.g., Touch ID, Android Biometrics)
 */
@Serializable
data class CredDescriptor(
    val type: String? = null,
    val id: String? = null,
    val transports: List<String>? = null
)

/**
 * Criteria that guide authenticator selection during passkey creation.
 *
 * - **residentKey**: Whether a discoverable credential is required.
 *   Passkeys require `"required"`.
 * - **userVerification**: Biometric/PIN requirements ("required" recommended).
 * - **authenticatorAttachment**:
 *     - `"platform"` for on-device authenticators (Android passkeys)
 *     - `"cross-platform"` for roaming authenticators (USB/NFC keys)
 */
@Serializable
data class AuthenticatorSelection(
    val residentKey: String? = null,
    val userVerification: String? = null,
    val authenticatorAttachment: String? = null
)

/**
 * PublicKeyCredentialCreationOptions (subset, all optional for resilient parsing).
 * Typically provided by your server on "begin registration".
 *
 * @property rp Relying Party info (domain + display name).
 * @property user User account info (opaque id + username + display name).
 * @property challenge Fresh, random Base64URL challenge (server-generated, single-use).
 * @property pubKeyCredParams Accepted COSE algorithms (e.g., -7 for ES256).
 * @property timeout Optional timeout (ms) for the operation.
 * @property excludeCredentials Existing credential IDs to prevent re-registration.
 * @property authenticatorSelection Hints for resident key / verification / attachment.
 * @property attestation "none" (typical), "indirect", or "direct" (attestation certs).
 */
@Serializable
data class CreationPublicKeyOptions(
    val rp: Rp? = null,
    val user: User? = null,
    val challenge: String? = null,
    val pubKeyCredParams: List<Param?>? = null,
    val timeout: Int? = null,
    val excludeCredentials: List<CredDescriptor?>? = null,
    val authenticatorSelection: AuthenticatorSelection? = null,
    val attestation: String? = null
)
