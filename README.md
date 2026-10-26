# ZX Lite Messenger

> **Developer & Branding:** ZUCCHERO XANN  
> **Core Focus:** Ultra-low RAM, minimal data consumption, and reliable realtime messaging between independent Android devices over the internet.

---

## Overview

ZX Lite Messenger is a high-performance native Android application built with **Jetpack Compose**, **Kotlin Coroutines**, and **Material Design 3**, accompanied by a production-ready **Node.js + Fastify + PostgreSQL** backend with realtime **WebSockets**.

Designed for resilience in high-latency or fluctuating connectivity environments, ZX Lite features local caching via **Room (v2 schema)**, secure credential isolation via the **Android Keystore**, automatic exponential backoff with jitter on reconnects, an **offline message queue with UUID-based idempotency keys** to guarantee zero duplicated messages, and a **comprehensive friendship and contact discovery engine**.

---

## Visual & Design Philosophy

- **Dark Premium Charcoal Aesthetic**: Deep charcoal background (`#0D0F0E`), graphite surface containers (`#141C18`), and sharp emerald accents (`#10B981` / `#34D399`).
- **Original Monogram Logo**: Signature ZX monogram with high-contrast emerald vector rendering.
- **Resource Conservation**: No heavy blur effects, continuous animation loops, or extraneous memory hogs.
- **Dynamic Text Scaling**: Native settings toggle between Small (0.85x), Normal (1.0x), and Large (1.15x) font scales.
- **4-Tab Bottom Navigation**: Native Material 3 NavigationBar with Chats, Friends, Updates, and Settings.

---

## Key Features & Functional Modules

### 1. Chats & Direct Messaging
- 1-on-1 direct conversations gated by friendship status and block list verification.
- Realtime delivery over WebSockets with offline Room fallback and auto-sync outbox.
- Message status indicators: `PENDING` (clock), `SENT` (single checkmark), and `READ` (double emerald checkmark).
- 30-message cursor pagination avoiding full history memory dumps.

### 2. Friendship & Request System
- Search users by `@username` or verified international phone number (`+E.164`).
- Incoming, Outgoing, and Accepted friends list tabs.
- Accept, decline, or cancel pending requests with instantaneous UI and database updates.
- Realtime socket events: `FRIEND_REQUEST_RECEIVED` and `FRIEND_REQUEST_ACCEPTED`.
- Unfriend and block/unblock actions with mutual chat revocation.

### 3. Contact Discovery & Privacy
- **Permission-Gated Contact Matching**: Safely queries device contacts via Android Contacts Provider (`READ_CONTACTS`).
- **Zero Full Address Book Storage**: Normalizes phone numbers to E.164 without uploading names, emails, or personal contact metadata.
- **Optional Mutual Auto-Accept**: When both accounts enable `auto_accept_matched_contacts` and have matching verified phone numbers, friendship is automatically confirmed. Disabled by default for maximum privacy.
- **Manual Username Fallback**: Fully functional search even if contact permissions are denied.

### 4. 24-Hour Ephemeral Status Updates
- Post and read lightweight text status updates.
- Automatic 24-hour expiration filter enforced in database queries and UI layers.

### 5. Profile & Live Account Editing
- Monogram avatar, `@username`, custom bio/status, and verified phone number.
- In-app profile editor with instant validation.
- Technical specifications card detailing hardware Keystore encryption and transport protocols.

### 6. Notifications & System Channels
- Pre-configured Android O+ notification channels:
  - `zx_messages`: High importance for incoming direct messages.
  - `zx_friend_requests`: Standard importance for friendship invitations.
- Runtime `POST_NOTIFICATIONS` permission management on Android 13+.
- FCM token registration endpoint `/users/fcm-token` for background push delivery.

---

## Project Architecture

### 1. Android Application (`/app`)
- **Presentation**: 100% Jetpack Compose with Material 3, adhering to edge-to-edge system insets and accessible touch targets (48dp+).
- **Security**: `SecureSessionManager` protects JWT access/refresh tokens in SharedPreferences using hardware-backed **Android Keystore AES-256-GCM**.
- **Local Persistence & Cache**: **Room Database (v2)** storing:
  - `UserEntity`
  - `ConversationEntity`
  - `MessageEntity`
  - `OfflineQueueEntity`
  - `FriendEntity`
  - `FriendRequestEntity`
  - `BlockedUserEntity`
  - `UserStatusEntity`
- **Networking**:
  - **Retrofit & Moshi**: REST API communication for auth, friendships, contacts, updates, user blocks, and profile settings.
  - **OkHttp WebSocket**: Bidirectional socket for instantaneous message delivery, typing receipts, and friend request alerts.
