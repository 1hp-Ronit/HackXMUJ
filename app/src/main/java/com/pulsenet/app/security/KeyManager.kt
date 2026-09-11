package com.pulsenet.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and persists this device's Ed25519 identity keypair on first launch.
 *
 * Prefers signing directly inside AndroidKeyStore (hardware-backed where available).
 * Ed25519 keystore support varies by device/API level, so on any failure this falls
 * back to a software keypair (Bouncy Castle) whose private key is encrypted at rest
 * with an AES-256-GCM key that itself lives in AndroidKeyStore.
 */
@Singleton
class KeyManager @Inject constructor(
    @ApplicationContext context: Context
) : KeySigner {

    private companion object {
        const val PREFS_NAME = "pulsenet_identity"
        const val PREF_PUBLIC_KEY = "public_key"
        const val PREF_BACKING = "backing"
        const val PREF_BC_PRIVATE_ENC = "bc_private_key_enc"
        const val PREF_BC_PRIVATE_IV = "bc_private_key_iv"
        const val BACKING_KEYSTORE = "keystore"
        const val BACKING_BOUNCYCASTLE = "bouncycastle"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val IDENTITY_ALIAS = "pulsenet_identity_ed25519"
        const val AES_WRAP_ALIAS = "pulsenet_key_wrap_aes"
        const val ED25519_RAW_KEY_LENGTH = 32
        const val ED25519_ALGORITHM = "Ed25519"
        const val TAG = "KeyManager"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private var cachedBcPrivateKey: ByteArray? = null

    init {
        if (prefs.getString(PREF_PUBLIC_KEY, null) == null) {
            generateIdentity()
        }
    }

    override fun getPublicKeyBase64(): String =
        prefs.getString(PREF_PUBLIC_KEY, null) ?: error("PulseNet identity not initialized")

    override fun sign(payload: ByteArray): ByteArray =
        when (prefs.getString(PREF_BACKING, null)) {
            BACKING_KEYSTORE -> try {
                signWithKeystore(payload)
            } catch (e: Exception) {
                // A keystore identity that passed its generation-time probe can still
                // fail later (OS upgrade, key invalidation). Regenerating a software
                // identity beats leaving the user unable to send anything at all —
                // safe because every message embeds its own sender public key, so
                // already-sent messages stay verifiable under the previous one.
                Log.w(TAG, "Keystore signing failed; regenerating software identity", e)
                generateBouncyCastleIdentity()
                Ed25519Crypto.sign(bouncyCastlePrivateKey(), payload)
            }
            else -> Ed25519Crypto.sign(bouncyCastlePrivateKey(), payload)
        }

    override fun verify(payload: ByteArray, signature: ByteArray, publicKeyBase64: String): Boolean =
        try {
            Ed25519Crypto.verify(Base64.getDecoder().decode(publicKeyBase64), payload, signature)
        } catch (e: IllegalArgumentException) {
            false
        }

    private fun generateIdentity() {
        if (!tryGenerateKeystoreIdentity()) {
            generateBouncyCastleIdentity()
        }
    }

    private fun tryGenerateKeystoreIdentity(): Boolean {
        return try {
            // AndroidKeyStore exposes no KeyProperties constant for Ed25519, so this goes
            // through the raw JCA algorithm name. Newer Android versions do support it;
            // older ones throw here and fall through to the Bouncy Castle path below.
            val generator = KeyPairGenerator.getInstance(ED25519_ALGORITHM, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                IDENTITY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).build()
            generator.initialize(spec)
            val keyPair = generator.generateKeyPair()
            val rawPublicKey = extractRawEd25519PublicKey(keyPair.public.encoded)

            // Generating a key proves nothing on its own — a device can support Ed25519
            // keygen and still fail at signing time, or return a public key encoding that
            // doesn't match what peers would verify against. Exercise the whole path once
            // here and fall back to software keys unless a real signature verifies, rather
            // than discovering it later when a message fails to send.
            if (keystoreSigningWorks(rawPublicKey)) {
                prefs.edit()
                    .putString(PREF_PUBLIC_KEY, Base64.getEncoder().encodeToString(rawPublicKey))
                    .putString(PREF_BACKING, BACKING_KEYSTORE)
                    .apply()
                true
            } else {
                runCatching { keyStore.deleteEntry(IDENTITY_ALIAS) }
                false
            }
        } catch (e: Exception) {
            runCatching { keyStore.deleteEntry(IDENTITY_ALIAS) }
            false
        }
    }

    private fun keystoreSigningWorks(rawPublicKey: ByteArray): Boolean = try {
        val probe = "pulsenet-keystore-probe".toByteArray()
        Ed25519Crypto.verify(rawPublicKey, probe, signWithKeystore(probe))
    } catch (e: Exception) {
        false
    }

    private fun signWithKeystore(payload: ByteArray): ByteArray {
        val privateKey = keyStore.getKey(IDENTITY_ALIAS, null) as PrivateKey
        // The provider must be named explicitly: without it JCA picks whichever
        // provider claims Ed25519 first (Conscrypt), which can't operate on an
        // opaque AndroidKeyStore key handle and throws on initSign.
        return Signature.getInstance(ED25519_ALGORITHM, ANDROID_KEYSTORE).apply {
            initSign(privateKey)
            update(payload)
        }.sign()
    }

    private fun generateBouncyCastleIdentity() {
        val keyPair = Ed25519Crypto.generateKeyPair()
        val (encrypted, iv) = encryptWithKeystoreAes(keyPair.privateKey)
        prefs.edit()
            .putString(PREF_PUBLIC_KEY, Base64.getEncoder().encodeToString(keyPair.publicKey))
            .putString(PREF_BACKING, BACKING_BOUNCYCASTLE)
            .putString(PREF_BC_PRIVATE_ENC, Base64.getEncoder().encodeToString(encrypted))
            .putString(PREF_BC_PRIVATE_IV, Base64.getEncoder().encodeToString(iv))
            .apply()
        cachedBcPrivateKey = keyPair.privateKey
    }

    private fun bouncyCastlePrivateKey(): ByteArray =
        cachedBcPrivateKey ?: decryptBcPrivateKey().also { cachedBcPrivateKey = it }

    private fun decryptBcPrivateKey(): ByteArray {
        val encrypted = Base64.getDecoder().decode(prefs.getString(PREF_BC_PRIVATE_ENC, null))
        val iv = Base64.getDecoder().decode(prefs.getString(PREF_BC_PRIVATE_IV, null))
        val aesKey = keyStore.getKey(AES_WRAP_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun encryptWithKeystoreAes(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateAesWrapKey())
        }
        val encrypted = cipher.doFinal(data)
        return encrypted to cipher.iv
    }

    private fun getOrCreateAesWrapKey(): SecretKey {
        (keyStore.getKey(AES_WRAP_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                AES_WRAP_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    // AndroidKeyStore returns Ed25519 public keys as X.509 SubjectPublicKeyInfo DER.
    // That encoding has a fixed-size algorithm-identifier prefix, so the raw 32-byte
    // key is always the final 32 bytes regardless of provider.
    private fun extractRawEd25519PublicKey(x509Encoded: ByteArray): ByteArray =
        x509Encoded.copyOfRange(x509Encoded.size - ED25519_RAW_KEY_LENGTH, x509Encoded.size)
}
