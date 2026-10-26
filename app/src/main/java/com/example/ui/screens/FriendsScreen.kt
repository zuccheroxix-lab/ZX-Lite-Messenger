package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.FriendRequestEntity
import com.example.ui.components.EmptyState
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldContainer
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxOnlineGreen
import com.example.ui.theme.ZxStatusError
import com.example.ui.theme.ZxSurfaceBorder
import com.example.ui.theme.ZxSurfaceCard
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextPrimary
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.theme.ZxTextTertiary
import com.example.ui.viewmodel.FriendsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onOpenChat: (conversationId: String, title: String) -> Unit,
    onOpenFindContacts: () -> Unit
) {
    val context = LocalContext.current
    val friends by viewModel.friends.collectAsState()
    val incomingRequests by viewModel.incomingRequests.collectAsState()
    val outgoingRequests by viewModel.outgoingRequests.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val createdConv by viewModel.createdConversation.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(createdConv) {
        createdConv?.let { conv ->
            viewModel.onConversationNavigated()
            onOpenChat(conv.id, conv.title.ifBlank { "Chat" })
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends & Contacts", fontWeight = FontWeight.Bold, color = ZxTextPrimary) },
                actions = {
                    IconButton(
                        onClick = onOpenFindContacts,
                        modifier = Modifier.testTag("friends_find_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "Find from contacts",
                            tint = ZxEmeraldBright
                        )
                    }
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("friends_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add friend",
                            tint = ZxTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ZxSurfaceDark)
            )
        },
        containerColor = ZxBackgroundCharcoal
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = ZxSurfaceDark,
                contentColor = ZxEmeraldBright,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ZxEmeraldPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Friends (${friends.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 0) ZxEmeraldBright else ZxTextSecondary
                        )
                    }
                )

                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Requests",
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 1) ZxEmeraldBright else ZxTextSecondary
                            )
                            if (incomingRequests.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(ZxEmeraldPrimary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = incomingRequests.size.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF042F22)
                                    )
                                }
                            }
                        }
                    }
                )

                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            text = "Sent (${outgoingRequests.size})",
                            fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 2) ZxEmeraldBright else ZxTextSecondary
                        )
                    }
                )
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Friends List
                    val filteredFriends = remember(friends, searchQuery) {
                        if (searchQuery.isBlank()) friends
                        else friends.filter {
                            it.displayName.contains(searchQuery, ignoreCase = true) ||
                            it.username.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (friends.isNotEmpty()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Filter friends...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = ZxEmeraldPrimary)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ZxEmeraldPrimary,
                                    unfocusedBorderColor = ZxSurfaceBorder,
                                    focusedTextColor = ZxTextPrimary,
                                    unfocusedTextColor = ZxTextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }

                        if (filteredFriends.isEmpty()) {
                            EmptyState(
                                icon = Icons.Default.Group,
                                title = "No Friends Yet",
                                description = "Find contacts from your phone or add users by @username to start messaging.",
                                actionLabel = "Find from Contacts",
                                onActionClick = onOpenFindContacts,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(items = filteredFriends, key = { it.id }) { friend ->
                                    FriendItemRow(
                                        friend = friend,
                                        onMessageClick = { viewModel.startChatWithFriend(friend.username) },
                                        onUnfriendClick = { viewModel.unfriend(friend.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Incoming Requests
                    if (incomingRequests.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.GroupAdd,
                            title = "No Pending Requests",
                            description = "When someone sends you a friend request, it will appear here for you to accept or decline.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(items = incomingRequests, key = { it.id }) { req ->
                                IncomingRequestCard(
                                    request = req,
                                    onAccept = { viewModel.acceptRequest(req.id) },
                                    onDecline = { viewModel.declineRequest(req.id) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                2 -> {
                    // Outgoing Requests
                    if (outgoingRequests.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.PersonAdd,
                            title = "No Outgoing Requests",
                            description = "Friend requests you send to other users will be listed here until they respond.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(items = outgoingRequests, key = { it.id }) { req ->
                                OutgoingRequestCard(
                                    request = req,
                                    onCancel = { viewModel.cancelRequest(req.id) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        // Add Friend Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Friend", color = ZxTextPrimary) },
                text = {
                    Column {
                        Text(
                            text = "Enter a username (e.g. zann) or full phone number with country code (e.g. +628123456789).",
                            color = ZxTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = addInput,
                            onValueChange = { addInput = it },
                            label = { Text("Username or Phone") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZxEmeraldPrimary,
                                unfocusedBorderColor = ZxSurfaceBorder,
                                focusedTextColor = ZxTextPrimary,
                                unfocusedTextColor = ZxTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("add_friend_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trimmed = addInput.trim()
                            if (trimmed.isNotEmpty()) {
                                if (trimmed.startsWith("+") || trimmed.any { it.isDigit() } && !trimmed.any { it.isLetter() }) {
                                    viewModel.sendRequest(null, trimmed)
                                } else {
                                    val cleanUser = if (trimmed.startsWith("@")) trimmed.drop(1) else trimmed
                                    viewModel.sendRequest(cleanUser, null)
                                }
                                addInput = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Send Request", color = ZxEmeraldBright, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = ZxTextSecondary)
                    }
                },
                containerColor = ZxSurfaceDark,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun FriendItemRow(
    friend: FriendEntity,
    onMessageClick: () -> Unit,
    onUnfriendClick: () -> Unit
) {
    val initial = (friend.displayName.ifBlank { friend.username }).firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMessageClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B2E24)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = ZxEmeraldBright
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = friend.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ZxTextPrimary
                )
                if (friend.isOnline) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ZxOnlineGreen)
                    )
                }
            }
            Text(
                text = "@${friend.username} • ${friend.bio ?: "Available"}",
                style = MaterialTheme.typography.bodySmall,
                color = ZxTextSecondary,
                maxLines = 1
            )
        }

        IconButton(onClick = onMessageClick) {
            Icon(Icons.Default.Chat, contentDescription = "Chat", tint = ZxEmeraldPrimary)
        }
    }
}

@Composable
fun IncomingRequestCard(
    request: FriendRequestEntity,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2E24)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (request.peerDisplayName.ifBlank { request.peerUsername }).firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                        fontWeight = FontWeight.Bold,
                        color = ZxEmeraldBright
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = request.peerDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZxTextPrimary
                    )
                    Text(
                        text = "@${request.peerUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZxEmeraldPrimary,
                        contentColor = Color(0xFF042F22)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZxTextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZxSurfaceBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
fun OutgoingRequestCard(
    request: FriendRequestEntity,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.peerDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ZxTextPrimary
                )
                Text(
                    text = "@${request.peerUsername} • Pending response",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZxTextSecondary
                )
            }

            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = ZxStatusError)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    }
}
