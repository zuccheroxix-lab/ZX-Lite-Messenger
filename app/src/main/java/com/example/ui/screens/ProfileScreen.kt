package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxStatusError
import com.example.ui.theme.ZxSurfaceBorder
import com.example.ui.theme.ZxSurfaceCard
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextPrimary
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.theme.ZxTextTertiary
import com.example.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    fallbackUsername: String,
    fallbackDisplayName: String,
    fallbackUserId: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val feedback by viewModel.feedback.collectAsState()

    val currentDisplayName = profile?.displayName ?: fallbackDisplayName
    val currentUsername = profile?.username ?: fallbackUsername
    val currentUserId = profile?.id ?: fallbackUserId
    val currentBio = profile?.bio ?: "Available on ZX Lite"
    val currentPhone = profile?.phoneNumber

    var editDisplayName by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
    var editBio by remember(currentBio) { mutableStateOf(currentBio) }
    var editPhone by remember(currentPhone) { mutableStateOf(currentPhone ?: "") }

    LaunchedEffect(feedback) {
        feedback?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Account", color = ZxTextPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ZxTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startEditing() },
                        modifier = Modifier.testTag("profile_edit_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = ZxEmeraldBright
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Avatar Monogram
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF162A20))
                    .border(2.dp, ZxEmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (currentDisplayName.ifBlank { currentUsername }).firstOrNull()?.uppercaseChar()?.toString() ?: "Z",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZxEmeraldBright
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = currentDisplayName.ifBlank { currentUsername },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ZxTextPrimary
            )

            Text(
                text = "@$currentUsername",
                style = MaterialTheme.typography.bodyMedium,
                color = ZxEmeraldBright
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentBio,
                style = MaterialTheme.typography.bodyMedium,
                color = ZxTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.startEditing() },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZxEmeraldPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZxEmeraldBright),
                modifier = Modifier.testTag("edit_profile_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile Details", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Phone Number Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = ZxEmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VERIFIED PHONE NUMBER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = ZxEmeraldBright
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentPhone?.ifBlank { "Not configured" } ?: "Not configured (Required for contact discovery)",
                            fontSize = 14.sp,
                            color = if (currentPhone.isNullOrBlank()) ZxTextTertiary else ZxTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security & Specifications Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SECURITY & SPECIFICATIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ZxEmeraldBright
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileSpecRow(
                        icon = Icons.Default.Security,
                        label = "Credentials Storage",
                        value = "Hardware Keystore AES-256"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileSpecRow(
                        icon = Icons.Default.Speed,
                        label = "Network Protocol",
                        value = "Fastify REST & WebSocket"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileSpecRow(
                        icon = Icons.Default.Memory,
                        label = "Memory Optimization",
                        value = "Low RAM / Cursor Paging"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileSpecRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Message Deduplication",
                        value = "UUID Idempotency Active"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ZX ACCOUNT ID",
                        fontSize = 10.sp,
                        color = ZxTextTertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentUserId,
                        fontSize = 12.sp,
                        color = ZxTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF331616),
                    contentColor = ZxStatusError
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
        }

        // Edit Profile Dialog
        if (isEditing) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelEditing() },
                title = { Text("Edit Profile Details", color = ZxTextPrimary) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editDisplayName,
                            onValueChange = { editDisplayName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZxEmeraldPrimary,
                                unfocusedBorderColor = ZxSurfaceBorder,
                                focusedTextColor = ZxTextPrimary,
                                unfocusedTextColor = ZxTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio / Status") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZxEmeraldPrimary,
                                unfocusedBorderColor = ZxSurfaceBorder,
                                focusedTextColor = ZxTextPrimary,
                                unfocusedTextColor = ZxTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number (E.164 e.g. +628123456789)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZxEmeraldPrimary,
                                unfocusedBorderColor = ZxSurfaceBorder,
                                focusedTextColor = ZxTextPrimary,
                                unfocusedTextColor = ZxTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("edit_phone_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.saveProfile(
                                displayName = editDisplayName,
                                bio = editBio,
                                phoneNumber = editPhone.ifBlank { null }
                            )
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ZxEmeraldBright)
                        } else {
                            Text("Save", color = ZxEmeraldBright, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelEditing() }) {
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
fun ProfileSpecRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ZxEmeraldPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = ZxTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ZxTextPrimary
        )
    }
}
