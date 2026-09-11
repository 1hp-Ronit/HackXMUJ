package com.pulsenet.app.data.repository

import com.pulsenet.app.data.local.entity.MessageEntity
import com.pulsenet.app.mesh.FakeMessageDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncRepositoryTest {

    private lateinit var dao: FakeMessageDao

    @Before
    fun setUp() {
        dao = FakeMessageDao()
    }

    private fun message(id: String, lat: Double = 26.9124, lng: Double = 75.7873) = MessageEntity(
        messageId = id,
        senderPublicKey = "pubkey",
        senderAlias = "Ronit",
        content = "test $id",
        latitude = lat,
        longitude = lng,
        priority = 2,
        hopCount = 0,
        signature = "sig",
        createdAtEpochMs = 1_726_000_000_000,
        receivedAtEpochMs = 1_726_000_000_000
    )

    @Test
    fun flushSendsGeoJsonWithLongitudeFirst() = runTest {
        dao.insertMessage(message("msg-1", lat = 26.9124, lng = 75.7873))
        val fakeApi = FakeBackendApiService()
        val repository = SyncRepository(dao, fakeApi)

        val success = repository.flushUnsyncedMessages()

        assertTrue(success)
        val coordinates = fakeApi.receivedRequests.single().documents.single().location.coordinates
        assertEquals(listOf(75.7873, 26.9124), coordinates)
    }

    @Test
    fun flushMarksMessagesSyncedOnSuccess() = runTest {
        dao.insertMessage(message("msg-1"))
        val repository = SyncRepository(dao, FakeBackendApiService())

        repository.flushUnsyncedMessages()

        assertTrue(dao.getMessageById("msg-1")!!.isSynced)
    }

    @Test
    fun flushChunksIntoBatchesOfFifty() = runTest {
        repeat(120) { dao.insertMessage(message("msg-$it")) }
        val fakeApi = FakeBackendApiService()
        val repository = SyncRepository(dao, fakeApi)

        repository.flushUnsyncedMessages()

        assertEquals(3, fakeApi.receivedRequests.size)
        assertEquals(50, fakeApi.receivedRequests[0].documents.size)
        assertEquals(50, fakeApi.receivedRequests[1].documents.size)
        assertEquals(20, fakeApi.receivedRequests[2].documents.size)
    }

    @Test
    fun flushStopsAndReportsFailureOnHttpError() = runTest {
        repeat(60) { dao.insertMessage(message("msg-$it")) }
        val fakeApi = FakeBackendApiService(failOnCallIndex = 1)
        val repository = SyncRepository(dao, fakeApi)

        val success = repository.flushUnsyncedMessages()

        assertFalse(success)
        // The first batch of 50 succeeded and was marked synced before the second batch failed.
        assertEquals(50, (0 until 50).count { dao.getMessageById("msg-$it")!!.isSynced })
        assertFalse(dao.getMessageById("msg-50")!!.isSynced)
    }

    @Test
    fun flushWithNoUnsyncedMessagesIsANoOp() = runTest {
        val fakeApi = FakeBackendApiService()
        val repository = SyncRepository(dao, fakeApi)

        val success = repository.flushUnsyncedMessages()

        assertTrue(success)
        assertTrue(fakeApi.receivedRequests.isEmpty())
    }
}
