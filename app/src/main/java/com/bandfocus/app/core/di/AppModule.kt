package com.bandfocus.app.core.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.bandfocus.app.data.local.BandFocusDatabase
import okhttp3.OkHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BandFocusDatabase {
        return Room.databaseBuilder(
            context,
            BandFocusDatabase::class.java,
            "bandfocus.db"
        ).build()
    }

    @Provides
    fun provideDownloadDao(db: BandFocusDatabase) = db.downloadDao()

    @Provides
    fun provideAppRuleDao(db: BandFocusDatabase) = db.appRuleDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context) =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("bandfocus_prefs") }
        )
}
