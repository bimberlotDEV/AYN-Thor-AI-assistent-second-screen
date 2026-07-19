package com.gameside.data.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gameside.data.DatabaseModule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration5To6Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GameSideDatabase::class.java,
    )

    @Test fun migrationKeepsGamesAndCreatesQuickFavorites() {
        helper.createDatabase(DB_NAME, 5).apply {
            insertGame(this)
            close()
        }
        helper.runMigrationsAndValidate(DB_NAME, 6, true, DatabaseModule.MIGRATION_5_6).use { db ->
            db.query("SELECT title FROM game_profiles WHERE id = 'migration-game'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Migration Game", cursor.getString(0))
            }
            db.query("SELECT COUNT(*) FROM quick_question_favorites").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DB_NAME)
    }

    private fun insertGame(db: SupportSQLiteDatabase) {
        db.execSQL(
            """INSERT INTO game_profiles (id,title,platform,coverImageUri,spoilerLevel,currentArea,currentChapter,currentQuest,customContext,customSystemPrompt,isPinned,isArchived,createdAtEpochMillis,updatedAtEpochMillis) VALUES ('migration-game','Migration Game','OTHER',NULL,'MINIMAL',NULL,NULL,NULL,NULL,NULL,0,0,1,1)""",
        )
    }

    private companion object { const val DB_NAME = "migration-5-6-test" }
}
