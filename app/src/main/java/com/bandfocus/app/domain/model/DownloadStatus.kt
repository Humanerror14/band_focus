package com.bandfocus.app.domain.model

enum class DownloadStatus {
    QUEUED,
    ANALYZING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED
}
