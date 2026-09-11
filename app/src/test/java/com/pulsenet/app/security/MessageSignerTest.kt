package com.pulsenet.app.security

import com.pulsenet.app.data.local.entity.MessageEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class MessageSignerTest {

    private lateinit var keyPair: Ed25519Crypto.RawKeyPair
    private lateinit var fakeKeySigner: KeySigner
    private lateinit var messageSigner: MessageSigner

    @Before
    fun setUp() {
        keyPair = Ed25519Crypto.generateKeyPair()
        fakeKeySigner = object : KeySigner {
            override fun getPublicKeyBase64() = Base64.getEncoder().encodeToString(keyPair.publicKey)
            override fun sign(payload: ByteArray) = Ed25519Crypto.sign(keyPair.privateKey, payload)
            override fun verify(payload: ByteArray, signature: ByteArray, publicKeyBase64: String) =
                Ed25519Crypto.verify(Base64.getDecoder().decode(publicKeyBase64), payload, signature)
        }
        messageSigner = MessageSigner(fakeKeySigner)
    }

    private fun sampleMessage(content: String = "Need water urgently") = MessageEntity(
        messageId = "msg-1",
        senderPublicKey = fakeKeySigner.getPublicKeyBase64(),
        senderAlias = "Ronit",
        content = content,
        latitude = 26.9124,
        longitude = 75.7873,
        priority = 2,
        hopCount = 0,
        signature = "",
        createdAtEpochMs = 1_726_000_000_000,
        receivedAtEpochMs = 1_726_000_000_000
    )

    private fun signSample(message: MessageEntity): String = messageSigner.sign(
        message.messageId,
        message.content,
        message.latitude,
        message.longitude,
        message.priority,
        message.createdAtEpochMs
    )

    @Test
    fun signThenVerifySucceeds() {
        val message = sampleMessage()
        val signed = message.copy(signature = signSample(message))
        assertTrue(messageSigner.verify(signed))
    }

    @Test
    fun tamperedContentFailsVerification() {
        val message = sampleMessage()
        val signature = signSample(message)
        val tampered = message.copy(signature = signature, content = "Everything is fine")
        assertFalse(messageSigner.verify(tampered))
    }

    @Test
    fun tamperedSignatureFailsVerification() {
        val message = sampleMessage()
        val signature = signSample(message)
        val corrupted = signature.dropLast(4) + "abcd"
        val signed = message.copy(signature = corrupted)
        assertFalse(messageSigner.verify(signed))
    }

    @Test
    fun wrongSenderKeyFailsVerification() {
        val message = sampleMessage()
        val signature = signSample(message)
        val otherKeyPair = Ed25519Crypto.generateKeyPair()
        val signed = message.copy(
            signature = signature,
            senderPublicKey = Base64.getEncoder().encodeToString(otherKeyPair.publicKey)
        )
        assertFalse(messageSigner.verify(signed))
    }
}
