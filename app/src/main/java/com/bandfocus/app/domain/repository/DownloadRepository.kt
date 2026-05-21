package com.bandfocus.app.domain.repository

import com.bandfocus.app.domain.model.DownloadTask
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeAll(): Flow<List<DownloadTask>>
    suspend fun upsert(download: DownloadTask)
    suspend fun getById(id: String): DownloadTask?
    suspend fun deleteById(id: String)
    fun observeTotalDownloadedBytes(): Flow<Long>
    fun observeAverageSpeed(): Flow<Long>
    fun observeTotalCount(): Flow<Int>
    fun observeCompletedCount(): Flow<Int>
}
