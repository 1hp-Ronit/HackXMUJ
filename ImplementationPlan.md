# PulseNet — Asynchronous Survival Mesh Network

> **Hackathon:** MUJ HackX 4.0 (36-hour sprint — **live now**)
> **Language:** Kotlin · **Min SDK:** 26 · **Target SDK:** 34 · **Architecture:** MVVM + Clean Architecture

## Context & Problem

Existing offline mesh apps fail in real disasters because continuous BLE/Wi-Fi scanning triggers Android Doze Mode and kills the battery within hours. PulseNet solves this with an **asynchronous, sensor-triggered mesh protocol** — the app sleeps like a submarine and only wakes radios under disciplined conditions or when hardware sensors detect distress.

---

## Critical Technical Notes

> [!WARNING]
> **Nearby Connections does NOT provide multi-hop routing out of the box.** The P2P_CLUSTER strategy gives you M-to-N direct connections. You MUST implement application-layer gossip routing (see `GossipEngine.kt` in Component 4). Do NOT assume the API routes messages through intermediate nodes.

> [!WARNING]
> **MongoDB Realm / Atlas Device SDK is officially deprecated** (EOL September 2025). Use **Room + Retrofit/OkHttp** for local storage and direct Atlas Data API calls for cloud sync.

> [!CAUTION]
> **Ed25519 in Android Keystore** requires API 31+ and hardware support varies by device. You MUST implement the Bouncy Castle software fallback described in Component 6.

---

## Resolved Configuration

| Item | Status | Detail |
|---|---|---|
| **MongoDB Atlas** | ✅ User will provision | Free M0 cluster, `ap-south-1` Mumbai. Database: `pulsenet`, Collections: `messages`, `nodes` |
| **Sarvam AI** | ✅ API key available | Stored in `local.properties` as `SARVAM_API_KEY` |
| **Test Devices** | ✅ 4 physical Android devices | Full P2P_CLUSTER mesh testable (4-node cluster) |
| **Rescue Dashboard** | ✅ Web-based | Express + Leaflet.js, real-time heat map — included in Tier 2 |

---

## Agent Handoff Brief

> [!IMPORTANT]
> **This section contains everything a coding agent needs to build PulseNet from scratch.** Send this entire `implementation_plan.md` file to the agent. No additional context is required.

### Environment & Credentials

The agent must create a `local.properties` file (git-ignored) in the Android project root with:
```properties
# MongoDB Atlas Data API
ATLAS_APP_ID=<to-be-filled-after-cluster-setup>
ATLAS_API_KEY=<to-be-filled-after-cluster-setup>
ATLAS_BASE_URL=https://data.mongodb-api.com/app

# Sarvam AI Translation
SARVAM_API_KEY=sk_zri9h2km_ea1FeczlFbR67sJ1aFgjRo1U
SARVAM_BASE_URL=https://api.sarvam.ai

# MongoDB Connection String (for backend/dashboard server)
MONGO_URI=<to-be-filled-after-cluster-setup>
```

These values should be read in `build.gradle.kts` via:
```kotlin
val localProps = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}
android {
    defaultConfig {
        buildConfigField("String", "ATLAS_APP_ID", "\"${localProps["ATLAS_APP_ID"]}\"")
        buildConfigField("String", "ATLAS_API_KEY", "\"${localProps["ATLAS_API_KEY"]}\"")
        buildConfigField("String", "SARVAM_API_KEY", "\"${localProps["SARVAM_API_KEY"]}\"")
    }
}
```

### MongoDB Atlas Setup Checklist (User will do this)

1. Create free M0 cluster on `ap-south-1` (Mumbai)
2. Database: `pulsenet`
3. Collections: `messages`, `nodes`
4. Indexes to create:
   ```javascript
   db.messages.createIndex({ "location": "2dsphere" })
   db.messages.createIndex({ "priority": 1, "createdAt": -1 })
   db.messages.createIndex({ "messageId": 1 }, { unique: true })
   ```
5. Enable Data API → generate App ID + API Key
6. Network access: `0.0.0.0/0` (hackathon only)
7. DB user: `pulsenet-app` with `readWrite` on `pulsenet`

### What the Agent Must Build

The project has **2 deliverables** built in **2 tiers**:

