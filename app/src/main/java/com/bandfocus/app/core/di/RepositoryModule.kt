package com.bandfocus.app.core.di

import com.bandfocus.app.data.repository.AppRuleRepositoryImpl
import com.bandfocus.app.data.repository.DownloadRepositoryImpl
import com.bandfocus.app.data.repository.PreferencesRepositoryImpl
import com.bandfocus.app.domain.repository.AppRuleRepository
import com.bandfocus.app.domain.repository.DownloadRepository
import com.bandfocus.app.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindAppRuleRepository(impl: AppRuleRepositoryImpl): AppRuleRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
