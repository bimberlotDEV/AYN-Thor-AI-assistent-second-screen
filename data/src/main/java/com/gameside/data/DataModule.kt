package com.gameside.data

import android.content.Context
import androidx.room.Room
import com.gameside.data.database.GameProfileDao
import com.gameside.data.database.GameSideDatabase
import com.gameside.data.game.RoomGameProfileRepository
import com.gameside.data.security.KeystoreCredentialStore
import com.gameside.data.settings.DataStoreSettingsRepository
import com.gameside.domain.game.GameProfileRepository
import com.gameside.domain.security.CredentialStore
import com.gameside.domain.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds @Singleton abstract fun bindGameRepository(value: RoomGameProfileRepository): GameProfileRepository
    @Binds @Singleton abstract fun bindSettingsRepository(value: DataStoreSettingsRepository): SettingsRepository
    @Binds @Singleton abstract fun bindCredentialStore(value: KeystoreCredentialStore): CredentialStore
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GameSideDatabase =
        Room.databaseBuilder(context, GameSideDatabase::class.java, "gameside.db")
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides
    fun provideGameProfileDao(database: GameSideDatabase): GameProfileDao = database.gameProfileDao()
}
