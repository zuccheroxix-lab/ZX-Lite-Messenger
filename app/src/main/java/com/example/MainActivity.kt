package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.ZxMessengerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannels()
        enableEdgeToEdge()
        setContent {
            ZxMessengerApp()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messagesChannel = NotificationChannel(
                "zx_messages",
                "ZX Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct chat messages and realtime conversations"
                enableVibration(true)
            }

            val friendsChannel = NotificationChannel(
                "zx_friend_requests",
                "ZX Friend Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Incoming friend requests and friendship confirmations"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannels(listOf(messagesChannel, friendsChannel))
        }
    }
}

