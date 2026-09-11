package com.pulsenet.app.security

/**
 * Abstraction over this device's Ed25519 identity, so [MessageSigner] can be
 * unit tested against a fake implementation without touching AndroidKeyStore.
 */
interface KeySigner {
    fun getPublicKeyBase64(): String
    fun sign(payload: ByteArray): ByteArray
    fun verify(payload: ByteArray, signature: ByteArray, publicKeyBase64: String): Boolean
}
