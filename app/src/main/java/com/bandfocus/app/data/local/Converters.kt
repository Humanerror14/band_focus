package com.bandfocus.app.data.local

import androidx.room.TypeConverter
import com.bandfocus.app.domain.model.DownloadMode
import com.bandfocus.app.domain.model.DownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadMode(value: DownloadMode): String = value.name

    @TypeConverter
    fun toDownloadMode(value: String): DownloadMode = DownloadMode.valueOf(value)

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}
