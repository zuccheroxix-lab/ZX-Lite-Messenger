package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ConversationEntity
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.EmptyState
import com.example.ui.components.ZxMonogram
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxStatusRead
import com.example.ui.theme.ZxStatusSent
import com.example.ui.theme.ZxSurfaceCard
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextPrimary
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.theme.ZxTextTertiary
import com.example.ui.viewmodel.ConversationsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    onOpenChat: (conversationId: String, title: String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZxMonogram(size = 32.dp, textSizeSp = 14)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ZX Lite",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ZxTextPrimary
                            )
                            Text(
                                text = "ZUCCHERO XANN",
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = ZxEmeraldBright
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("search_users_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Users",
                            tint = ZxTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("profile_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = ZxTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ZxTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZxSurfaceDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenSearch,
                containerColor = ZxEmeraldPrimary,
                contentColor = Color(0xFF042F22),
                shape = CircleShape,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.AddComment,
                    contentDescription = "New Message"
                )
            }
        },
        containerColor = ZxBackgroundCharcoal
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Connection Status (Reconnection backoff, offline notification)
            ConnectionStatusBar(
                connectionState = connectionState,
                onRetry = { viewModel.reconnect() }
            )

            if (conversations.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AddComment,
                    title = "No Conversations Yet",
                    description = "Search for a username to start messaging directly with low data and realtime sync.",
                    actionLabel = "Start a Chat",
                    onActionClick = onOpenSearch,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("conversations_list"),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = conversations,
                        key = { it.id }
                    ) { conversation ->
                        ConversationListItem(
                            conversation = conversation,
                            onClick = {
                                onOpenChat(
                                    conversation.id,
                                    conversation.title.ifBlank { conversation.peerUsername ?: "Chat" }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationListItem(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    val initial = (conversation.title.ifBlank { conversation.peerUsername ?: "Z" })
        .firstOrNull()?.uppercaseChar()?.toString() ?: "Z"

    val timeFormatted = remember(conversation.lastMessageTimestamp) {
        if (conversation.lastMessageTimestamp <= 0L) ""
        else {
            val date = Date(conversation.lastMessageTimestamp)
            val now = System.currentTimeMillis()
            val diff = now - conversation.lastMessageTimestamp
            if (diff < 24 * 3600 * 1000) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_item_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with Initial
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B2E24)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ZxEmeraldBright
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name and Last Message
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.title.ifBlank { conversation.peerUsername ?: "Direct Chat" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ZxTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (timeFormatted.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = 12.sp,
                        color = if (conversation.unreadCount > 0) ZxEmeraldBright else ZxTextTertiary,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessageText ?: "Tap to start conversation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.unreadCount > 0) ZxTextPrimary else ZxTextSecondary,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ZxEmeraldPrimary)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = Color(0xFF042F22),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
