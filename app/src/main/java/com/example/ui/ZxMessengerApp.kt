package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import com.example.core.preferences.UserPreferencesManager
import com.example.core.security.SecureSessionManager
import com.example.data.local.AppDatabase
import com.example.data.remote.NetworkClient
import com.example.data.remote.websocket.ChatWebSocketClient
import com.example.data.repository.ChatRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BlockedUsersScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.FindContactsScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.NotificationSettingsScreen
import com.example.ui.screens.PrivacySettingsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.screens.UserSearchScreen
import com.example.ui.theme.ZxBackgroundCharcoal
import com.example.ui.theme.ZxEmeraldBright
import com.example.ui.theme.ZxEmeraldPrimary
import com.example.ui.theme.ZxLiteTheme
import com.example.ui.theme.ZxSurfaceDark
import com.example.ui.theme.ZxTextSecondary
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BlockedUsersViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ConversationsViewModel
import com.example.ui.viewmodel.FindContactsViewModel
import com.example.ui.viewmodel.FriendsViewModel
import com.example.ui.viewmodel.PrivacySettingsViewModel
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.UpdatesViewModel
import com.example.ui.viewmodel.UserSearchViewModel

enum class MainTab {
    CHATS,
    FRIENDS,
    UPDATES,
    SETTINGS
}

sealed class Screen {
    data object Splash : Screen()
    data object Auth : Screen()
    data class Main(val tab: MainTab = MainTab.CHATS) : Screen()
    data object UserSearch : Screen()
    data class Chat(val conversationId: String, val title: String) : Screen()
    data object FindContacts : Screen()
    data object Profile : Screen()
    data object PrivacySettings : Screen()
    data object NotificationSettings : Screen()
    data object BlockedUsers : Screen()
}

