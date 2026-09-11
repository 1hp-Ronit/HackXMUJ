package com.pulsenet.app.data.local

import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.data.local.entity.MessageEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Seeds a realistic disaster-scenario dataset around the MUJ campus so the mesh
 * map and rescue dashboard aren't empty on first launch. Runs once (gated by a
 * DataStore flag) and only if the vault is actually empty, so it never
 * overwrites real mesh traffic. Seed messages carry a placeholder signature —
 * they're written directly to Room, bypassing GossipEngine's verify step
 * entirely, so there's no need to sign them with a real keypair.
 */
@Singleton
class DemoSeeder @Inject constructor(
    private val messageDao: MessageDao,
    private val userPreferences: UserPreferences
) {
    private companion object {
        const val SEED_SIGNATURE = "seed-data-unsigned"
        const val BASE_LAT = 26.8420 // MUJ campus, Jaipur
        const val BASE_LNG = 75.5650
    }

    suspend fun seedIfNeeded() {
        if (userPreferences.isDemoDataSeeded()) return
        if (messageDao.getMessageCount() > 0) {
            userPreferences.setDemoDataSeeded(true)
            return
        }
        messageDao.insertMessages(buildDemoMessages())
        userPreferences.setDemoDataSeeded(true)
    }

    private fun buildDemoMessages(): List<MessageEntity> {
        val random = Random(42)
        val now = System.currentTimeMillis()

        data class Template(val content: String, val priority: Int, val alias: String)

        // Kept small and deliberately weighted toward MEDIUM/GENERAL: a real feed
        // is mostly check-ins and resource requests, with SOS the rare exception,
        // not the norm — a wall of SOS pins reads as fake and undercuts the demo.
        val templates = listOf(
            Template("Trapped under debris near hostel block C, please send help", 0, "Ananya"),
            Template("मुझे मदद चाहिए, मैं इमारत के नीचे फंस गया हूं", 0, "Rohit"),
            Template("Heavy bleeding from leg injury, need ambulance urgently", 0, "Priya"),
            Template("Broken arm, in a lot of pain, near library building", 1, "Vikram"),
            Template("घायल हूं लेकिन ज्यादा गंभीर नहीं, help chahiye", 1, "Arjun"),
            Template("We have shelter space for 10 people near Block D", 2, "Divya"),
            Template("Need clean drinking water, ran out 2 hours ago", 2, "Rahul"),
            Template("Khana aur pani chahiye, 5 log hain humare group mein", 2, "Farhan"),
            Template("Elderly couple needs shelter, currently in open ground", 2, "Tanvi"),
            Template("All good here, checking in with the mesh", 3, "Dev"),
            Template("Network is down but we're safe at the sports complex", 3, "Ritu"),
            Template("Anyone else near the north gate? Trying to regroup", 3, "Yash"),
            Template("Roads near the market are blocked, avoid that route", 3, "Nikhil"),
            Template("Safe and sound, staying with family at the college ground", 3, "Aisha"),
            Template("Mobile network partially restored near admin block", 3, "Sneha")
        )

        return templates.mapIndexed { index, template ->
            val hopCount = random.nextInt(0, 7)
            val latOffset = (random.nextDouble() - 0.5) * 0.03
            val lngOffset = (random.nextDouble() - 0.5) * 0.03
            val createdAt = now - random.nextLong(0, 6 * 60 * 60 * 1000L)

            MessageEntity(
                messageId = "seed-$index-${random.nextInt(100_000, 999_999)}",
                senderPublicKey = "seed-public-key-$index",
                senderAlias = template.alias,
                content = template.content,
                latitude = BASE_LAT + latOffset,
                longitude = BASE_LNG + lngOffset,
                priority = template.priority,
                hopCount = hopCount,
                signature = SEED_SIGNATURE,
                createdAtEpochMs = createdAt,
                receivedAtEpochMs = createdAt,
                isSynced = random.nextBoolean(),
                isOwnMessage = false
            )
        }
    }
}
