package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.OfflineQueueEntity

@Dao
interface OfflineQueueDao {

    @Query("SELECT * FROM offline_queue ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<OfflineQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OfflineQueueEntity)

    @Query("DELETE FROM offline_queue WHERE idempotencyKey = :idempotencyKey")
    suspend fun deleteByIdempotencyKey(idempotencyKey: String)

    @Query("DELETE FROM offline_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE offline_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)

    @Query("DELETE FROM offline_queue")
    suspend fun clearAll()
}
