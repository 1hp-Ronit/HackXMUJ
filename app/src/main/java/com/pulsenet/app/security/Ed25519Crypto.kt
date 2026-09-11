package com.pulsenet.app.security

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom

/**
 * Pure Bouncy Castle Ed25519 primitives operating on raw key/signature bytes.
 * Has no Android framework dependency so it can run in plain JVM unit tests
 * as well as on-device, unlike the AndroidKeyStore-backed path in [KeyManager].
 */
object Ed25519Crypto {

    data class RawKeyPair(val publicKey: ByteArray, val privateKey: ByteArray)

    fun generateKeyPair(): RawKeyPair {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val keyPair = generator.generateKeyPair()
        val privateKey = keyPair.private as Ed25519PrivateKeyParameters
        val publicKey = keyPair.public as Ed25519PublicKeyParameters
        return RawKeyPair(publicKey.encoded, privateKey.encoded)
    }

    fun sign(privateKeyBytes: ByteArray, payload: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(privateKeyBytes, 0))
        signer.update(payload, 0, payload.size)
        return signer.generateSignature()
    }

    fun verify(publicKeyBytes: ByteArray, payload: ByteArray, signature: ByteArray): Boolean =
        try {
            val verifier = Ed25519Signer()
            verifier.init(false, Ed25519PublicKeyParameters(publicKeyBytes, 0))
            verifier.update(payload, 0, payload.size)
            verifier.verifySignature(signature)
        } catch (e: Exception) {
            false
        }
}
