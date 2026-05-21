package com.bandfocus.app.domain.repository

import com.bandfocus.app.domain.model.AppRule
import kotlinx.coroutines.flow.Flow

interface AppRuleRepository {
    fun observeAll(): Flow<List<AppRule>>
    suspend fun upsert(rule: AppRule)
    suspend fun getByPackageName(packageName: String): AppRule?
    suspend fun deleteAll()
}
