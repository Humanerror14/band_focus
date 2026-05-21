package com.bandfocus.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bandfocus.app.domain.model.DownloadMode
import com.bandfocus.app.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val downloadedBytes: Long,
    val savedPath: String?,
    val status: DownloadStatus,
    val mode: DownloadMode,
    val threadCount: Int,
    val averageSpeed: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val supportsRange: Boolean
)