- **Offline & Reconnection**: Messages sent during network interruption are saved locally with `PENDING` status and queued in `offline_queue`. On reconnection (with exponential backoff and randomized jitter), the queue automatically flushes to the server.

### 2. Backend Server (`/backend`)
- **Runtime**: Node.js 20 LTS with TypeScript.
- **Web Framework**: Fastify for low-overhead HTTP request processing and `@fastify/websocket` for realtime duplex communication.
- **Database**: PostgreSQL with schema migrations:
  - `001_initial_schema.sql`: Users, refresh tokens, conversations, conversation members, messages, receipts.
  - `002_friends_contacts_privacy.sql`: Friendship requests, friendships (canonical pair constraint), blocked users, privacy settings, user statuses, and FCM tokens.
- **Dual Engine**: Supports real PostgreSQL database and includes an in-memory fallback for immediate zero-setup execution and tests.
- **Security**: Password hashing using `bcryptjs`, JWT authorization tokens, rate limiting, and strict friend/block validation gates.

---

## Setup & Running Guide

### 1. Running the Backend with Docker Compose

To run PostgreSQL and the Fastify backend server simultaneously:

```bash
docker compose up --build -d
```

The REST API will be available at `http://localhost:3000` and WebSockets at `ws://localhost:3000/ws`.

Alternatively, run locally with Node.js:
```bash
cd backend
npm install
npm run build
npm start
```

### 2. Running Automated Tests

Run the backend test suite:
```bash
npm --prefix backend test
```

Run Android unit and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```

### 3. Configuring the Android App for Local Development

When running in an Android Emulator, the host machine's `localhost` is mapped to `10.0.2.2`.
- Default server configured in ZX Lite: `http://10.0.2.2:3000`
- You can change the backend URL directly inside the app:
  1. On the **Sign In / Register** screen, tap the Server Settings icon in the top right.
  2. Or navigate to **Settings -> Backend Server URL** anytime after signing in.

### 4. Building & Downloading the Android APK

#### A. Automated Builds via GitHub Actions (Recommended)
This repository includes a continuous integration and deployment workflow (`.github/workflows/android.yml`):
- **On Push to `main` / `master`**: Automatically builds and tests the app, producing the `ZX-Lite-Messenger-APK` artifact.
- **Manual Trigger (`workflow_dispatch`)**: Go to **Actions** -> **ZX Lite Messenger CI & Release** -> click **Run workflow**.
- **On Version Tag (e.g. `v1.0.0`)**: Automatically creates a GitHub Release and attaches the standalone installable APK as `ZX-Lite-Messenger.apk`.

#### B. Direct Download Link Pattern
When a release tag is pushed to your repository:
```
https://github.com/<OWNER>/<REPO>/releases/latest/download/ZX-Lite-Messenger.apk
```
*(Replace `<OWNER>/<REPO>` with your actual GitHub username/organization and repository name).*

#### C. Local Build via Gradle
```bash
# Using Gradle wrapper
./gradlew :app:assembleDebug

# Or using system Gradle
gradle :app:assembleDebug
```
The resulting debug APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk` (or `ZX-Lite-Messenger.apk`).

#### D. Keystore Signing Configuration
- **Test / Sideloading (Default)**: Automatically signed using the debug certificate (`debug.keystore`), allowing immediate installation on any physical Android smartphone (Android 7.0+ / API 24+) or emulator.
- **Custom Production Signing**: To sign with your private upload key in GitHub Actions, configure the following GitHub Secrets in your repository settings:
  - `KEYSTORE_BASE64`: Base64-encoded string of your `.jks` or `.keystore` file (`base64 -w 0 my-key.jks`).
  - `STORE_PASSWORD`: Keystore store password.
  - `KEY_PASSWORD`: Key alias password.

---

## Testing Verification Points

- **Authentication**: Duplicate usernames or duplicate verified phone numbers are rejected with status 409.
- **Friendship Gating**: Attempting to open a direct chat or send a message to a non-friend returns HTTP 403 Forbidden.
- **Contact Discovery**: Validates E.164 normalization against database records while preserving contact address book privacy.
- **Blocking Protection**: Blocked accounts are immediately disallowed from sending friend requests, direct chats, or reading updates.
- **Idempotency**: Retransmitting a message with an identical `idempotencyKey` confirms delivery without creating duplicate records.
- **Offline Delivery**: Sending while disconnected retains the message in local Room storage with `PENDING` status. Upon network restoration, messages are transmitted to the server and updated to `SENT`.
- **Read Receipts**: Marking a conversation as open immediately updates peer delivery status to `READ` (double checkmark in emerald green).

---

**ZX Lite Messenger** — Engineered by **ZUCCHERO XANN**.
