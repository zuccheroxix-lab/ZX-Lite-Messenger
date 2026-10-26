package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ZxMonogram
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
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZxBackgroundCharcoal)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { viewModel.openServerSettings() },
                    modifier = Modifier.testTag("server_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Server Settings",
                        tint = ZxTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            ZxMonogram(size = 64.dp, textSizeSp = 28)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ZX LITE MESSENGER",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = ZxTextPrimary
            )
            Text(
                text = "BY ZUCCHERO XANN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = ZxEmeraldBright
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZxSurfaceDark)
                    .border(1.dp, ZxSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!uiState.isRegisterMode) ZxEmeraldContainer else Color.Transparent)
                        .clickable { if (uiState.isRegisterMode) viewModel.toggleMode() }
                        .padding(vertical = 10.dp)
                        .testTag("login_tab_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        fontWeight = if (!uiState.isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (!uiState.isRegisterMode) ZxEmeraldBright else ZxTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (uiState.isRegisterMode) ZxEmeraldContainer else Color.Transparent)
                        .clickable { if (!uiState.isRegisterMode) viewModel.toggleMode() }
                        .padding(vertical = 10.dp)
                        .testTag("register_tab_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Register",
                        fontWeight = if (uiState.isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.isRegisterMode) ZxEmeraldBright else ZxTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error banner
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF331616))
                            .border(1.dp, ZxStatusError.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ZxStatusError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Input Fields
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("Username") },
                placeholder = { Text("e.g. zann") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ZxEmeraldPrimary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = if (uiState.isRegisterMode) ImeAction.Next else ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZxEmeraldPrimary,
                    unfocusedBorderColor = ZxSurfaceBorder,
                    focusedLabelColor = ZxEmeraldBright,
                    unfocusedLabelColor = ZxTextSecondary,
                    focusedTextColor = ZxTextPrimary,
                    unfocusedTextColor = ZxTextPrimary,
                    cursorColor = ZxEmeraldPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_username_input")
            )

            if (uiState.isRegisterMode) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = { viewModel.onDisplayNameChange(it) },
                    label = { Text("Display Name (optional)") },
                    placeholder = { Text("e.g. Zucchero Xann") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ZxEmeraldPrimary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZxEmeraldPrimary,
                        unfocusedBorderColor = ZxSurfaceBorder,
                        focusedLabelColor = ZxEmeraldBright,
                        unfocusedLabelColor = ZxTextSecondary,
                        focusedTextColor = ZxTextPrimary,
                        unfocusedTextColor = ZxTextPrimary,
                        cursorColor = ZxEmeraldPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_display_name_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ZxEmeraldPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = ZxTextSecondary
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.submit() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZxEmeraldPrimary,
                    unfocusedBorderColor = ZxSurfaceBorder,
                    focusedLabelColor = ZxEmeraldBright,
                    unfocusedLabelColor = ZxTextSecondary,
                    focusedTextColor = ZxTextPrimary,
                    unfocusedTextColor = ZxTextPrimary,
                    cursorColor = ZxEmeraldPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = { viewModel.submit() },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZxEmeraldPrimary,
                    contentColor = Color(0xFF042F22),
                    disabledContainerColor = ZxSurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button")
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.isRegisterMode) "Create Account" else "Sign In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Demo / Standalone Mode note
            Text(
                text = "Connecting to backend at " + uiState.serverUrlInput.ifEmpty { "10.0.2.2:3000" },
                fontSize = 12.sp,
                color = ZxTextSecondary
            )
        }

        // Server Config Dialog
        if (uiState.isServerSettingsOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.closeServerSettings() },
                title = { Text("Server Configuration", color = ZxTextPrimary) },
                text = {
                    Column {
                        Text(
                            text = "Specify the backend address for REST API and WebSocket connection. For Android Emulator, use http://10.0.2.2:3000.",
                            color = ZxTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.serverUrlInput,
                            onValueChange = { viewModel.onServerUrlChange(it) },
                            label = { Text("Backend URL") },
                            placeholder = { Text("http://10.0.2.2:3000") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZxEmeraldPrimary,
                                unfocusedBorderColor = ZxSurfaceBorder,
                                focusedTextColor = ZxTextPrimary,
                                unfocusedTextColor = ZxTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("server_url_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.saveServerUrl() }) {
                        Text("Save & Connect", color = ZxEmeraldBright, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeServerSettings() }) {
                        Text("Cancel", color = ZxTextSecondary)
                    }
                },
                containerColor = ZxSurfaceDark,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
