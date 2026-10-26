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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.preferences.UserPreferencesManager
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldContainer
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxStatusError
import com.example.ui.theme.ZxSurfaceBorder
import com.example.ui.theme.ZxSurfaceCard
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextPrimary
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenPrivacySettings: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onOpenBlockedUsers: () -> Unit = {},
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val serverUrl by viewModel.serverUrl.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isServerDialogVisible by viewModel.isServerDialogVisible.collectAsState()
    val serverUrlInput by viewModel.serverUrlInput.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = ZxTextPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Account info card
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACCOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ZxEmeraldBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.getDisplayName(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ZxTextPrimary
                    )
                    Text(
                        text = "@${viewModel.getUsername()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZxTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PRIVACY & SECURITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ZxEmeraldBright
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsTile(
                icon = Icons.Default.Security,
                title = "Privacy & Discovery",
                subtitle = "Phone discovery, contact auto-accept, read receipts",
                onClick = onOpenPrivacySettings,
                testTag = "settings_privacy_tile"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsTile(
                icon = Icons.Default.Block,
                title = "Blocked Accounts",
                subtitle = "Manage blocked contacts and messaging restrictions",
                onClick = onOpenBlockedUsers,
                testTag = "settings_blocked_users_tile"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "NETWORK & SERVER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ZxEmeraldBright
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsTile(
                icon = Icons.Default.Dns,
                title = "Backend Server URL",
                subtitle = serverUrl,
                onClick = { viewModel.showServerDialog() },
                testTag = "settings_server_tile"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DISPLAY & PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ZxEmeraldBright
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Font Scale Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = ZxEmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Text Size Scale",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ZxTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Small" to UserPreferencesManager.FONT_SCALE_SMALL,
                            "Normal" to UserPreferencesManager.FONT_SCALE_MEDIUM,
                            "Large" to UserPreferencesManager.FONT_SCALE_LARGE
                        ).forEach { (label, scale) ->
                            val isSelected = fontScale == scale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) ZxEmeraldContainer else Color(0xFF1E2822),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setFontScale(scale) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ZxEmeraldBright else ZxTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Notifications Toggle
            SettingsSwitchTile(
                icon = Icons.Default.Notifications,
                title = "In-App Notifications",
                subtitle = "Vibrate and alert for incoming messages",
                checked = notifications,
                onCheckedChange = { viewModel.toggleNotifications() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsTile(
                icon = Icons.Default.Notifications,
                title = "Notification Preferences & Permissions",
                subtitle = "Alert sounds, message previews, Android 13+ permission",
                onClick = onOpenNotificationSettings,
                testTag = "settings_notifications_tile"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dark Theme Toggle
            SettingsSwitchTile(
                icon = Icons.Default.DarkMode,
                title = "ZX Dark Premium Theme",
                subtitle = "Optimized charcoal and emerald aesthetic",
                checked = darkTheme,
                onCheckedChange = { viewModel.toggleDarkTheme() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "STORAGE & DATA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ZxEmeraldBright
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsTile(
                icon = Icons.Default.CleaningServices,
                title = "Clear Local Message Cache",
                subtitle = "Frees database storage; syncs fresh from backend",
                onClick = { viewModel.clearLocalCache() },
                testTag = "settings_clear_cache_tile"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Logout Button
            Button(
                onClick = { viewModel.logout(onLoggedOut) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF331616),
                    contentColor = ZxStatusError
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out of ZX Lite", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ZX Lite Messenger v1.0 • ZUCCHERO XANN",
                style = MaterialTheme.typography.bodySmall,
                color = ZxTextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Server URL Dialog
        if (isServerDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.hideServerDialog() },
                title = { Text("Set Backend Server URL", color = ZxTextPrimary) },
                text = {
                    Column {
                        Text(
                            text = "Set the Fastify REST & WebSocket backend address. For Android Emulator, use http://10.0.2.2:3000.",
                            color = ZxTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { viewModel.onServerUrlInputChange(it) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZxEmeraldPrimary,
                                unfocusedBorderColor = ZxSurfaceBorder,
                                focusedTextColor = ZxTextPrimary,
                                unfocusedTextColor = ZxTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("server_url_dialog_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.saveServerUrl() }) {
                        Text("Save & Reconnect", color = ZxEmeraldBright, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideServerDialog() }) {
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
fun SettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ZxEmeraldPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ZxTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ZxTextSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ZxEmeraldPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ZxTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ZxTextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ZxEmeraldBright,
                    checkedTrackColor = ZxEmeraldContainer,
                    uncheckedThumbColor = ZxTextSecondary,
                    uncheckedTrackColor = Color(0xFF1E2822)
                )
            )
        }
    }
}
