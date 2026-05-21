package com.bandfocus.app.domain.model

data class DownloadTask(
    val id: String,
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
