package com.example.core.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    private val _serverUrlFlow = MutableStateFlow(getServerUrl())
    val serverUrlFlow: StateFlow<String> = _serverUrlFlow.asStateFlow()

    private val _fontScaleFlow = MutableStateFlow(getFontScale())
    val fontScaleFlow: StateFlow<Float> = _fontScaleFlow.asStateFlow()

    private val _darkThemeFlow = MutableStateFlow(isDarkTheme())
    val darkThemeFlow: StateFlow<Boolean> = _darkThemeFlow.asStateFlow()

    private val _notificationsFlow = MutableStateFlow(isNotificationsEnabled())
    val notificationsFlow: StateFlow<Boolean> = _notificationsFlow.asStateFlow()

    companion object {
        private const val PREF_FILE_NAME = "zx_user_settings_prefs"
        private const val KEY_SERVER_URL = "setting_server_url"
        private const val KEY_FONT_SCALE = "setting_font_scale"
        private const val KEY_DARK_THEME = "setting_dark_theme"
        private const val KEY_NOTIFICATIONS = "setting_notifications"
        private const val KEY_DEMO_MODE = "setting_demo_mode"

        const val DEFAULT_SERVER_URL = "http://10.0.2.2:3000"
        const val FONT_SCALE_SMALL = 0.85f
        const val FONT_SCALE_MEDIUM = 1.0f
        const val FONT_SCALE_LARGE = 1.15f
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        val cleanUrl = if (url.endsWith("/")) url.dropLast(1) else url
        prefs.edit().putString(KEY_SERVER_URL, cleanUrl).apply()
        _serverUrlFlow.value = cleanUrl
    }

    fun getFontScale(): Float {
        return prefs.getFloat(KEY_FONT_SCALE, FONT_SCALE_MEDIUM)
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
        _fontScaleFlow.value = scale
    }

    fun isDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _darkThemeFlow.value = enabled
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _notificationsFlow.value = enabled
    }

    fun isDemoGatewayEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEMO_MODE, false)
    }

    fun setDemoGatewayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_MODE, enabled).apply()
    }
}
