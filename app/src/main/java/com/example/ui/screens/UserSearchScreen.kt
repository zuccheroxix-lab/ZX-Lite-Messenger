package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.model.UserDto
import com.example.ui.components.EmptyState
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxSurfaceBorder
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextPrimary
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.viewmodel.UserSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    viewModel: UserSearchViewModel,
    onBack: () -> Unit,
    onOpenChat: (conversationId: String, title: String) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val createdConv by viewModel.createdConversation.collectAsState()

    LaunchedEffect(createdConv) {
        createdConv?.let { conv ->
            viewModel.onConversationNavigated()
            onOpenChat(conv.id, conv.title.ifBlank { conv.peerUsername ?: "Chat" })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Users", color = ZxTextPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("user_search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ZxTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZxSurfaceDark
                )
            )
        },
        containerColor = ZxBackgroundCharcoal
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Search by username or name...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ZxEmeraldPrimary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = ZxTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZxEmeraldPrimary,
                    unfocusedBorderColor = ZxSurfaceBorder,
                    focusedTextColor = ZxTextPrimary,
                    unfocusedTextColor = ZxTextPrimary,
                    cursorColor = ZxEmeraldPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_search_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ZxEmeraldPrimary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (query.isNotEmpty() && results.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No Users Found",
                    description = "No user found matching \"$query\". Check spelling and try again.",
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (query.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "Search by Username",
                    description = "Enter a username to initiate direct 1-on-1 end-to-server messaging.",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("user_search_results_list")
                ) {
                    items(items = results, key = { it.id }) { user ->
                        UserSearchResultItem(
                            user = user,
                            onChatClick = { viewModel.startChatWith(user.username) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    user: UserDto,
    onChatClick: () -> Unit
) {
    val initial = (user.displayName.ifBlank { user.username })
        .firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZxSurfaceDark)
            .clickable(onClick = onChatClick)
            .padding(12.dp)
            .testTag("user_search_item_${user.username}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2E24)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ZxEmeraldBright
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = user.displayName.ifBlank { user.username },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ZxTextPrimary
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZxTextSecondary
                )
            }
        }

        Button(
            onClick = onChatClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZxEmeraldPrimary,
                contentColor = Color(0xFF042F22)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("chat_with_${user.username}_button")
        ) {
            Text("Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
