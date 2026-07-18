package com.gameside.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameSideDatabaseTest {
    private lateinit var database: GameSideDatabase
    private lateinit var dao: GameProfileDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GameSideDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.gameProfileDao()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun upsertReplacesRelationsAndDeleteCascades() = runBlocking {
        val profile = GameProfileEntity(
            id = "elden-ring",
            title = "Elden Ring",
            platform = "ANDROID",
            coverImageUri = null,
            spoilerLevel = "MINIMAL",
            currentArea = null,
            currentChapter = null,
            currentQuest = null,
            customContext = null,
            customSystemPrompt = null,
            isPinned = true,
            isArchived = false,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
        )
        dao.upsert(
            GameProfileWithRelations(
                profile,
                packages = listOf(GamePackageEntity(profile.id, "com.example.game")),
                wikiSources = listOf(GameWikiSourceEntity(profile.id, "https://example.org/api.php")),
            ),
        )

        val stored = dao.observeById(profile.id).first()
        assertEquals("Elden Ring", stored?.profile?.title)
        assertEquals(1, stored?.packages?.size)

        dao.delete(profile.id)
        assertEquals(0, dao.packageCount(profile.id))
        assertEquals(null, dao.observeById(profile.id).first())
    }

    @Test
    fun chatMessagesPersistInOrderAndCascadeWithGame() = runBlocking {
        val profile = GameProfileEntity(
            id = "chat-game", title = "Chat Game", platform = "EMULATED", coverImageUri = null,
            spoilerLevel = "NONE", currentArea = null, currentChapter = null, currentQuest = null,
            customContext = null, customSystemPrompt = null, isPinned = false, isArchived = false,
            createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L,
        )
        dao.upsert(GameProfileWithRelations(profile, emptyList(), emptyList()))
        val chatDao = database.chatDao()
        chatDao.insertSession(ChatSessionEntity("session", profile.id, profile.title, 2L, 2L))
        chatDao.insertMessage(ChatMessageWithCitations(ChatMessageEntity("user", "session", "USER", "Question", 3L), emptyList()))
        chatDao.insertMessage(
            ChatMessageWithCitations(
                ChatMessageEntity("assistant", "session", "ASSISTANT", "Answer", 4L),
                listOf(SourceCitationEntity("assistant", 0, "Guide", "Wikipedia", "https://example.org", "Excerpt", 4L)),
            ),
        )

        val thread = chatDao.observeLatestThread(profile.id).first()
        assertEquals(listOf("Question", "Answer"), thread?.messages?.sortedBy { it.message.createdAtEpochMillis }?.map { it.message.content })
        assertEquals("Guide", thread?.messages?.last()?.citations?.single()?.title)

        dao.delete(profile.id)
        assertEquals(null, chatDao.observeLatestThread(profile.id).first())
    }
}
