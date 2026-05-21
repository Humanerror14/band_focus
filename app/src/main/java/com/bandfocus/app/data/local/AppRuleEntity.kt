package com.bandfocus.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlockedInFocusMode: Boolean,
    val isWhitelisted: Boolean,
    val updatedAt: Long
)
