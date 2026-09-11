# PulseNet — Build Phases

> Derived from [ImplementationPlan.md](file:///run/media/ronit/New%20Volume/Prgm_Phases/Projects/hackMUJ/ImplementationPlan.md)
> Each phase is self-contained with clear inputs, outputs, files to create, and a "done when" checklist.

---

## Phase 0: Project Scaffold (Hours 0–1.5)

**Goal:** Bootable Android project with all dependencies, Gradle config, and manifest.

### What to do

1. Initialize a new Android project in the `hackMUJ/` workspace root:
   - **Package name:** `com.pulsenet.app`
   - **Min SDK:** 26, **Target SDK:** 34, **Compile SDK:** 34
   - Kotlin DSL (`build.gradle.kts`)
   - Jetpack Compose enabled

2. Configure root `build.gradle.kts`:
   - Kotlin `1.9.x`, AGP `8.x`
   - Hilt plugin (`com.google.dagger.hilt.android`)
   - KSP plugin for Room annotation processing

3. Configure app-level `build.gradle.kts` with all dependencies:
   ```
   Room 2.6.1, Nearby Connections 19.3.0, Location 21.3.0,
   WorkManager 2.9.1, Hilt 2.51.1, Compose BOM,
   Bouncy Castle 1.78, Retrofit 2.11.0, Moshi 1.15.1,
   OSMDroid 6.1.18
   ```
   - Read `local.properties` for `ATLAS_APP_ID`, `ATLAS_API_KEY`, `SARVAM_API_KEY`
   - Expose via `BuildConfig` fields

4. Create `local.properties` with placeholder values (git-ignored)

5. Set up `AndroidManifest.xml` with all permissions:
   - Bluetooth (ADVERTISE, CONNECT, SCAN)
   - Wi-Fi (ACCESS, CHANGE)
   - Location (FINE, COARSE, BACKGROUND)
   - Foreground Service (CONNECTED_DEVICE type)
   - Internet, Network State, Wake Lock, High Sampling Rate Sensors
   - Declare `MeshService` with `foregroundServiceType="connectedDevice"`

6. Create the `PulseNetApp.kt` Application class with `@HiltAndroidApp`

### Files to create

| File | Purpose |
|---|---|
| `build.gradle.kts` (root) | Plugins, Kotlin version |
| `settings.gradle.kts` | Module include, repository config |
| `gradle.properties` | JVM args, AndroidX opt-in |
| `app/build.gradle.kts` | All dependencies, BuildConfig |
| `local.properties` | Credential placeholders |
| `app/src/main/AndroidManifest.xml` | Permissions + service declarations |
| `app/src/main/java/com/pulsenet/app/PulseNetApp.kt` | Hilt application entry point |

### Done when

- [ ] `./gradlew assembleDebug` completes without errors
- [ ] `PulseNetApp` class compiles with `@HiltAndroidApp`
- [ ] All permissions declared in manifest

---

## Phase 1: Data Layer — Room Database (Hours 1.5–3)

**Goal:** Local encrypted vault with message storage, deduplication, eviction queries, and reactive observation.

### What to do

1. Create `MessageEntity.kt` — Room entity with:
   - `messageId` (UUID, PK), `senderPublicKey`, `senderAlias`, `content`
   - `latitude`, `longitude`, `priority` (0–3), `hopCount`, `maxHops` (default 7)
   - `signature`, `createdAtEpochMs`, `receivedAtEpochMs`
   - `isSynced` (Boolean), `isOwnMessage` (Boolean)
   - Unique index on `messageId`

2. Create `PeerEntity.kt` — Room entity with:
   - `endpointId` (PK), `alias`, `publicKey`
   - `lastSeenEpochMs`, `latitude`, `longitude`

3. Create `MessageDao.kt` with queries:
   - `insertMessage` (OnConflict.IGNORE for dedup)
   - `getAllMessageIds` (returns List<String> for gossip hash exchange)
   - `getMessagesByIds` (bulk fetch for delta transfer)
   - `getUnsyncedMessages` (ordered by priority ASC)
   - `markSynced` (batch update)
   - `evictOldMessages` (triage: never evict priority 0, evict synced general first)
   - `getMessageCount`
   - `observeAllMessages` (returns Flow)
   - `observeSOSMessages` (returns Flow, priority = 0 only)

4. Create `PeerDao.kt` with queries:
   - `insertOrUpdatePeer` (upsert)
   - `getAllPeers` (Flow)
   - `deleteStalePeers` (older than 24h)

5. Create `PulseDatabase.kt` — Room database class
   - Entities: `MessageEntity`, `PeerEntity`
   - Version 1, no migrations needed for hackathon

6. Create `DatabaseModule.kt` — Hilt module providing Room DB and DAOs

### Files to create

| File | Purpose |
|---|---|
| `data/local/entity/MessageEntity.kt` | Message table schema |
| `data/local/entity/PeerEntity.kt` | Peer node table schema |
| `data/local/dao/MessageDao.kt` | All message queries |
| `data/local/dao/PeerDao.kt` | Peer tracking queries |
| `data/local/PulseDatabase.kt` | Room database definition |
| `di/DatabaseModule.kt` | Hilt DI for database + DAOs |

> **All file paths are relative to** `app/src/main/java/com/pulsenet/app/`

### Done when

- [ ] All entities compile with Room annotations
- [ ] All DAO queries compile (verify with `./gradlew kspDebugKotlin`)
- [ ] Database module provides singleton DB instance via Hilt
- [ ] Unit test: insert a message, query it back, verify dedup ignores duplicate UUID

---

## Phase 2: Cryptography — Ed25519 Key Management (Hours 3–5)

**Goal:** Generate a persistent Ed25519 identity keypair on first launch, sign outgoing messages, verify incoming messages.

### What to do

1. Create `KeyManager.kt`:
   - On first launch, attempt Android Keystore Ed25519 (API 31+)
   - Fallback: Bouncy Castle software keygen → encrypt privkey with Keystore AES-256-GCM → store in SharedPreferences
   - `getPublicKey(): String` (Base64 encoded)
   - `getPrivateKey(): PrivateKey` (from Keystore or decrypted from prefs)
   - Keypair persists forever, never regenerated

2. Create `MessageSigner.kt`:
   - `sign(content: String, lat: Double, lng: Double, ...): String` → Base64 signature
   - `verify(message: MessageEntity): Boolean` → verify using sender's embedded pubkey
   - Signing payload = deterministic concatenation: `messageId|content|lat|lng|priority|createdAt`

3. Create domain model `Priority.kt`:
   ```kotlin
   enum class Priority(val value: Int) {
       SOS(0), MEDICAL(1), RESOURCE(2), GENERAL(3)
   }
   ```

4. Create domain model `Message.kt`:
   - Clean domain representation (no Room annotations)
   - Mapper functions to/from `MessageEntity`

5. Create `AppModule.kt` — Hilt module providing `KeyManager`, `MessageSigner`

### Files to create

| File | Purpose |
|---|---|
| `security/KeyManager.kt` | Ed25519 keypair generation + storage |
| `security/MessageSigner.kt` | Sign and verify message payloads |
| `domain/model/Priority.kt` | Priority enum |
| `domain/model/Message.kt` | Domain model + mappers |
| `di/AppModule.kt` | Hilt module for security classes |

### Done when

- [ ] KeyManager generates a keypair on fresh install
- [ ] KeyManager returns the same keypair on subsequent calls
- [ ] MessageSigner signs a test payload and verifies it successfully
- [ ] Tampered payload (modified content) fails verification
- [ ] Works on both API 31+ (Keystore path) and API 26–30 (Bouncy Castle path)

---

## Phase 3: Mesh Networking — Nearby Connections (Hours 5–9)

**Goal:** Discover nearby peers, connect, and exchange data using P2P_CLUSTER strategy.

### What to do

1. Create `NearbyMeshManager.kt`:
   - `startAdvertising()` — Strategy.P2P_CLUSTER, service ID `com.pulsenet.mesh`
   - `startDiscovery()` — same strategy and service ID
   - `stopAll()` — tear down advertising + discovery
   - `sendPayload(endpointId, bytes)` — send data to a specific peer
   - `ConnectionLifecycleCallback`:
     - `onConnectionInitiated` → **auto-accept** (no pairing UI in disaster)
     - `onConnectionResult(SUCCESS)` → notify GossipEngine
     - `onDisconnected` → remove from active peer set
   - `PayloadCallback`:
     - `onPayloadReceived` → route to GossipEngine for processing
   - Duty-cycling: 30s discovery window every 5 minutes (configurable)
   - SOS override: continuous discovery when SOS is active

2. Create `GossipEngine.kt`:
   - `syncWith(endpointId: String)` — triggered when a peer connects:
     - **Phase 1:** Send local message ID list, receive peer's list
     - **Phase 2:** Compute delta (set difference), request/send missing messages
     - **Phase 3:** Priority sort — SOS messages transfer first
   - `processIncoming(payload: ByteArray)` — deserialize, verify signature, check TTL:
     - If `hopCount >= maxHops` → discard
     - If signature invalid → discard + log
     - If messageId already in Room → ignore (dedup)
     - Else → insert into Room with `hopCount + 1`
   - `prepareOutgoing(messages: List<MessageEntity>)` — serialize for transfer:
     - Increment `hopCount` by 1
     - Exclude messages at max hops

3. Define wire protocol (Moshi JSON over Nearby Connections BYTES payload):
   ```json
   // Gossip handshake
   { "type": "HASH_LIST", "messageIds": ["uuid1", "uuid2", ...] }

   // Message transfer
   { "type": "MESSAGE_BATCH", "messages": [ { ...MessageEntity fields... } ] }
   ```

4. Create `domain/model/PeerNode.kt` — domain model for connected peers

### Files to create

| File | Purpose |
|---|---|
| `mesh/NearbyMeshManager.kt` | Nearby Connections advertising + discovery |
| `mesh/GossipEngine.kt` | Anti-entropy sync protocol |
| `domain/model/PeerNode.kt` | Peer domain model |

### Done when

- [ ] Two physical devices discover each other via BLE
- [ ] Connection auto-accepts without user prompt
- [ ] Hash exchange correctly computes delta (unit test with mock data)
- [ ] Messages transfer from Device A to Device B
- [ ] Duplicate messages are ignored (Room IGNORE strategy)
- [ ] Messages at max hop count (7) are NOT forwarded
- [ ] Tampered messages are dropped (signature verification)

---

## Phase 4: Foreground Service + Sensor SOS (Hours 9–13)

**Goal:** Persistent foreground service hosting the mesh engine and sensor listener. Physical tap pattern triggers SOS without touching the screen.

### What to do

1. Create `MeshService.kt` (Foreground Service):
   - `foregroundServiceType = connectedDevice`
   - Persistent notification (LOW priority):
     `"PulseNet Active • X peers nearby • Y messages cached"`
   - Lifecycle:
     - `onCreate` → initialize NearbyMeshManager, DistressSensorManager, register ConnectivityCallback
     - `onStartCommand` → start advertising + begin duty-cycled discovery
     - `onDestroy` → stop all, unregister sensors
   - Update notification in real-time as peer count / message count changes
   - Acquire partial WakeLock for sensor listener continuity

2. Create `DistressSensorManager.kt`:
   - Register `SensorEventListener` for `TYPE_LINEAR_ACCELERATION`
   - Sample rate: `SENSOR_DELAY_GAME` (20ms)
   - Detection algorithm:
     - Compute magnitude: `sqrt(x² + y² + z²)`
     - Threshold: 15.0 m/s²
     - Maintain circular buffer of last 5 spike timestamps
     - Trigger condition: 5 spikes within 3000ms window
   - On trigger:
     - Acquire WakeLock (10s)
     - GPS fix via `FusedLocationProviderClient` (high accuracy, 5s timeout)
     - Create PRIORITY_0 message → sign it → insert to Room → broadcast via NearbyMeshManager
     - Launch full-screen SOS Activity (`FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON`)
     - Vibrate device (pattern: long-short-long)
   - Debounce: 30s cooldown after trigger

3. Create `ShakePatternDetector.kt`:
   - Low-pass filter for noise reduction
   - Spike detection with configurable threshold
   - Cadence validation (reject random bumps vs. deliberate taps)

4. Create `BridgeManager.kt` (skeleton — full implementation in Phase 6):
   - Register `ConnectivityManager.NetworkCallback`
   - On `onAvailable()` with `NET_CAPABILITY_INTERNET` → flag for Phase 6

### Files to create

| File | Purpose |
|---|---|
| `mesh/MeshService.kt` | Foreground service orchestrator |
| `sensor/DistressSensorManager.kt` | SOS tap detection |
| `sensor/ShakePatternDetector.kt` | Signal processing for tap cadence |
| `mesh/BridgeManager.kt` | Network detection (skeleton) |

### Done when

- [ ] `MeshService` starts as a foreground service with visible notification
- [ ] Service survives screen-off and Doze Mode
- [ ] Notification updates live with peer/message counts
- [ ] Tapping phone 5 times rapidly triggers SOS
- [ ] SOS creates a PRIORITY_0 message in Room with GPS coordinates
- [ ] SOS screen appears even when phone is locked
- [ ] Walking/pocket movement does NOT false-trigger
- [ ] 30s cooldown prevents SOS spam

---

## Phase 5: UI — Jetpack Compose Screens (Hours 13–18)

**Goal:** Complete Android UI with onboarding, pulse dashboard, SOS screen, and message feed. Panic-proof design — giant touch targets, dark theme, zero navigation complexity.

### What to do

1. Create theme system:
   - `Color.kt` — Dark palette: background `#0A0A0F`, SOS red `#FF2D2D`, pulse blue `#00D4FF`, text white `#F0F0F0`
   - `Type.kt` — Inter/Roboto, body 18sp, headers 24sp+
   - `Theme.kt` — Dark-only MaterialTheme

2. Create `OnboardingScreen.kt` + `OnboardingViewModel.kt`:
   - Single-page emergency setup
   - Step 1: Explain what PulseNet does (brief, 2 sentences)
   - Step 2: Request all permissions in sequence (Bluetooth, Location, Nearby Devices)
   - Step 3: Request battery optimization exemption with explanation
   - Step 4: User enters a name/alias (no email, no account)
   - Step 5: Generate Ed25519 keypair (show "Identity created" confirmation)
   - Store onboarding completion in DataStore/SharedPrefs
   - Large buttons, large text, high contrast

3. Create `HomeScreen.kt` + `HomeViewModel.kt` — The Pulse Dashboard:
   - **Status bar** (top): peer count, message count, battery percentage
   - **Pulse radar** (center): animated concentric rings with peer dots
     - Use Canvas composable for custom drawing
     - Peers shown as colored dots at varying distances
     - Continuous subtle pulse animation
   - **Action buttons** (bottom): two large buttons (80dp min height):
     - 🔴 SOS Button → navigates to SOSScreen
     - 💬 Messages → navigates to MessagesScreen
   - ViewModel observes: peer count from NearbyMeshManager, message count from Room

4. Create `SOSScreen.kt` + `SOSViewModel.kt`:
   - Giant red circular button filling 60% of screen
   - **Single tap** → confirmation dialog ("Are you in danger? This alerts all nearby devices")
   - **Long press (2s)** → immediate SOS, no confirmation
   - After trigger: show GPS acquisition progress → "SOS SENT" confirmation with coordinates
   - Vibration + screen flash feedback
   - Bottom text: "If you can't use the screen, tap the back of your phone 5 times rapidly"
   - Cancel SOS option (within 10s)

5. Create `MessagesScreen.kt` + `MessagesViewModel.kt`:
   - Scrollable LazyColumn of messages from Room (observe Flow)
   - Each message card (`MessageCard.kt`):
     - Color-coded left border by priority (red/orange/yellow/gray)
     - Sender alias, content, timestamp
     - Hop count badge, distance from origin (if location available)
   - Compose FAB → bottom sheet with:
     - Text input for message content
     - Priority selector: 4 large icon buttons (SOS/Medical/Resource/General)
     - Send button → sign, insert to Room, broadcast via mesh

6. Create reusable components:
   - `PulseRadar.kt` — Canvas-based animated radar
   - `SOSButton.kt` — Reusable giant emergency button
   - `MessageCard.kt` — Message list item

7. Set up Navigation (Compose Navigation):
   - Onboarding → Home (if not completed) OR Home directly
   - Home → SOS, Home → Messages
   - SOS Activity (separate, for lock-screen bypass)

8. Create `MainActivity.kt` with `@AndroidEntryPoint`, Compose content host

### Files to create

| File | Purpose |
|---|---|
| `ui/theme/Color.kt` | Color palette |
| `ui/theme/Type.kt` | Typography |
| `ui/theme/Theme.kt` | Material theme |
| `ui/onboarding/OnboardingScreen.kt` | Permission + setup flow |
| `ui/onboarding/OnboardingViewModel.kt` | Onboarding state |
| `ui/home/HomeScreen.kt` | Pulse dashboard |
| `ui/home/HomeViewModel.kt` | Dashboard state |
| `ui/sos/SOSScreen.kt` | SOS trigger screen |
| `ui/sos/SOSViewModel.kt` | SOS state + trigger logic |
| `ui/messages/MessagesScreen.kt` | Message feed |
| `ui/messages/MessagesViewModel.kt` | Message list + compose |
| `ui/components/PulseRadar.kt` | Animated radar widget |
| `ui/components/SOSButton.kt` | Giant panic button |
| `ui/components/MessageCard.kt` | Message list item |
| `ui/MainActivity.kt` | Compose host activity |

### Done when

- [ ] App launches → Onboarding (first run) → Home
- [ ] All permissions requested and granted
- [ ] Pulse radar animates with mock peer data
- [ ] SOS button triggers message creation + vibration
- [ ] Message feed displays messages from Room (live updates via Flow)
- [ ] New message can be composed with priority selection
- [ ] Dark theme throughout, all touch targets ≥ 56dp
- [ ] App is **fully demo-able** on a single device

---

## Phase 6: Integration Testing — MVP Checkpoint (Hours 18–20)

**Goal:** Validate the complete MVP flow across physical devices. Fix any integration bugs.

### What to do

1. Install on all 4 test devices
2. Test matrix:

| Test | Steps | Expected |
|---|---|---|
| **Peer discovery** | Launch app on 2 devices within 10m, wait 30s | Both show 1 peer in radar |
| **Message sync** | Device A sends message, wait for discovery cycle | Message appears on Device B |
| **4-node cluster** | All 4 devices running | Each device discovers 3 peers |
| **SOS trigger (screen)** | Long-press SOS button on Device A | SOS message on all devices |
| **SOS trigger (sensor)** | Lock phone, tap back 5x rapidly | SOS screen + message broadcast |
| **Dedup** | Device A sends same message twice | Only one copy on Device B |
| **TTL enforcement** | Create message with hopCount=7 | Not forwarded further |
| **Signature verify** | Tamper with message in debugger | Receiving device drops it |
| **Battery** | Run 1 hour, screen off | < 3% drain |

3. Fix all P0 bugs found during testing
4. **MVP is DONE after this phase**

### Done when

- [ ] All test matrix items pass
- [ ] No crashes on any device
- [ ] Battery drain < 3%/hour in background

---

## Phase 7: Bridge Mode — Cloud Sync (Hours 20–24)

**Goal:** When a device gets internet access, flush all cached messages to MongoDB Atlas.

### What to do

1. Complete `BridgeManager.kt`:
   - `ConnectivityManager.NetworkCallback`:
     - `onAvailable()` with `NET_CAPABILITY_INTERNET` → enqueue WorkManager flush
     - Show notification: "📡 Network detected! Syncing community data..."

2. Create `BridgeFlushWorker.kt` (WorkManager `CoroutineWorker`):
   - Constraint: `NetworkType.CONNECTED`
   - Query Room for unsynced messages
   - Batch into chunks of 50
   - For each batch:
     - Convert to MongoDB GeoJSON format (longitude first!)
     - POST to Atlas Data API `insertMany`
     - On success → `markSynced()`
   - On failure → `Result.retry()` (exponential backoff)
   - Update notification with sync progress

3. Create `AtlasApiService.kt` (Retrofit interface):
   - Base URL from BuildConfig
   - `insertMany` endpoint
   - API key in header

4. Create `data/repository/SyncRepository.kt`:
   - Coordinates BridgeFlushWorker and AtlasApiService
   - Provides sync status as Flow

5. Create `di/NetworkModule.kt`:
   - Hilt module providing Retrofit, OkHttp, Moshi, API services

### Files to create

| File | Purpose |
|---|---|
| `mesh/BridgeManager.kt` | Complete network detection + WorkManager trigger |
| `worker/BridgeFlushWorker.kt` | Batch upload to Atlas |
| `data/remote/AtlasApiService.kt` | Retrofit interface for Atlas Data API |
| `data/repository/SyncRepository.kt` | Sync coordination |
| `di/NetworkModule.kt` | Hilt network module |

### Done when

- [ ] Turning on cellular data triggers automatic sync
- [ ] Messages appear in MongoDB Atlas collection
- [ ] `location` field is valid GeoJSON (longitude first)
- [ ] Sync notification shows progress
- [ ] Failed sync retries via WorkManager backoff
- [ ] Synced messages are marked `isSynced = true` in Room

---

## Phase 8: Sarvam AI Translation (Hours 24–27)

**Goal:** Translate non-English distress messages and auto-tag severity before uploading to MongoDB.

### What to do

1. Create `SarvamApiService.kt` (Retrofit interface):
   - `POST /translate`
   - Header: `api-subscription-key`
   - Body: `TranslateRequest(input, source_language_code="auto", target_language_code="en", mode="code-mixed")`
   - Response: `TranslateResponse(translated_text)`

2. Create severity classifier:
   - Keyword-based `classifySeverity(text: String): String`
   - CRITICAL: trapped, bleeding, drowning, fire, dying, crushed, buried, unconscious
   - HIGH: injured, broken, medical, pain, help, hurt, wound, fracture, ambulance
   - MEDIUM: food, water, shelter, medicine, blanket, clothes, baby, child, elderly
   - LOW: everything else

3. Integrate into `BridgeFlushWorker`:
   - Before uploading each batch, run translation on non-English messages
   - Heuristic for non-English: contains non-ASCII characters or mixed script
   - Attach `translatedContent` + `severityTag` to each MongoDB document
   - Add 100ms delay between Sarvam API calls (rate limiting)

### Files to create

| File | Purpose |
|---|---|
| `data/remote/SarvamApiService.kt` | Retrofit interface for Sarvam AI |

### Files to modify

| File | Change |
|---|---|
| `worker/BridgeFlushWorker.kt` | Add translation + severity step before upload |
| `di/NetworkModule.kt` | Provide SarvamApiService via Hilt |

### Done when

- [ ] Hindi/Bengali message gets translated to English
- [ ] Hinglish (code-mixed) message translates correctly
- [ ] Severity tags are attached: CRITICAL, HIGH, MEDIUM, LOW
- [ ] Translation runs only on non-English messages (no wasted API calls)
- [ ] MongoDB documents contain `translatedContent` and `severityTag` fields

---

## Phase 9: Map View + Rescue Dashboard + Eviction (Hours 27–30)

**Goal:** In-app map view, web-based rescue command center, and storage eviction.

### Sub-phase 9A: Android Map View

1. Create `MeshMapScreen.kt` + `MeshMapViewModel.kt`:
   - OSMDroid `MapView` wrapped in `AndroidView` composable
   - Plot all Room messages as markers (color by priority)
   - SOS markers: pulsing red overlay
   - Current location: blue dot
   - Default zoom: 15 (neighborhood)
   - Add navigation from HomeScreen

### Sub-phase 9B: Rescue Dashboard (Web)

1. Create `backend/package.json` — Express, MongoDB driver, WebSocket, dotenv, cors
2. Create `backend/.env` — MONGO_URI placeholder, PORT=3000
3. Create `backend/server.js`:
   - Connect to MongoDB Atlas
   - Ensure indexes on startup (2dsphere, priority+createdAt, messageId unique)
   - REST endpoints:
     - `POST /api/messages/bulk` — bulk insert (from Android Bridge)
     - `GET /api/messages` — paginated, filterable by priority
     - `GET /api/messages/sos` — PRIORITY_0 only
     - `GET /api/stats` — aggregate stats
     - `GET /api/heatmap` — geospatial aggregation
   - WebSocket server on same port
   - MongoDB Change Stream → broadcast new messages to all WS clients
   - Serve static files from `./public/`

4. Create `backend/public/index.html`:
   - Full-page dark command center
   - Leaflet.js map (CartoDB Dark Matter tiles)
   - leaflet.heat plugin for heat map layer
   - Color-coded markers (red/orange/yellow/gray)
   - Click marker → popup with content, translated text, severity, hop count, timestamp
   - Statistics sidebar (glassmorphism cards): total messages, active SOS, nodes seen
   - Live SOS feed panel
   - Bottom message ticker
   - WebSocket connection for real-time updates

5. Create `backend/public/style.css`:
   - Dark theme matching Android app (#0a0a0f background)
   - Accent colors: #FF2D2D (SOS), #00D4FF (pulse blue)
   - Inter font (Google Fonts)
   - Glassmorphism cards, pulsing SOS animations

6. Create `backend/public/app.js`:
   - Leaflet map initialization
   - Fetch messages on load, plot markers
   - WebSocket listener for real-time marker additions
   - Stats panel updates
   - Heat map layer toggle

### Sub-phase 9C: Eviction Worker

1. Create `worker/EvictionWorker.kt`:
   - Periodic WorkManager (every 6 hours)
   - Check message count × estimated avg size
   - If > 50MB: evict synced general messages (never evict unsynced SOS)

### Files to create

| File | Purpose |
|---|---|
| `ui/map/MeshMapScreen.kt` | OSMDroid map composable |
| `ui/map/MeshMapViewModel.kt` | Map data provider |
| `worker/EvictionWorker.kt` | Storage triage cleanup |
| `backend/package.json` | Node.js dependencies |
| `backend/.env` | Environment config |
| `backend/server.js` | Express + MongoDB + WebSocket |
| `backend/public/index.html` | Dashboard HTML |
| `backend/public/style.css` | Dashboard styles |
| `backend/public/app.js` | Dashboard JS logic |

### Done when

- [ ] In-app map shows messages with colored markers
- [ ] SOS markers pulse red
- [ ] `cd backend && npm install && npm start` launches dashboard on port 3000
- [ ] Dashboard map shows messages from MongoDB
- [ ] New messages appear on dashboard in real-time (WebSocket)
- [ ] Stats sidebar shows correct counts
- [ ] SOS feed shows latest SOS messages
- [ ] EvictionWorker deletes old synced general messages when storage threshold hit

---

## Phase 10: Stretch — Voice Notes + Ultrasonic (Hours 30–33)

**Goal:** Voice note messaging and ultrasonic GPS fallback.

### What to do (only if time permits)

1. Voice notes:
   - Add `RECORD_AUDIO` permission to manifest
   - Record 15s max via MediaRecorder → Opus
   - Send via Nearby Connections STREAM payload
   - Playback in message feed with waveform

2. Ultrasonic fallback:
   - Integrate Quiet library (`com.quiet:quiet-android`)
   - Encode GPS + priority into ultrasonic chirps
   - Trigger when Bluetooth advertising fails
   - Range: ~3m, ~100 bps

### Done when

- [ ] Voice note records, sends, and plays back on another device
- [ ] Ultrasonic chirp transmits GPS coordinates between two devices

---

## Phase 11: Demo Polish (Hours 33–36)

**Goal:** Polished demo-ready prototype with seed data, branding, and pitch materials.

### What to do

1. Seed Room DB with 50+ realistic messages:
   - Various priorities (SOS, Medical, Resource, General)
   - Realistic locations around Jaipur (MUJ campus, nearby areas)
   - Mix of Hindi, Hinglish, English content
   - Varying hop counts (1–6)

2. App icon + splash screen:
   - PulseNet logo (radar/pulse motif)
   - Dark theme splash matching app

3. Update `README.md`:
   - Project description + problem statement
   - Architecture diagram (Mermaid)
   - Setup instructions
   - Screenshots of all screens
   - Tech stack table
   - Team members

4. Record 3-minute demo video:
   - Onboarding flow
   - SOS trigger (both screen + physical tap)
   - Mesh sync between 2 devices
   - Bridge mode flush to cloud
   - Rescue dashboard live update

### Done when

- [ ] App looks polished with icon and splash
- [ ] Demo runs smoothly for 3 minutes without crashes
- [ ] README is complete with screenshots + architecture diagram
- [ ] Seed data shows a realistic disaster scenario on dashboard map
- [ ] Pitch is rehearsed and timed

---

## Quick Reference: File Count by Phase

| Phase | New Files | Modified | Total |
|---|---|---|---|
| 0 — Scaffold | 7 | 0 | 7 |
| 1 — Room DB | 6 | 0 | 6 |
| 2 — Crypto | 5 | 0 | 5 |
| 3 — Mesh | 3 | 0 | 3 |
| 4 — Service + SOS | 4 | 0 | 4 |
| 5 — UI | 15 | 0 | 15 |
| 6 — Integration test | 0 | ~5 | 5 |
| 7 — Bridge | 5 | 1 | 6 |
| 8 — Sarvam | 1 | 2 | 3 |
| 9 — Map + Dashboard | 9 | 1 | 10 |
| 10 — Stretch | 2 | 2 | 4 |
| 11 — Polish | 2 | 1 | 3 |
| **TOTAL** | **~59** | **~12** | **~71** |