1. **Android App** (`app/`) — Native Kotlin, Jetpack Compose, MVVM+Hilt
2. **Rescue Dashboard** (`backend/`) — Node.js/Express + static HTML with Leaflet.js

### Build Order (Dependency Chain)

```
Room DB (Entity/DAO) → KeyManager/MessageSigner → NearbyMeshManager → GossipEngine
    → MeshService → DistressSensorManager → BridgeManager → UI Screens
    → BridgeFlushWorker → Backend Server → Dashboard → Sarvam Integration
```

### Package Name & App ID

- **Package:** `com.pulsenet.app`
- **Application ID:** `com.pulsenet.app`
- **Nearby Connections Service ID:** `com.pulsenet.mesh`

---

## Proposed Changes

### Project Structure

```
hackMUJ/
├── app/
│   ├── src/main/
│   │   ├── java/com/pulsenet/
│   │   │   ├── PulseNetApp.kt                 # Application class
│   │   │   ├── di/                             # Hilt DI modules
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   └── NetworkModule.kt
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── PulseDatabase.kt        # Room database
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── MessageDao.kt
│   │   │   │   │   │   └── PeerDao.kt
│   │   │   │   │   └── entity/
│   │   │   │   │       ├── MessageEntity.kt
│   │   │   │   │       └── PeerEntity.kt
│   │   │   │   ├── remote/
│   │   │   │   │   ├── AtlasApiService.kt      # MongoDB Data API
│   │   │   │   │   └── SarvamApiService.kt     # Sarvam AI translation
│   │   │   │   └── repository/
│   │   │   │       ├── MessageRepository.kt
│   │   │   │       └── SyncRepository.kt
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Message.kt              # Domain model
│   │   │   │   │   ├── Priority.kt             # SOS priority levels
│   │   │   │   │   └── PeerNode.kt
│   │   │   │   └── usecase/
│   │   │   │       ├── SendMessageUseCase.kt
│   │   │   │       ├── SyncMessagesUseCase.kt
│   │   │   │       └── TriggerSOSUseCase.kt
│   │   │   ├── mesh/
│   │   │   │   ├── MeshService.kt              # Foreground service (core)
│   │   │   │   ├── NearbyMeshManager.kt        # Nearby Connections wrapper
│   │   │   │   ├── GossipEngine.kt             # Anti-entropy sync protocol
│   │   │   │   └── BridgeManager.kt            # Cellular backhaul logic
│   │   │   ├── sensor/
│   │   │   │   ├── DistressSensorManager.kt    # Tap-cadence SOS detector
│   │   │   │   └── ShakePatternDetector.kt     # Signal processing
│   │   │   ├── security/
│   │   │   │   ├── KeyManager.kt               # Ed25519 key generation
│   │   │   │   └── MessageSigner.kt            # Sign/verify payloads
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   ├── onboarding/
│   │   │   │   │   ├── OnboardingScreen.kt     # Emergency permissions
│   │   │   │   │   └── OnboardingViewModel.kt
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt           # Main pulse dashboard
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── sos/
│   │   │   │   │   ├── SOSScreen.kt            # SOS trigger + status
│   │   │   │   │   └── SOSViewModel.kt
│   │   │   │   ├── messages/
│   │   │   │   │   ├── MessagesScreen.kt       # Message feed
│   │   │   │   │   └── MessagesViewModel.kt
│   │   │   │   ├── map/
│   │   │   │   │   ├── MeshMapScreen.kt        # Peer/SOS map view
│   │   │   │   │   └── MeshMapViewModel.kt
│   │   │   │   └── components/
│   │   │   │       ├── PulseRadar.kt           # Animated radar widget
│   │   │   │       ├── SOSButton.kt            # Giant panic button
│   │   │   │       └── MessageCard.kt
│   │   │   └── worker/
│   │   │       ├── BridgeFlushWorker.kt        # WorkManager cloud sync
│   │   │       └── EvictionWorker.kt           # Storage triage cleanup
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── backend/                                     # Tier 2: Rescue dashboard
│   ├── package.json
│   ├── server.js                                # Express + MongoDB + WebSocket
│   ├── .env                                     # MONGO_URI, PORT
│   └── public/
│       ├── index.html                           # Rescue command dashboard
│       ├── style.css                            # Dashboard styles
│       └── app.js                               # Leaflet map + real-time updates
├── build.gradle.kts                             # Root build file
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

### Tier 1 — MVP (Hours 0–20): Core Mesh + SOS + Local Storage

This tier delivers a **fully functional demo**: sensor-triggered SOS, peer discovery, gossip sync, and local message storage.

---

#### Component 1: Android Project Scaffold

##### [NEW] Root build files & Gradle configuration

- Initialize an Android project with Kotlin DSL (`build.gradle.kts`)
- **Min SDK 26**, Target SDK 34, Compose BOM for Jetpack Compose UI
- Dependencies:
  | Library | Purpose |
  |---|---|
  | `androidx.room:room-*:2.6.1` | Local encrypted SQLite vault |
  | `com.google.android.gms:play-services-nearby:19.3.0` | Nearby Connections mesh |
  | `com.google.android.gms:play-services-location:21.3.0` | FusedLocationProvider GPS |
  | `androidx.work:work-runtime-ktx:2.9.1` | Battery-disciplined background jobs |
  | `com.google.dagger:hilt-android:2.51.1` | Dependency injection |
  | `androidx.compose.*` | Modern declarative UI |
  | `org.bouncycastle:bcprov-jdk18on:1.78` | Ed25519 crypto fallback |
  | `com.squareup.retrofit2:retrofit:2.11.0` | Cloud API calls |
  | `com.squareup.moshi:moshi-kotlin:1.15.1` | JSON serialization |

##### [NEW] `AndroidManifest.xml`

Key permissions and declarations:
```xml
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS"/>

