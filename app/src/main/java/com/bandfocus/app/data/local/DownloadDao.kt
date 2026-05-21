package com.bandfocus.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadEntity?

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM downloads WHERE status = 'COMPLETED'")
    fun observeTotalDownloadedBytes(): Flow<Long>

    @Query("SELECT COALESCE(AVG(averageSpeed), 0) FROM downloads WHERE averageSpeed > 0")
    fun observeAverageSpeed(): Flow<Long>

    @Query("SELECT COUNT(*) FROM downloads")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>
}
