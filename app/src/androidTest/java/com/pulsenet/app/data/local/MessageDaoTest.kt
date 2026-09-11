package com.pulsenet.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.data.local.entity.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageDaoTest {

    private lateinit var db: PulseDatabase
    private lateinit var dao: MessageDao

    private fun sampleMessage(id: String = "msg-1") = MessageEntity(
        messageId = id,
        senderPublicKey = "pubkey",
        senderAlias = "Ronit",
        content = "Need water urgently",
        latitude = 26.9124,
        longitude = 75.7873,
        priority = 2,
        hopCount = 0,
        signature = "sig",
        createdAtEpochMs = System.currentTimeMillis(),
        receivedAtEpochMs = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.messageDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveMessage() = runTest {
        dao.insertMessage(sampleMessage())
        val result = dao.getMessageById("msg-1")
        assertEquals("Need water urgently", result?.content)
    }

    @Test
    fun duplicateMessageIdIsIgnored() = runTest {
        dao.insertMessage(sampleMessage())
        dao.insertMessage(sampleMessage().copy(content = "Different content, same id"))
        assertEquals(1, dao.getMessageCount())
        assertEquals("Need water urgently", dao.getMessageById("msg-1")?.content)
    }

    @Test
    fun sosMessagesAreNeverEvicted() = runTest {
        val sos = sampleMessage("sos-1").copy(priority = 0, isSynced = true)
        val general = sampleMessage("general-1").copy(priority = 3, isSynced = true)
        dao.insertMessages(listOf(sos, general))

        dao.evictOldMessages(10)

        assertEquals(1, dao.getMessageCount())
        assertEquals("sos-1", dao.getMessageById("sos-1")?.messageId)
    }
}
