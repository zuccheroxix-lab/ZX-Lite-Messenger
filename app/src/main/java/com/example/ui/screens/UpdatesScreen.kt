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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.UserStatusEntity
import com.example.ui.components.EmptyState
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxSurfaceBorder
import com.example.ui.theme.ZxSurfaceCard
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextPrimary
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.theme.ZxTextTertiary
import com.example.ui.viewmodel.UpdatesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    viewModel: UpdatesViewModel
) {
    val context = LocalContext.current
    val statuses by viewModel.statuses.collectAsState()
    val postSuccess by viewModel.postSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showComposeDialog by remember { mutableStateOf(false) }
    var statusInput by remember { mutableStateOf("") }

    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            Toast.makeText(context, "Status update published", Toast.LENGTH_SHORT).show()
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
                title = { Text("Updates", fontWeight = FontWeight.Bold, color = ZxTextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ZxSurfaceDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showComposeDialog = true },
                containerColor = ZxEmeraldPrimary,
                contentColor = Color(0xFF042F22),
                shape = CircleShape,
                modifier = Modifier.testTag("post_update_fab")
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New Status")
            }
        },
        containerColor = ZxBackgroundCharcoal
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // My Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showComposeDialog = true }
                    .testTag("my_status_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2E24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.getMyDisplayName().firstOrNull()?.uppercaseChar()?.toString() ?: "M",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ZxEmeraldBright
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        Text(
                            text = "Tap to share what's on your mind (24h)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextSecondary
                        )
                    }

                    Icon(Icons.Default.Add, contentDescription = "Add", tint = ZxEmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "RECENT UPDATES FROM FRIENDS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ZxEmeraldBright
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (statuses.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = "No Recent Updates",
                    description = "When your friends post updates or status notes, they will appear here for 24 hours.",
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(items = statuses, key = { it.id }) { item ->
                        StatusUpdateRow(status = item)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Status Composer Dialog
        if (showComposeDialog) {
            AlertDialog(
                onDismissRequest = { showComposeDialog = false },
                title = { Text("New Status Update", color = ZxTextPrimary) },
                text = {
                    Column {
                        Text(
                            text = "Share a quick thought with your friends. Automatically expires in 24 hours.",
                            color = ZxTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = statusInput,
                            onValueChange = { if (it.length <= 500) statusInput = it },
                            placeholder = { Text("What's happening?") },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZxEmeraldPrimary,
                                unfocusedBorderColor = ZxSurfaceBorder,
                                focusedTextColor = ZxTextPrimary,
                                unfocusedTextColor = ZxTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("status_composer_input")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${statusInput.length} / 500",
                            fontSize = 11.sp,
                            color = ZxTextTertiary,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (statusInput.trim().isNotEmpty()) {
                                viewModel.postStatus(statusInput)
                                statusInput = ""
                                showComposeDialog = false
                            }
                        }
                    ) {
                        Text("Post", color = ZxEmeraldBright, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showComposeDialog = false }) {
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
fun StatusUpdateRow(status: UserStatusEntity) {
    val initial = (status.displayName.ifBlank { status.username }).firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    val timeFormatted = remember(status.createdAt) {
        val diff = System.currentTimeMillis() - status.createdAt
        when {
            diff < 60 * 1000 -> "Just now"
            diff < 3600 * 1000 -> "${diff / 60000}m ago"
            diff < 24 * 3600 * 1000 -> "${diff / 3600000}h ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(status.createdAt))
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2E24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = initial, fontWeight = FontWeight.Bold, color = ZxEmeraldBright)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        Text(
                            text = "@${status.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextSecondary
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = ZxTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = status.text,
                style = MaterialTheme.typography.bodyMedium,
                color = ZxTextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}
