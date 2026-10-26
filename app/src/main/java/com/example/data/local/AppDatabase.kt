package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BlockedUserDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.FriendDao
import com.example.data.local.dao.FriendRequestDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.OfflineQueueDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.UserStatusDao
import com.example.data.local.entity.BlockedUserEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.FriendRequestEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.OfflineQueueEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserStatusEntity

@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        OfflineQueueEntity::class,
        FriendEntity::class,
        FriendRequestEntity::class,
        BlockedUserEntity::class,
        UserStatusEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun offlineQueueDao(): OfflineQueueDao
    abstract fun friendDao(): FriendDao
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun blockedUserDao(): BlockedUserDao
    abstract fun userStatusDao(): UserStatusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zx_lite_messenger.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