@Composable
fun ZxMessengerApp() {
    val context = LocalContext.current.applicationContext

    // Dependencies setup
    val sessionManager = remember { SecureSessionManager(context) }
    val preferencesManager = remember { UserPreferencesManager(context) }
    val networkClient = remember { NetworkClient(sessionManager, preferencesManager) }
    val wsClient = remember {
        ChatWebSocketClient(sessionManager, preferencesManager, networkClient.moshi)
    }
    val repository = remember {
        ChatRepository(
            database = AppDatabase.getInstance(context),
            networkClient = networkClient,
            sessionManager = sessionManager,
            preferencesManager = preferencesManager,
            wsClient = wsClient
        )
    }

    val fontScaleMultiplier by preferencesManager.fontScaleFlow.collectAsState()
    val isDarkTheme by preferencesManager.darkThemeFlow.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var currentMainTab by remember { mutableStateOf(MainTab.CHATS) }

    val conversationsViewModel = remember { ConversationsViewModel(repository) }
    val friendsViewModel = remember { FriendsViewModel(repository) }
    val updatesViewModel = remember { UpdatesViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository) }

    val incomingRequests by friendsViewModel.incomingRequests.collectAsState()

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, fontScaleMultiplier) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScaleMultiplier
        )
    }

    CompositionLocalProvider(LocalDensity provides customDensity) {
        ZxLiteTheme(darkTheme = isDarkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ZxBackgroundCharcoal
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { target ->
                    when (target) {
                        is Screen.Splash -> {
                            SplashScreen(
                                isLoggedIn = sessionManager.isLoggedIn(),
                                onNavigateNext = { loggedIn ->
                                    currentScreen = if (loggedIn) Screen.Main(MainTab.CHATS) else Screen.Auth
                                }
                            )
                        }

                        is Screen.Auth -> {
                            val authViewModel = remember { AuthViewModel(repository) }
                            AuthScreen(
                                viewModel = authViewModel,
                                onAuthSuccess = {
                                    currentMainTab = MainTab.CHATS
                                    currentScreen = Screen.Main(MainTab.CHATS)
                                }
                            )
                        }

                        is Screen.Main -> {
                            BackHandler(enabled = currentMainTab != MainTab.CHATS) {
                                currentMainTab = MainTab.CHATS
                            }

                            Scaffold(
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = ZxSurfaceDark,
                                        contentColor = ZxEmeraldBright
                                    ) {
                                        NavigationBarItem(
                                            selected = currentMainTab == MainTab.CHATS,
                                            onClick = { currentMainTab = MainTab.CHATS },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Chat,
                                                    contentDescription = "Chats"
                                                )
                                            },
                                            label = { Text("Chats") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFF042F22),
                                                selectedTextColor = ZxEmeraldBright,
                                                indicatorColor = ZxEmeraldPrimary,
                                                unselectedIconColor = ZxTextSecondary,
                                                unselectedTextColor = ZxTextSecondary
                                            ),
                                            modifier = Modifier.testTag("nav_chats_tab")
                                        )

                                        NavigationBarItem(
                                            selected = currentMainTab == MainTab.FRIENDS,
                                            onClick = { currentMainTab = MainTab.FRIENDS },
                                            icon = {
                                                if (incomingRequests.isNotEmpty()) {
                                                    BadgedBox(
                                                        badge = {
                                                            Badge(
                                                                containerColor = ZxEmeraldPrimary,
                                                                contentColor = Color(0xFF042F22)
                                                            ) {
                                                                Text(incomingRequests.size.toString())
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Group,
                                                            contentDescription = "Friends"
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Group,
                                                        contentDescription = "Friends"
                                                    )
                                                }
                                            },
                                            label = { Text("Friends") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFF042F22),
                                                selectedTextColor = ZxEmeraldBright,
                                                indicatorColor = ZxEmeraldPrimary,
                                                unselectedIconColor = ZxTextSecondary,
                                                unselectedTextColor = ZxTextSecondary
                                            ),
                                            modifier = Modifier.testTag("nav_friends_tab")
                                        )

                                        NavigationBarItem(
                                            selected = currentMainTab == MainTab.UPDATES,
                                            onClick = { currentMainTab = MainTab.UPDATES },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = "Updates"
                                                )
                                            },
                                            label = { Text("Updates") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFF042F22),
                                                selectedTextColor = ZxEmeraldBright,
                                                indicatorColor = ZxEmeraldPrimary,
                                                unselectedIconColor = ZxTextSecondary,
                                                unselectedTextColor = ZxTextSecondary
                                            ),
                                            modifier = Modifier.testTag("nav_updates_tab")
                                        )

                                        NavigationBarItem(
                                            selected = currentMainTab == MainTab.SETTINGS,
                                            onClick = { currentMainTab = MainTab.SETTINGS },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Settings"
                                                )
                                            },
                                            label = { Text("Settings") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFF042F22),
                                                selectedTextColor = ZxEmeraldBright,
                                                indicatorColor = ZxEmeraldPrimary,
                                                unselectedIconColor = ZxTextSecondary,
                                                unselectedTextColor = ZxTextSecondary
                                            ),
                                            modifier = Modifier.testTag("nav_settings_tab")
                                        )
                                    }
                                },
                                containerColor = ZxBackgroundCharcoal
                            ) { scaffoldPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(scaffoldPadding)
                                ) {
                                    when (currentMainTab) {
                                        MainTab.CHATS -> {
                                            ConversationsScreen(
                                                viewModel = conversationsViewModel,
                                                onOpenChat = { convId, title ->
                                                    currentScreen = Screen.Chat(convId, title)
                                                },
                                                onOpenSearch = {
                                                    currentScreen = Screen.UserSearch
                                                },
                                                onOpenSettings = {
                                                    currentMainTab = MainTab.SETTINGS
                                                },
                                                onOpenProfile = {
                                                    currentScreen = Screen.Profile
                                                }
                                            )
                                        }

                                        MainTab.FRIENDS -> {
                                            FriendsScreen(
                                                viewModel = friendsViewModel,
                                                onOpenChat = { convId, title ->
                                                    currentScreen = Screen.Chat(convId, title)
                                                },
                                                onOpenFindContacts = {
                                                    currentScreen = Screen.FindContacts
                                                }
                                            )
                                        }

                                        MainTab.UPDATES -> {
                                            UpdatesScreen(
                                                viewModel = updatesViewModel
                                            )
                                        }

                                        MainTab.SETTINGS -> {
                                            SettingsScreen(
                                                viewModel = settingsViewModel,
                                                onBack = { currentMainTab = MainTab.CHATS },
                                                onOpenPrivacySettings = {
                                                    currentScreen = Screen.PrivacySettings
                                                },
                                                onOpenNotificationSettings = {
                                                    currentScreen = Screen.NotificationSettings
                                                },
                                                onOpenBlockedUsers = {
                                                    currentScreen = Screen.BlockedUsers
                                                },
                                                onLoggedOut = {
                                                    currentScreen = Screen.Auth
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is Screen.UserSearch -> {
                            BackHandler {
                                currentScreen = Screen.Main(MainTab.CHATS)
                            }
                            val searchViewModel = remember { UserSearchViewModel(repository) }
                            UserSearchScreen(
                                viewModel = searchViewModel,
                                onBack = { currentScreen = Screen.Main(MainTab.CHATS) },
                                onOpenChat = { convId, title ->
                                    currentScreen = Screen.Chat(convId, title)
                                }
                            )
                        }

                        is Screen.Chat -> {
                            BackHandler {
                                currentScreen = Screen.Main(MainTab.CHATS)
                            }
                            val chatViewModel = remember(target.conversationId) {
                                ChatViewModel(
                                    conversationId = target.conversationId,
                                    peerTitle = target.title,
                                    repository = repository
                                )
                            }
                            ChatScreen(
                                viewModel = chatViewModel,
                                onBack = { currentScreen = Screen.Main(MainTab.CHATS) }
                            )
                        }

                        is Screen.FindContacts -> {
                            BackHandler {
                                currentScreen = Screen.Main(MainTab.FRIENDS)
                            }
                            val findContactsViewModel = remember { FindContactsViewModel(repository) }
                            FindContactsScreen(
                                viewModel = findContactsViewModel,
                                onBack = { currentScreen = Screen.Main(MainTab.FRIENDS) }
                            )
                        }

                        is Screen.Profile -> {
                            BackHandler {
                                currentScreen = Screen.Main(currentMainTab)
                            }
                            val profileViewModel = remember { ProfileViewModel(repository) }
                            ProfileScreen(
                                viewModel = profileViewModel,
                                fallbackUsername = sessionManager.getUsername() ?: "user",
                                fallbackDisplayName = sessionManager.getDisplayName() ?: "ZX User",
                                fallbackUserId = sessionManager.getUserId() ?: "unknown",
                                onBack = { currentScreen = Screen.Main(currentMainTab) },
                                onLogout = {
                                    settingsViewModel.logout {
                                        currentScreen = Screen.Auth
                                    }
                                }
                            )
                        }

                        is Screen.PrivacySettings -> {
                            BackHandler {
                                currentScreen = Screen.Main(MainTab.SETTINGS)
                            }
                            val privacyViewModel = remember { PrivacySettingsViewModel(repository) }
                            PrivacySettingsScreen(
                                viewModel = privacyViewModel,
                                onBack = { currentScreen = Screen.Main(MainTab.SETTINGS) },
                                onOpenBlockedUsers = { currentScreen = Screen.BlockedUsers }
                            )
                        }

                        is Screen.NotificationSettings -> {
                            BackHandler {
                                currentScreen = Screen.Main(MainTab.SETTINGS)
                            }
                            NotificationSettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { currentScreen = Screen.Main(MainTab.SETTINGS) }
                            )
                        }

                        is Screen.BlockedUsers -> {
                            BackHandler {
                                currentScreen = Screen.PrivacySettings
                            }
                            val blockedViewModel = remember { BlockedUsersViewModel(repository) }
                            BlockedUsersScreen(
                                viewModel = blockedViewModel,
                                onBack = { currentScreen = Screen.PrivacySettings }
                            )
                        }
                    }
                }
            }
        }
    }
}
