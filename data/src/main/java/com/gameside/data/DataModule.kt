package com.gameside.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gameside.data.ai.DeepSeekTextAiProvider
import com.gameside.data.backup.JsonBackupRepository
import com.gameside.data.chat.RoomChatRepository
import com.gameside.data.database.ChatDao
import com.gameside.data.database.GameProfileDao
import com.gameside.data.database.GameSideDatabase
import com.gameside.data.database.PersonalToolsDao
import com.gameside.data.database.KnowledgeCacheDao
import com.gameside.data.game.RoomGameProfileRepository
import com.gameside.data.knowledge.CachingGameKnowledgeProvider
import com.gameside.data.personal.RoomPersonalToolsRepository
import com.gameside.data.privacy.RoomPrivacyRepository
import com.gameside.data.security.KeystoreCredentialStore
import com.gameside.data.settings.DataStoreSettingsRepository
import com.gameside.domain.game.GameProfileRepository
import com.gameside.domain.backup.BackupRepository
import com.gameside.domain.knowledge.GameKnowledgeProvider
import com.gameside.domain.knowledge.KnowledgeRetriever
import com.gameside.domain.knowledge.KnowledgeCacheRepository
import com.gameside.domain.personal.PersonalToolsRepository
import com.gameside.domain.privacy.PrivacyRepository
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
    @Binds @Singleton abstract fun bindKnowledgeProvider(value: CachingGameKnowledgeProvider): GameKnowledgeProvider
    @Binds @Singleton abstract fun bindKnowledgeCache(value: CachingGameKnowledgeProvider): KnowledgeCacheRepository
    @Binds @Singleton abstract fun bindPersonalToolsRepository(value: RoomPersonalToolsRepository): PersonalToolsRepository
    @Binds @Singleton abstract fun bindPrivacyRepository(value: RoomPrivacyRepository): PrivacyRepository
    @Binds @Singleton abstract fun bindBackupRepository(value: JsonBackupRepository): BackupRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GameSideDatabase =
        Room.databaseBuilder(context, GameSideDatabase::class.java, "gameside.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides
    fun provideGameProfileDao(database: GameSideDatabase): GameProfileDao = database.gameProfileDao()

    @Provides
    fun provideChatDao(database: GameSideDatabase): ChatDao = database.chatDao()

    @Provides
    fun providePersonalToolsDao(database: GameSideDatabase): PersonalToolsDao = database.personalToolsDao()

    @Provides
    fun provideKnowledgeCacheDao(database: GameSideDatabase): KnowledgeCacheDao = database.knowledgeCacheDao()

    @Provides
    fun providePrivacyDao(database: GameSideDatabase): com.gameside.data.database.PrivacyDao = database.privacyDao()

    @Provides
    fun provideBackupDao(database: GameSideDatabase): com.gameside.data.database.BackupDao = database.backupDao()

    @Provides
    @Singleton
    fun provideKnowledgeRetriever(provider: GameKnowledgeProvider): KnowledgeRetriever = KnowledgeRetriever(provider)

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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `source_citations` (`messageId` TEXT NOT NULL, `position` INTEGER NOT NULL, `title` TEXT NOT NULL, `sourceName` TEXT NOT NULL, `url` TEXT NOT NULL, `excerpt` TEXT NOT NULL, `retrievedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`messageId`, `position`), FOREIGN KEY(`messageId`) REFERENCES `chat_messages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_citations_messageId` ON `source_citations` (`messageId`)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `saved_answers` (`id` TEXT NOT NULL, `gameProfileId` TEXT NOT NULL, `sourceMessageId` TEXT NOT NULL, `question` TEXT NOT NULL, `answer` TEXT NOT NULL, `citationsJson` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`gameProfileId`) REFERENCES `game_profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_answers_gameProfileId` ON `saved_answers` (`gameProfileId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_answers_sourceMessageId` ON `saved_answers` (`sourceMessageId`)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `game_notes` (`id` TEXT NOT NULL, `gameProfileId` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`gameProfileId`) REFERENCES `game_profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_notes_gameProfileId` ON `game_notes` (`gameProfileId`)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `game_checklists` (`id` TEXT NOT NULL, `gameProfileId` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`gameProfileId`) REFERENCES `game_profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_checklists_gameProfileId` ON `game_checklists` (`gameProfileId`)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `checklist_items` (`id` TEXT NOT NULL, `checklistId` TEXT NOT NULL, `text` TEXT NOT NULL, `isChecked` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`checklistId`) REFERENCES `game_checklists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_checklistId` ON `checklist_items` (`checklistId`)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `knowledge_cache` (`gameProfileId` TEXT NOT NULL, `sourceApiUrl` TEXT NOT NULL, `pageId` TEXT NOT NULL, `title` TEXT NOT NULL, `sourceName` TEXT NOT NULL, `url` TEXT NOT NULL, `plainText` TEXT NOT NULL, `retrievedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`gameProfileId`, `sourceApiUrl`, `pageId`), FOREIGN KEY(`gameProfileId`) REFERENCES `game_profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_cache_gameProfileId` ON `knowledge_cache` (`gameProfileId`)")
        }
    }
}
