package com.bandfocus.app.domain.model

data class AppRule(
    val packageName: String,
    val appName: String,
    val isBlockedInFocusMode: Boolean,
    val isWhitelisted: Boolean,
    val updatedAt: Long
)
