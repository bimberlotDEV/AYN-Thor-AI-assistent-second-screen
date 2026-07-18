package com.gameside.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gameside.data.ai.DeepSeekTextAiProvider
import com.gameside.data.chat.RoomChatRepository
import com.gameside.data.database.ChatDao
import com.gameside.data.database.GameProfileDao
import com.gameside.data.database.GameSideDatabase
import com.gameside.data.game.RoomGameProfileRepository
import com.gameside.data.security.KeystoreCredentialStore
import com.gameside.data.settings.DataStoreSettingsRepository
import com.gameside.domain.game.GameProfileRepository
import com.gameside.domain.ai.TextAiProvider
import com.gameside.domain.chat.ChatRepository
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
    @Binds @Singleton abstract fun bindChatRepository(value: RoomChatRepository): ChatRepository
    @Binds @Singleton abstract fun bindTextAiProvider(value: DeepSeekTextAiProvider): TextAiProvider
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GameSideDatabase =
        Room.databaseBuilder(context, GameSideDatabase::class.java, "gameside.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides
    fun provideGameProfileDao(database: GameSideDatabase): GameProfileDao = database.gameProfileDao()

    @Provides
    fun provideChatDao(database: GameSideDatabase): ChatDao = database.chatDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `chat_sessions` (`id` TEXT NOT NULL, `gameProfileId` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`gameProfileId`) REFERENCES `game_profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_sessions_gameProfileId` ON `chat_sessions` (`gameProfileId`)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `chat_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` ON `chat_messages` (`sessionId`)")
        }
    }
}