<service
    android:name=".mesh.MeshService"
    android:foregroundServiceType="connectedDevice"
    android:exported="false"/>
```

---

#### Component 2: Data Layer — Room Database (The Local Vault)

##### [NEW] `MessageEntity.kt`

```kotlin
@Entity(
    tableName = "messages",
    indices = [Index(value = ["messageId"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,       // UUID v4
    val senderPublicKey: String,             // Base64-encoded Ed25519 pubkey
    val senderAlias: String,                 // User-chosen name
    val content: String,                     // Message body (encrypted at rest)
    val latitude: Double,                    // GPS fix at creation
    val longitude: Double,
    val priority: Int,                       // 0=SOS, 1=Medical, 2=Resource, 3=General
    val hopCount: Int,                       // Current hop count
    val maxHops: Int = 7,                    // TTL — message dies after 7 hops
    val signature: String,                   // Ed25519 signature (Base64)
    val createdAtEpochMs: Long,              // Originator timestamp
    val receivedAtEpochMs: Long,             // When THIS device received it
    val isSynced: Boolean = false,           // Has been flushed to cloud?
    val isOwnMessage: Boolean = false        // Did this device originate it?
)
```

##### [NEW] `MessageDao.kt`

Key queries:
```kotlin
@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)  // Dedup by UUID
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT messageId FROM messages")
    suspend fun getAllMessageIds(): List<String>       // For anti-entropy hash exchange

    @Query("SELECT * FROM messages WHERE messageId IN (:ids)")
    suspend fun getMessagesByIds(ids: List<String>): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE isSynced = 0 ORDER BY priority ASC, createdAtEpochMs ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Query("UPDATE messages SET isSynced = 1 WHERE messageId IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    // Triage eviction: delete lowest-priority, oldest messages first
    // NEVER evict PRIORITY_0 (SOS) messages
    @Query("""
        DELETE FROM messages WHERE messageId IN (
            SELECT messageId FROM messages
            WHERE priority > 0 AND isSynced = 1
            ORDER BY priority DESC, createdAtEpochMs ASC
            LIMIT :count
        )
    """)
    suspend fun evictOldMessages(count: Int)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    @Query("SELECT * FROM messages ORDER BY priority ASC, createdAtEpochMs DESC")
    fun observeAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE priority = 0 ORDER BY createdAtEpochMs DESC")
    fun observeSOSMessages(): Flow<List<MessageEntity>>
}
```

---

#### Component 3: Sensor-Triggered SOS (The "Trapped Under Rubble" Feature)

##### [NEW] `DistressSensorManager.kt`

**Algorithm:** Detect 5 sharp Z-axis spikes within a 3-second window, each exceeding 15 m/s² magnitude. This is a deliberate, violent tap pattern that won't false-trigger from walking or pocket movement.

```
Implementation details:
1. Register SensorEventListener for TYPE_LINEAR_ACCELERATION
   at SENSOR_DELAY_GAME (20ms sample rate)
2. Maintain a circular buffer of the last 5 spike timestamps
3. When magnitude > THRESHOLD (15.0 m/s²):
   - Record timestamp
   - If 5 spikes within 3000ms window → TRIGGER SOS
4. On trigger:
   - Acquire partial WakeLock (10s timeout)
   - Request GPS fix via FusedLocationProviderClient (high accuracy, 5s timeout)
   - Create PRIORITY_0 MessageEntity with GPS coordinates
   - Wake MeshService to broadcast immediately
   - Trigger full-screen SOS Activity (bypasses lock screen via
     FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON)
5. Debounce: 30-second cooldown after trigger to prevent spam
```

> [!NOTE]
> The sensor listener runs inside the MeshService foreground service, so it survives Doze Mode. Power draw is negligible (~2mA for accelerometer alone).

---

#### Component 4: Mesh Networking — Nearby Connections

##### [NEW] `NearbyMeshManager.kt`

```
Core flow:
1. startAdvertising(serviceId = "com.pulsenet.mesh", strategy = Strategy.P2P_CLUSTER)
2. startDiscovery(serviceId = "com.pulsenet.mesh", strategy = Strategy.P2P_CLUSTER)
3. ConnectionLifecycleCallback:
   - onConnectionInitiated → auto-accept (disaster scenario, no pairing friction)
   - onConnectionResult(SUCCESS) → trigger GossipEngine.syncWith(endpointId)
   - onDisconnected → remove from active peers
4. PayloadCallback:
   - onPayloadReceived → deserialize → verify signature → insert to Room
   - onPayloadTransferUpdate → track progress for large payloads
```

**Duty-cycling strategy** (battery discipline):
```
- Advertise continuously (BLE advertising is ~0.5mA, negligible)
- Discovery: Run for 30s every 5 minutes via AlarmManager.setExactAndAllowWhileIdle()
  (falls back to 15-min WorkManager periodic if exact alarms restricted)
- Exception: When SOS is active, discovery runs continuously until
  at least one peer connection succeeds
```

##### [NEW] `GossipEngine.kt`

Anti-entropy synchronization protocol:
```
When two peers connect:
1. PHASE 1 — Hash Exchange
   - Device A sends: { type: "HASH_LIST", hashes: [messageId1, messageId2, ...] }
   - Device B computes delta: missingOnA = B.hashes - A.hashes
   - Device B sends: { type: "HASH_LIST", hashes: [...] }
   - Device A computes delta: missingOnB = A.hashes - B.hashes

2. PHASE 2 — Delta Transfer
   - Each device sends the messages the other is missing
   - Messages with hopCount >= maxHops are NOT forwarded (TTL enforcement)
   - hopCount is incremented by 1 on each forward

3. PHASE 3 — Priority Sorting
   - PRIORITY_0 (SOS) messages are transferred FIRST
   - Remaining messages sorted by timestamp (newest first)

4. Connection teardown after sync completes
```

---

#### Component 5: Foreground Service (The Heartbeat)

##### [NEW] `MeshService.kt`

```kotlin
class MeshService : Service() {
    // Foreground service with CONNECTED_DEVICE type
    // Runs with a persistent LOW_PRIORITY notification showing:
    //   "PulseNet Active • X peers nearby • Y messages cached"
    //
    // Responsibilities:
    // 1. Host DistressSensorManager (always listening)
    // 2. Host NearbyMeshManager (duty-cycled discovery)
    // 3. Host ConnectivityMonitor (network callback for Bridge Mode)
    // 4. Update notification with live peer/message counts
}
```

---

#### Component 6: Security — Ed25519 Message Signing

##### [NEW] `KeyManager.kt`

```
Strategy (with device fragmentation fallback):
1. Try: Generate Ed25519 keypair in Android Keystore (API 31+)
   - KeyPairGenerator("Ed25519", "AndroidKeyStore")
   - PURPOSE_SIGN | PURPOSE_VERIFY
2. Catch: If StrongBoxUnavailableException or unsupported:
   - Generate Ed25519 keypair via Bouncy Castle (software)
   - Generate AES-256-GCM key in Android Keystore
   - Encrypt the Ed25519 private key with the AES key
   - Store encrypted privkey in SharedPreferences
3. Public key is embedded in every outgoing message
4. On first launch, generate keypair and persist forever
```

##### [NEW] `MessageSigner.kt`

```
signMessage(message) → Base64(Ed25519.sign(message.content + message.latitude + ...))
verifyMessage(message) → Ed25519.verify(message.signature, message.senderPublicKey, payload)

On receive: if !verifyMessage(msg) → DROP packet, log tampering attempt
```

---

#### Component 7: UI — Jetpack Compose (Panic-Proof Design)

> [!IMPORTANT]
> **Design philosophy:** In a disaster, the user may be injured, panicking, or in darkness. The UI must be **aggressively simple** — giant touch targets, high-contrast colors, zero navigation complexity.

##### [NEW] `OnboardingScreen.kt`
- Single-screen emergency permission flow
- Requests all permissions in sequence with clear disaster-context explanations
- Option to disable battery optimization (with explanation of why)
- User sets a name/alias (no account creation)

##### [NEW] `HomeScreen.kt` — The Pulse Dashboard
```
Layout (full-screen, 3 zones):
┌─────────────────────────────────┐
│     MESH STATUS BAR             │  ← "3 peers • 47 messages • ⚡ 78%"
├─────────────────────────────────┤
│                                 │
│     ANIMATED PULSE RADAR        │  ← Circular radar showing nearby
│     (concentric rings +         │     peers as dots with distance
│      peer dots animating)       │
│                                 │
├─────────────────────────────────┤
│   ┌─────────┐  ┌─────────┐     │
│   │  SOS 🔴  │  │ MESSAGE │     │  ← Two giant buttons, min 80dp
│   │  BUTTON  │  │  FEED   │     │     touch target
│   └─────────┘  └─────────┘     │
└─────────────────────────────────┘

Theme:
- Dark background (#0A0A0F) — saves OLED battery
- Accent: Emergency red (#FF2D2D) for SOS, Pulse blue (#00D4FF) for mesh
- Font: Inter/Roboto, large sizes (18sp body, 24sp headers)
- All interactive elements: minimum 56dp touch target
```

##### [NEW] `SOSScreen.kt`
- **Giant red button** (fills 60% of screen)
- Single tap → confirm dialog ("Are you in danger?")
- Long press (2s) → immediate SOS, no confirmation
- Shows GPS coordinates being acquired
- Vibration feedback + screen flash on SOS sent
- Instruction text: "If you can't use the screen, tap the back of your phone 5 times rapidly"

##### [NEW] `MessagesScreen.kt`
- Scrollable feed of received messages
- Color-coded by priority (red=SOS, orange=medical, yellow=resource, gray=general)
- Compose new message with priority selector (large icon buttons, not dropdowns)
- Shows hop count and originator distance

---

### Tier 2 — Polish (Hours 20–30): Bridge Mode + Cloud Sync + Map + Dashboard + Sarvam AI

---

#### Component 8: Bridge Mode — Cellular Backhaul

##### [NEW] `BridgeManager.kt`

```kotlin
// Register ConnectivityManager.NetworkCallback
// When onAvailable() fires with NET_CAPABILITY_INTERNET:
//   1. Enqueue OneTimeWorkRequest for BridgeFlushWorker
//   2. Show notification: "📡 Network detected! Syncing community data..."
//   3. WorkManager constraint: NetworkType.CONNECTED
```

##### [NEW] `BridgeFlushWorker.kt`

```
WorkManager Worker:
1. Query Room: SELECT * FROM messages WHERE isSynced = 0
2. Batch into chunks of 50 messages
3. For each batch:
   - Serialize to JSON array
   - POST to MongoDB Atlas Data API endpoint:
     POST https://data.mongodb-api.com/app/{ATLAS_APP_ID}/endpoint/data/v1/action/insertMany
     Headers:
       Content-Type: application/json
       api-key: {ATLAS_API_KEY}
     Body: {
       "collection": "messages",
       "database": "pulsenet",
       "dataSource": "PulseNet",
       "documents": [
         {
           "messageId": "uuid-here",
           "senderAlias": "Ronit",
           "content": "Need water urgently",
           "location": { "type": "Point", "coordinates": [75.7873, 26.9124] },
           "priority": 2,
           "hopCount": 3,
           "signature": "base64...",
           "senderPublicKey": "base64...",
           "createdAt": "2026-09-11T16:00:00Z",
           "translatedContent": null,
           "severityTag": null
         }
       ]
     }
   - On 200 OK → markSynced(batch.messageIds)
4. On failure → Result.retry() (WorkManager exponential backoff)
5. Show progress in notification
```

> [!IMPORTANT]
> The `location` field MUST use GeoJSON format `{ type: "Point", coordinates: [longitude, latitude] }` (note: **longitude first**) for the 2dsphere index to work.

##### [NEW] `EvictionWorker.kt`

```
Periodic WorkManager (every 6 hours):
1. Check Room DB size (approximate via message count × avg size)
2. If > 50MB threshold:
   - Run eviction query (delete synced general messages first)
   - NEVER delete PRIORITY_0 messages that haven't been synced
3. Log eviction stats
```

---

#### Component 9: Map View

##### [NEW] `MeshMapScreen.kt`

- Uses OSMDroid (OpenStreetMap) — works offline with cached tiles
- Add dependency: `org.osmdroid:osmdroid-android:6.1.18`
- Wrap in `AndroidView` Composable for Compose interop
- Plots all cached messages as pins, color-coded by priority
- SOS messages shown as pulsing red circles (custom Drawable overlay)
- Current device location shown as blue dot
- Nearby peers shown as green dots (from Nearby Connections endpoint info)
- Cluster markers when zoomed out
- Default zoom: 15 (neighborhood level)
- Offline tile caching: pre-cache India tiles for demo

---

#### Component 10: Sarvam AI Translation & Severity Tagging

##### [NEW] `SarvamApiService.kt`

```kotlin
// Retrofit interface for Sarvam AI
interface SarvamApiService {

    @POST("translate")
    suspend fun translate(
        @Header("api-subscription-key") apiKey: String,
        @Body request: TranslateRequest
    ): TranslateResponse
}

// Request model
data class TranslateRequest(
    val input: String,
    val source_language_code: String = "auto",  // auto-detect
    val target_language_code: String = "en",
    val mode: String = "code-mixed"  // handles Hinglish/Tanglish naturally
)

// Response model
data class TranslateResponse(
    val translated_text: String
)
```

**Integration point:** Inside `BridgeFlushWorker`, before uploading each batch to MongoDB:
```
for each message in batch:
  if message appears non-English (heuristic: contains non-ASCII or mixed script):
    translatedContent = sarvamApi.translate(message.content).translated_text
    severityTag = classifySeverity(translatedContent)
  else:
    translatedContent = message.content
    severityTag = classifySeverity(message.content)

  // Attach translatedContent + severityTag to the MongoDB document
```

**Severity classification** (keyword-based, no ML needed):
```kotlin
fun classifySeverity(text: String): String {
    val lower = text.lowercase()
    return when {
        lower.containsAny("trapped", "bleeding", "drowning", "fire", "dying",
            "crushed", "buried", "unconscious", "heart attack") -> "CRITICAL"
        lower.containsAny("injured", "broken", "medical", "pain", "help",
            "hurt", "wound", "fracture", "ambulance") -> "HIGH"
        lower.containsAny("food", "water", "shelter", "medicine",
            "blanket", "clothes", "baby", "child", "elderly") -> "MEDIUM"
        else -> "LOW"
    }
}
```

**Sarvam API config:**
- Base URL: `https://api.sarvam.ai/`
- Auth header: `api-subscription-key: {SARVAM_API_KEY}`
- Supports 22 Indian languages + Hinglish/code-mixed input
- Rate limit: be conservative, add 100ms delay between calls

---

#### Component 11: Rescue Command Dashboard (Web)

##### [NEW] `backend/package.json`

```json
{
  "name": "pulsenet-dashboard",
  "version": "1.0.0",
  "scripts": { "start": "node server.js" },
  "dependencies": {
    "express": "^4.21.0",
    "mongodb": "^6.9.0",
    "ws": "^8.18.0",
    "dotenv": "^16.4.5",
    "cors": "^2.8.5"
  }
}
```

##### [NEW] `backend/.env`

```env
MONGO_URI=<to-be-filled>
PORT=3000
```

##### [NEW] `backend/server.js`

```javascript
// Express + MongoDB + WebSocket server
//
// REST Endpoints:
//   POST /api/messages/bulk     → bulk insert messages (called by Android Bridge)
//   GET  /api/messages          → all messages, paginated, filterable by priority
//   GET  /api/messages/sos      → only PRIORITY_0 messages
//   GET  /api/stats             → { totalMessages, activeSOS, uniqueNodes, coverageAreaKm2 }
//   GET  /api/heatmap           → aggregation pipeline: $geoNear clusters for heat map
//
// WebSocket:
//   On new message insert → broadcast to all connected dashboard clients
//   Use MongoDB Change Streams (db.messages.watch()) to detect inserts
//
// MongoDB indexes (ensure on startup):
//   db.messages.createIndex({ "location": "2dsphere" })
//   db.messages.createIndex({ "priority": 1, "createdAt": -1 })
//   db.messages.createIndex({ "messageId": 1 }, { unique: true })
//
// Serve static files from ./public/
```

##### [NEW] `backend/public/index.html` — Rescue Dashboard UI

```
Full-page dark-themed command center dashboard:

┌────────────────────────────────────────────────────────────┐
│  PULSENET RESCUE COMMAND CENTER          [Live] [●] 47 msgs│
├──────────────────────────────────┬─────────────────────────┤
│                                  │  STATISTICS             │
│   LEAFLET MAP (full height)      │  ┌─────────────────┐   │
│   - Tile: CartoDB Dark Matter    │  │ Total Messages   │   │
│   - Heat map layer (leaflet.heat)│  │ Active SOS: 3    │   │
│   - SOS: pulsing red markers     │  │ Nodes seen: 12   │   │
│   - Medical: orange markers      │  │ Coverage: 4.2km² │   │
│   - Resource: yellow markers     │  └─────────────────┘   │
│   - General: gray markers        │                         │
│   - Click marker → popup with:   │  LIVE SOS FEED          │
│     sender, content, translated, │  ┌─────────────────┐   │
│     severity tag, hop count,     │  │ 🔴 "trapped under│   │
│     timestamp, coordinates       │  │    building..."  │   │
│                                  │  │ 🔴 "need medical │   │
│                                  │  │    help urgentl..│   │
│                                  │  └─────────────────┘   │
├──────────────────────────────────┴─────────────────────────┤
│  MESSAGE TICKER (horizontal scroll of recent messages)     │
└────────────────────────────────────────────────────────────┘

Libraries (CDN, no build step):
- Leaflet.js 1.9.4
- leaflet.heat plugin (for heat map layer)
- Chart.js 4.x (for stats donut/bar if time permits)
- Native WebSocket for real-time updates

Design:
- Background: #0a0a0f (matches Android app theme)
- Accent: #FF2D2D (SOS red), #00D4FF (pulse blue)
- Font: Inter (Google Fonts)
- Glassmorphism cards for stats sidebar
- Pulsing CSS animation on SOS markers
```

---

### Tier 3 — Stretch Goals (Hours 30–36): Audio + Ultrasonic + Demo Polish

---

#### Component 12: Voice Note Support

- Record short voice notes (max 15s) via MediaRecorder
- Compress to Opus codec
- Transmit via Nearby Connections STREAM payload type
- Playback in message feed with waveform visualization
- Add `android.permission.RECORD_AUDIO` to manifest

#### Component 13: Ultrasonic Audio Fallback (Quiet)

- If Bluetooth stack is unavailable, transmit GPS coordinates via ultrasonic chirps
- Use Quiet library (`com.quiet:quiet-android`) for modulation/demodulation
- Range: ~3 meters, ~100 bits/second (enough for lat/lng + priority)
- Fallback trigger: if Nearby Connections `startAdvertising()` fails with `STATUS_BLUETOOTH_ERROR`

#### Component 14: Demo Polish

- Seed Room DB with 50+ realistic demo messages (various priorities, locations around Jaipur)
- Pre-record a 3-minute demo video showing: onboarding → SOS trigger → mesh sync → bridge flush → dashboard update
- Add app icon and splash screen
- README.md with architecture diagram, setup instructions, screenshots

---

## Implementation Timeline (Hackathon Sprint)

| Block | Hours | Deliverable | Tier |
|---|---|---|---|
| **Sprint 1** | 0–3 | Project scaffold, Gradle setup, Room DB, Entity/DAO | MVP |
| **Sprint 2** | 3–6 | KeyManager + MessageSigner (Ed25519 crypto) | MVP |
| **Sprint 3** | 6–10 | NearbyMeshManager + GossipEngine (peer sync) | MVP |
| **Sprint 4** | 10–13 | MeshService foreground service + DistressSensorManager | MVP |
| **Sprint 5** | 13–17 | UI: Onboarding → Home → SOS → Messages (Compose) | MVP |
| **Sprint 6** | 17–20 | **Integration testing on physical devices** → **MVP DONE** | MVP |
| **Sprint 7** | 20–24 | BridgeManager + BridgeFlushWorker + Atlas Data API sync | Tier 2 |
| **Sprint 8** | 24–27 | Sarvam AI translation + severity tagging in BridgeFlushWorker | Tier 2 |
| **Sprint 9** | 27–30 | Map view (OSMDroid) + Rescue dashboard (Express + Leaflet) + EvictionWorker | Tier 2 |
| **Sprint 10** | 30–33 | Voice notes + ultrasonic fallback (stretch) | Tier 3 |
| **Sprint 11** | 33–36 | Demo polish, seed data, pitch prep, video recording | Tier 3 |

---

## Verification Plan

### Automated Tests

```bash
# Unit tests (can run in CI/emulator)
./gradlew testDebugUnitTest

# Instrumented tests (requires device)
./gradlew connectedDebugAndroidTest
```

| Test | What it validates |
|---|---|
| `MessageDaoTest` | Room insert, dedup (IGNORE strategy), eviction query, sync marking |
| `GossipEngineTest` | Hash delta computation, TTL enforcement, priority sorting |
| `MessageSignerTest` | Ed25519 sign/verify round-trip, tampered message rejection |
| `ShakePatternDetectorTest` | 5-tap cadence detection, false positive rejection, cooldown |
| `BridgeFlushWorkerTest` | Serialization, batch chunking, retry on failure |

### Manual Verification

1. **Two-device mesh test:**
   - Install on 2 physical Android phones
   - Enable airplane mode on both
   - Send message from Device A → verify it appears on Device B via BLE
   - Verify hop count increments correctly

2. **SOS trigger test:**
   - Lock phone, tap back 5 times rapidly
   - Verify SOS screen appears, GPS fix acquired, message broadcast

3. **Bridge mode test:**
   - Accumulate messages offline on Device A
   - Turn on cellular data on Device A
   - Verify all cached messages flush to MongoDB Atlas
   - Verify rescue dashboard updates in real-time

4. **Battery discipline test:**
   - Run app in background for 1 hour with screen off
   - Monitor battery drain via `adb shell dumpsys batterystats`
   - Target: < 3% battery per hour in standby

5. **Tampering test:**
   - Manually modify a message payload in transit
   - Verify receiving device drops it (signature verification failure)

---

## Key Technical Decisions

| Decision | Rationale |
|---|---|
| **Room over Realm** | Realm SDK deprecated Sep 2025. Room is Jetpack-standard, actively maintained, and has better coroutine/Flow support |
| **Bouncy Castle fallback** | Ed25519 hardware support varies wildly across devices. Software fallback ensures crypto works on all API 26+ devices |
| **OSMDroid over Google Maps** | Works fully offline with cached tiles — critical for disaster scenarios |
| **Moshi over Gson** | Kotlin-first, null-safe, faster reflection-free codegen |
| **MVVM + Hilt** | Industry standard for hackathon judges, clean separation, testable |
| **Strategy.P2P_CLUSTER** | Only Nearby Connections strategy that supports M-to-N topology needed for mesh |
| **Duty-cycled discovery** | 30s scan every 5 min instead of continuous — saves ~80% radio power |
| **Atlas Data API over Realm Sync** | Direct REST calls, no deprecated SDK dependency, works with any HTTP client |
