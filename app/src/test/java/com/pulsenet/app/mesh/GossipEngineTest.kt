package com.pulsenet.app.mesh

import com.pulsenet.app.data.local.entity.MessageEntity
import com.pulsenet.app.security.Ed25519Crypto
import com.pulsenet.app.security.KeySigner
import com.pulsenet.app.security.MessageSigner
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class GossipEngineTest {

    private lateinit var dao: FakeMessageDao
    private lateinit var transport: FakeMeshTransport
    private lateinit var messageSigner: MessageSigner
    private lateinit var gossipEngine: GossipEngine
    private lateinit var senderPublicKey: String

    private val moshi = Moshi.Builder().build()
    private val hashListAdapter = moshi.adapter(HashListPayload::class.java)
    private val messageBatchAdapter = moshi.adapter(MessageBatchPayload::class.java)

    @Before
    fun setUp() {
        val keyPair = Ed25519Crypto.generateKeyPair()
        senderPublicKey = Base64.getEncoder().encodeToString(keyPair.publicKey)
        val keySigner = object : KeySigner {
            override fun getPublicKeyBase64() = senderPublicKey
            override fun sign(payload: ByteArray) = Ed25519Crypto.sign(keyPair.privateKey, payload)
            override fun verify(payload: ByteArray, signature: ByteArray, publicKeyBase64: String) =
                Ed25519Crypto.verify(Base64.getDecoder().decode(publicKeyBase64), payload, signature)
        }
        messageSigner = MessageSigner(keySigner)
        dao = FakeMessageDao()
        transport = FakeMeshTransport()
        gossipEngine = GossipEngine(transport, dao, messageSigner)
    }

    private fun signedMessage(
        id: String,
        priority: Int = 3,
        hopCount: Int = 0,
        maxHops: Int = 7,
        createdAt: Long = 1_726_000_000_000,
        content: String = "test content $id"
    ): MessageEntity {
        val signature = messageSigner.sign(id, content, 26.9124, 75.7873, priority, createdAt)
        return MessageEntity(
            messageId = id,
            senderPublicKey = senderPublicKey,
            senderAlias = "Ronit",
            content = content,
            latitude = 26.9124,
            longitude = 75.7873,
            priority = priority,
            hopCount = hopCount,
            maxHops = maxHops,
            signature = signature,
            createdAtEpochMs = createdAt,
            receivedAtEpochMs = createdAt
        )
    }

    @Test
    fun syncWithSendsLocalHashList() = runTest {
        dao.insertMessage(signedMessage("msg-1"))
        dao.insertMessage(signedMessage("msg-2"))

        gossipEngine.syncWith("peer-1")

        val json = transport.lastPayloadJsonTo("peer-1")
        val sent = hashListAdapter.fromJson(json)!!
        assertEquals(HashListPayload.TYPE, sent.type)
        assertEquals(setOf("msg-1", "msg-2"), sent.messageIds.toSet())
    }

    @Test
    fun hashListDeltaSendsOnlyMissingMessagesSosFirst() = runTest {
        dao.insertMessage(signedMessage("general-1", priority = 3, createdAt = 2000))
        dao.insertMessage(signedMessage("sos-1", priority = 0, createdAt = 1000))
        dao.insertMessage(signedMessage("known-1", priority = 3, createdAt = 3000))

        // Peer already has "known-1"; everything else is missing on the peer.
        val peerHashList = hashListAdapter.toJson(HashListPayload(messageIds = listOf("known-1")))
        gossipEngine.processIncoming("peer-1", peerHashList.toByteArray())

        val batchJson = transport.lastPayloadJsonTo("peer-1")
        val batch = messageBatchAdapter.fromJson(batchJson)!!
        assertEquals(MessageBatchPayload.TYPE, batch.type)
        assertEquals(listOf("sos-1", "general-1"), batch.messages.map { it.messageId })
    }

    @Test
    fun hashListWithNothingMissingSendsNoBatch() = runTest {
        dao.insertMessage(signedMessage("msg-1"))
        val peerHashList = hashListAdapter.toJson(HashListPayload(messageIds = listOf("msg-1")))

        gossipEngine.processIncoming("peer-1", peerHashList.toByteArray())

        assertTrue(transport.sentPayloads.isEmpty())
    }

    @Test
    fun messageBatchInsertsValidSignedMessageWithIncrementedHop() = runTest {
        val wireMessage = signedMessage("msg-1", hopCount = 2).toWireMessage()
        val batchJson = messageBatchAdapter.toJson(MessageBatchPayload(messages = listOf(wireMessage)))

        gossipEngine.processIncoming("peer-1", batchJson.toByteArray())

        val stored = dao.getMessageById("msg-1")
        assertEquals(3, stored?.hopCount)
    }

    @Test
    fun messageBatchDropsMessageAtMaxHopCount() = runTest {
        val wireMessage = signedMessage("msg-1", hopCount = 7, maxHops = 7).toWireMessage()
        val batchJson = messageBatchAdapter.toJson(MessageBatchPayload(messages = listOf(wireMessage)))

        gossipEngine.processIncoming("peer-1", batchJson.toByteArray())

        assertNull(dao.getMessageById("msg-1"))
    }

    @Test
    fun messageBatchDropsTamperedSignature() = runTest {
        val original = signedMessage("msg-1").toWireMessage()
        val tampered = original.copy(content = "attacker-modified content")
        val batchJson = messageBatchAdapter.toJson(MessageBatchPayload(messages = listOf(tampered)))

        gossipEngine.processIncoming("peer-1", batchJson.toByteArray())

        assertNull(dao.getMessageById("msg-1"))
    }

    @Test
    fun messageBatchIgnoresDuplicateAlreadyInDao() = runTest {
        val original = signedMessage("msg-1", hopCount = 1)
        dao.insertMessage(original)

        val rebroadcast = original.copy(hopCount = 4).toWireMessage()
        val batchJson = messageBatchAdapter.toJson(MessageBatchPayload(messages = listOf(rebroadcast)))
        gossipEngine.processIncoming("peer-1", batchJson.toByteArray())

        assertEquals(1, dao.getMessageById("msg-1")?.hopCount)
    }

    @Test
    fun broadcastOwnMessageSendsToAllConnectedPeers() = runTest {
        transport.connectedEndpointIds.addAll(listOf("peer-1", "peer-2"))
        val message = signedMessage("sos-1", priority = 0)

        gossipEngine.broadcastOwnMessage(message)

        val batchAtPeer1 = messageBatchAdapter.fromJson(transport.lastPayloadJsonTo("peer-1"))!!
        val batchAtPeer2 = messageBatchAdapter.fromJson(transport.lastPayloadJsonTo("peer-2"))!!
        assertEquals(listOf("sos-1"), batchAtPeer1.messages.map { it.messageId })
        assertEquals(listOf("sos-1"), batchAtPeer2.messages.map { it.messageId })
    }

    @Test
    fun broadcastOwnMessageAtMaxHopsSendsNothing() = runTest {
        transport.connectedEndpointIds.add("peer-1")
        val message = signedMessage("sos-1", hopCount = 7, maxHops = 7)

        gossipEngine.broadcastOwnMessage(message)

        assertTrue(transport.sentPayloads.isEmpty())
    }

    private fun MessageEntity.toWireMessage() = WireMessage(
        messageId = messageId,
        senderPublicKey = senderPublicKey,
        senderAlias = senderAlias,
        content = content,
        latitude = latitude,
        longitude = longitude,
        priority = priority,
        hopCount = hopCount,
        maxHops = maxHops,
        signature = signature,
        createdAtEpochMs = createdAtEpochMs
    )
}
