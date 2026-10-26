package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat
import com.example.data.remote.model.ContactMatchDto
import com.example.ui.components.EmptyState
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldContainer
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxSurfaceBorder
import com.example.ui.theme.ZxSurfaceCard
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextPrimary
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.viewmodel.FindContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindContactsScreen(
    viewModel: FindContactsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val matches by viewModel.matches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.syncContacts(context)
        } else {
            Toast.makeText(context, "Contacts permission was denied. You can still add friends by username.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(infoMessage) {
        infoMessage?.let {
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
                title = { Text("Find Friends from Contacts", color = ZxTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("find_contacts_back_button")) {
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
            // Privacy Explanation Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = ZxSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ZxEmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy-Preserving Contact Discovery",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ZxTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ZX Lite only checks normalized phone numbers to identify friends already on ZX Lite. Your contact names and address book are never uploaded or stored permanently.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZxTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button to Sync Contacts
            Button(
                onClick = {
                    if (hasPermission) {
                        viewModel.syncContacts(context)
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZxEmeraldPrimary,
                    contentColor = Color(0xFF042F22)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("scan_contacts_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasPermission) "Scan & Match Contacts" else "Allow Contacts Access", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Results List
            if (matches.isEmpty() && !isLoading) {
                EmptyState(
                    icon = Icons.Default.ContactPhone,
                    title = "Discover Matched Friends",
                    description = "Tap the button above to match phone numbers with registered ZX Lite accounts securely.",
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                Text(
                    text = "REGISTERED CONTACTS (${matches.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ZxEmeraldBright
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(items = matches, key = { it.id }) { match ->
                        ContactMatchRow(
                            match = match,
                            onAddFriend = { viewModel.addFriend(match.username) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ContactMatchRow(
    match: ContactMatchDto,
    onAddFriend: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZxSurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2E24)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (match.displayName.ifBlank { match.username }).firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                        fontWeight = FontWeight.Bold,
                        color = ZxEmeraldBright
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = match.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZxTextPrimary
                    )
                    Text(
                        text = "@${match.username} • ${match.phoneNumber ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZxTextSecondary
                    )
                }
            }

            if (match.isFriend) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZxEmeraldContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ZxEmeraldBright, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Friend", color = ZxEmeraldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onAddFriend,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZxEmeraldPrimary,
                        contentColor = Color(0xFF042F22)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
