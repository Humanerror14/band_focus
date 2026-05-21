package com.bandfocus.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DownloadEntity::class,
        AppRuleEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BandFocusDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun appRuleDao(): AppRuleDao
}
