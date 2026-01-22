package com.example.network.dto.webauth.validation

import com.example.network.dto.webauth.CreationOptions
import com.example.network.dto.webauth.CredDescriptor
import com.example.network.dto.webauth.Param
import com.example.network.dto.webauth.RequestOptions
import com.example.network.dto.webauth.Rp

internal object WebAuthnRules {
    val VALID_UV = setOf("required", "preferred", "discouraged")
    val VALID_RK = setOf("required", "preferred", "discouraged")
    val VALID_ATTACH = setOf("platform", "cross-platform")
    val VALID_TRANSPORTS = setOf("usb", "nfc", "ble", "hybrid", "internal")
    val VALID_ALGS = setOf(-7, -257) // ES256, RS256 (extend if you support more)
}

/** Tiny Base64URL check: allows A–Z a–z 0–9 _ - ; disallows '+', '/', '='. */
internal fun String?.isBase64Url(): Boolean {
    if (this.isNullOrEmpty()) return false
    for (c in this) {
        val ok = (c in 'A'..'Z') || (c in 'a'..'z') || (c in '0'..'9') || c == '_' || c == '-'
        if (!ok) return false
    }
    return true
}

internal fun List<CredDescriptor?>?.prunedCreds(): List<CredDescriptor?>? =
    this?.mapNotNull { cd ->
        if (cd == null) null else {
            val ok = cd.type?.equals("public-key", ignoreCase = true) == true &&
                    !cd.id.isNullOrBlank() &&
                    cd.id.isBase64Url() &&
                    (cd.transports == null || cd.transports.all { it in WebAuthnRules.VALID_TRANSPORTS })
            if (ok) cd else null
        }
    }

internal fun List<Param?>?.prunedParamsOrDefault(): List<Param?> {
    val cleaned = this?.mapNotNull { p ->
        if (p == null) null else {
            val ok = p.type?.equals("public-key", ignoreCase = true) == true &&
                    p.alg != null && p.alg in WebAuthnRules.VALID_ALGS
            if (ok) p else null
        }
    }.orEmpty()

    return cleaned.ifEmpty { listOf(Param(type = "public-key", alg = -7)) }
}

/**
 * Validate & sanitize LOGIN ("get") options.
 * - Ensures publicKey & challenge present and Base64URL.
 * - Pins rpId to expectedRpId.
 * - Prunes invalid allowCredentials items.
 * - Bounds timeout if present (1s..120s).
 */
fun RequestOptions.sanitized(expectedRpId: String): RequestOptions {
    val pk = publicKey ?: error("Missing 'publicKey'")

    val challenge = pk.challenge ?: error("Missing 'challenge'")
    require(challenge.isBase64Url()) { "Challenge must be Base64URL" }

    pk.userVerification?.let {
        require(it in WebAuthnRules.VALID_UV) { "Invalid userVerification: $it" }
    }

    pk.timeout?.let {
        require(it in 1_000..120_000) { "Unreasonable timeout: $it" }
    }

    val cleanedAllow = pk.allowCredentials.prunedCreds()

    return copy(
        publicKey = pk.copy(
            rpId = expectedRpId,
            allowCredentials = cleanedAllow
        )
    )
}

/**
 * Validate & sanitize REGISTRATION ("create") options.
 * - Ensures publicKey, rp/user present; pins rp.id to expectedRpId.
 * - Validates Base64URL user.id & challenge; requires user.name/displayName.
 * - Prunes/normalizes pubKeyCredParams (defaults to ES256 if none valid).
 * - Prunes invalid excludeCredentials.
 * - Validates authenticatorSelection enums.
 * - Bounds timeout if present (1s..120s).
 */
fun CreationOptions.sanitized(expectedRpId: String): CreationOptions {
    val pk = publicKey ?: error("Missing 'publicKey'")

    val fixedRp = (pk.rp ?: Rp()).copy(id = expectedRpId)

    val user = pk.user ?: error("Missing 'user'")
    require(!user.id.isNullOrBlank() && user.id.isBase64Url()) { "user.id must be Base64URL" }
    require(!user.name.isNullOrBlank()) { "user.name required" }
    require(!user.displayName.isNullOrBlank()) { "user.displayName required" }

    val challenge = pk.challenge ?: error("Missing 'challenge'")
    require(challenge.isBase64Url()) { "Challenge must be Base64URL" }

    val normalizedParams = pk.pubKeyCredParams.prunedParamsOrDefault()
    val cleanedExclude = pk.excludeCredentials.prunedCreds()

    pk.authenticatorSelection?.let { a ->
        a.residentKey?.let {
            require(it in WebAuthnRules.VALID_RK) { "Invalid residentKey: $it" }
        }
        a.userVerification?.let {
            require(it in WebAuthnRules.VALID_UV) { "Invalid userVerification: $it" }
        }
        a.authenticatorAttachment?.let {
            require(it in WebAuthnRules.VALID_ATTACH) { "Invalid authenticatorAttachment: $it" }
        }
    }

    pk.timeout?.let {
        require(it in 1_000..120_000) { "Unreasonable timeout: $it" }
    }

    return copy(
        publicKey = pk.copy(
            rp = fixedRp,
            pubKeyCredParams = normalizedParams,
            excludeCredentials = cleanedExclude
        )
    )
}
