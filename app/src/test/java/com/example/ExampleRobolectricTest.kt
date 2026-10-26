package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.preferences.UserPreferencesManager
import com.example.core.security.SecureSessionManager
import com.example.data.local.entity.MessageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext_matchesAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZX Lite", appName)
    }

    @Test
    fun secureSessionManager_saveAndRetrieveSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SecureSessionManager(context)

        sessionManager.saveSession(
            accessToken = "test_token_abc_123",
            refreshToken = "test_refresh_xyz_789",
            userId = "usr_9988",
            username = "zann",
            displayName = "Zucchero Xann"
        )

        assertTrue(sessionManager.isLoggedIn())
        assertEquals("test_token_abc_123", sessionManager.getAccessToken())
        assertEquals("usr_9988", sessionManager.getUserId())
        assertEquals("zann", sessionManager.getUsername())
        assertEquals("Zucchero Xann", sessionManager.getDisplayName())

        sessionManager.clearSession()
        assertEquals(false, sessionManager.isLoggedIn())
    }

    @Test
    fun userPreferencesManager_defaultValuesAndUpdates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesManager(context)

        assertEquals("http://10.0.2.2:3000", prefs.getServerUrl())
        assertTrue(prefs.isDarkTheme())
        assertTrue(prefs.isNotificationsEnabled())

        prefs.setServerUrl("https://api.zxlite.chat")
        assertEquals("https://api.zxlite.chat", prefs.getServerUrl())

        prefs.setFontScale(UserPreferencesManager.FONT_SCALE_LARGE)
        assertEquals(UserPreferencesManager.FONT_SCALE_LARGE, prefs.getFontScale(), 0.01f)
    }

    @Test
    fun messageEntity_idempotencyKeyPreserved() {
        val key = UUID.randomUUID().toString()
        val msg = MessageEntity(
            id = "msg_001",
            conversationId = "conv_100",
            senderId = "usr_1",
            senderUsername = "user1",
            content = "Hello ZX Lite",
            timestamp = 1700000000000L,
            status = MessageEntity.STATUS_PENDING,
            isOutgoing = true,
            idempotencyKey = key
        )

        assertNotNull(msg.idempotencyKey)
        assertEquals(key, msg.idempotencyKey)
        assertEquals(MessageEntity.STATUS_PENDING, msg.status)
    }

    @Test
    fun friendEntity_and_requestEntity_properties() {
        val friend = com.example.data.local.entity.FriendEntity(
            id = "f_123",
            username = "alice",
            displayName = "Alice Wonderland",
            avatarUrl = null,
            bio = "Always ready",
            isOnline = true,
            friendsSince = System.currentTimeMillis()
        )
        assertEquals("alice", friend.username)
        assertEquals("Alice Wonderland", friend.displayName)
        assertTrue(friend.isOnline)

        val req = com.example.data.local.entity.FriendRequestEntity(
            id = "req_456",
            type = "INCOMING",
            peerId = "u_99",
            peerUsername = "bob",
            peerDisplayName = "Bob Builder",
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        assertEquals("bob", req.peerUsername)
        assertEquals("INCOMING", req.type)
    }

    @Test
    fun userStatusEntity_twentyFourHourExpirationCalculation() {
        val now = System.currentTimeMillis()
        val activeStatus = com.example.data.local.entity.UserStatusEntity(
            id = "status_1",
            userId = "u_1",
            username = "zann",
            displayName = "Zucchero Xann",
            avatarUrl = null,
            text = "Coding ZX Messenger",
            createdAt = now,
            expiresAt = now + 24 * 60 * 60 * 1000L
        )

        assertTrue(activeStatus.expiresAt > now)
        assertEquals("Coding ZX Messenger", activeStatus.text)
    }
}
