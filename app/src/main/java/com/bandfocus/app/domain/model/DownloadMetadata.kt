package com.bandfocus.app.domain.model

data class DownloadMetadata(
    val url: String,
    val fileName: String,
    val fileSize: Long?,
    val mimeType: String?,
    val supportsRange: Boolean,
    val recommendedMode: DownloadMode,
    val diagnosis: String
)
