package com.pulsenet.app.mesh

import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

/** In-memory MessageDao mirroring Room's real dedup/eviction semantics, for JVM tests. */
class FakeMessageDao : MessageDao {

    private val messages = LinkedHashMap<String, MessageEntity>()
    private val allFlow = MutableStateFlow<List<MessageEntity>>(emptyList())

    private fun publish() {
        allFlow.value = messages.values.sortedWith(
            compareBy<MessageEntity> { it.priority }.thenByDescending { it.createdAtEpochMs }
        )
    }

    override suspend fun insertMessage(message: MessageEntity): Long {
        if (messages.containsKey(message.messageId)) return -1
        messages[message.messageId] = message
        publish()
        return messages.size.toLong()
    }

    override suspend fun insertMessages(messages: List<MessageEntity>): List<Long> =
        messages.map { insertMessage(it) }

    override suspend fun getAllMessageIds(): List<String> = messages.keys.toList()

    override suspend fun getMessagesByIds(ids: List<String>): List<MessageEntity> =
        ids.mapNotNull { messages[it] }

    override suspend fun getMessageById(id: String): MessageEntity? = messages[id]

    override suspend fun getUnsyncedMessages(): List<MessageEntity> =
        messages.values.filter { !it.isSynced }
            .sortedWith(compareBy<MessageEntity> { it.priority }.thenBy { it.createdAtEpochMs })

    override suspend fun markSynced(ids: List<String>) {
        ids.forEach { id -> messages[id]?.let { messages[id] = it.copy(isSynced = true) } }
        publish()
    }

    override suspend fun evictOldMessages(count: Int) {
        messages.values
            .filter { it.priority > 0 && it.isSynced }
            .sortedWith(compareByDescending<MessageEntity> { it.priority }.thenBy { it.createdAtEpochMs })
            .take(count)
            .forEach { messages.remove(it.messageId) }
        publish()
    }

    override suspend fun getMessageCount(): Int = messages.size

    override fun observeAllMessages(): Flow<List<MessageEntity>> = allFlow

    override fun observeSOSMessages(): Flow<List<MessageEntity>> =
        MutableStateFlow(allFlow.value.filter { it.priority == 0 })
}
