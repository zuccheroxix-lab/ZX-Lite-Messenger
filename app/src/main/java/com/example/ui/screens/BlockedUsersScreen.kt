package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BlockedUserEntity
import com.example.ui.components.EmptyState
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextPrimary
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.viewmodel.BlockedUsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    viewModel: BlockedUsersViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Accounts", color = ZxTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("blocked_users_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZxTextPrimary)
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
                .padding(16.dp)
        ) {
            if (blockedUsers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Block,
                    title = "No Blocked Accounts",
                    description = "When you block accounts, they will be listed here. Blocked users cannot message you or send friend requests.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = blockedUsers, key = { it.id }) { user ->
                        BlockedUserRow(
                            user = user,
                            onUnblock = { viewModel.unblockUser(user.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedUserRow(
    user: BlockedUserEntity,
    onUnblock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2E24)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user.displayName.ifBlank { user.username }).firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                    fontWeight = FontWeight.Bold,
                    color = ZxEmeraldBright
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ZxTextPrimary
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZxTextSecondary
                )
            }

            Button(
                onClick = onUnblock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF26332C),
                    contentColor = ZxEmeraldBright
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Unblock", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
