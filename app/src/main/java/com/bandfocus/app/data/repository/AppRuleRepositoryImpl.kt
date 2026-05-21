package com.bandfocus.app.data.repository

import com.bandfocus.app.data.local.AppRuleDao
import com.bandfocus.app.data.local.AppRuleEntity
import com.bandfocus.app.domain.model.AppRule
import com.bandfocus.app.domain.repository.AppRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppRuleRepositoryImpl @Inject constructor(
    private val appRuleDao: AppRuleDao
) : AppRuleRepository {
    override fun observeAll(): Flow<List<AppRule>> =
        appRuleDao.observeAll().map { list ->
            list.map { it.toDomainModel() }
        }

    override suspend fun upsert(rule: AppRule) {
        appRuleDao.upsert(rule.toEntity())
    }

    override suspend fun getByPackageName(packageName: String): AppRule? =
        appRuleDao.getByPackageName(packageName)?.toDomainModel()

    override suspend fun deleteAll() = appRuleDao.deleteAll()
}

fun AppRuleEntity.toDomainModel(): AppRule = AppRule(
    packageName = packageName,
    appName = appName,
    isBlockedInFocusMode = isBlockedInFocusMode,
    isWhitelisted = isWhitelisted,
    updatedAt = updatedAt
)

fun AppRule.toEntity(): AppRuleEntity = AppRuleEntity(
    packageName = packageName,
    appName = appName,
    isBlockedInFocusMode = isBlockedInFocusMode,
    isWhitelisted = isWhitelisted,
    updatedAt = updatedAt
)
