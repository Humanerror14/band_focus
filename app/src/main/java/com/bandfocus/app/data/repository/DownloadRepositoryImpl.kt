package com.bandfocus.app.data.repository

import com.bandfocus.app.data.local.AppRuleDao
import com.bandfocus.app.data.local.AppRuleEntity
import com.bandfocus.app.data.local.DownloadDao
import com.bandfocus.app.data.local.DownloadEntity
import com.bandfocus.app.domain.model.AppRule
import com.bandfocus.app.domain.model.DownloadMode
import com.bandfocus.app.domain.model.DownloadStatus
import com.bandfocus.app.domain.repository.AppRuleRepository
import com.bandfocus.app.domain.repository.DownloadRepository
import com.bandfocus.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao
) : DownloadRepository {
    override fun observeAll(): Flow<List<com.bandfocus.app.domain.model.DownloadTask>> =
        downloadDao.observeAll().map { list ->
            list.map { it.toDomainModel() }
        }

    override suspend fun upsert(download: com.bandfocus.app.domain.model.DownloadTask) {
        downloadDao.upsert(download.toEntity())
    }

    override suspend fun getById(id: String): com.bandfocus.app.domain.model.DownloadTask? =
        downloadDao.getById(id)?.toDomainModel()

    override suspend fun deleteById(id: String) = downloadDao.deleteById(id)

    override fun observeTotalDownloadedBytes(): Flow<Long> = downloadDao.observeTotalDownloadedBytes()

    override fun observeAverageSpeed(): Flow<Long> = downloadDao.observeAverageSpeed()

    override fun observeTotalCount(): Flow<Int> = downloadDao.observeTotalCount()

    override fun observeCompletedCount(): Flow<Int> = downloadDao.observeCompletedCount()
}

fun DownloadEntity.toDomainModel(): com.bandfocus.app.domain.model.DownloadTask = com.bandfocus.app.domain.model.DownloadTask(
    id = id,
    url = url,
    fileName = fileName,
    fileSize = fileSize,
    downloadedBytes = downloadedBytes,
    savedPath = savedPath,
    status = status,
    mode = mode,
    threadCount = threadCount,
    averageSpeed = averageSpeed,
    createdAt = createdAt,
    updatedAt = updatedAt,
    supportsRange = supportsRange
)

fun com.bandfocus.app.domain.model.DownloadTask.toEntity(): DownloadEntity = DownloadEntity(
    id = id,
    url = url,
    fileName = fileName,
    fileSize = fileSize,
    downloadedBytes = downloadedBytes,
    savedPath = savedPath,
    status = status,
    mode = mode,
    threadCount = threadCount,
    averageSpeed = averageSpeed,
    createdAt = createdAt,
    updatedAt = updatedAt,
    supportsRange = supportsRange
)
