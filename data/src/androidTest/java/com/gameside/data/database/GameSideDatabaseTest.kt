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

        chatDao.insertSession(ChatSessionEntity("second-session", profile.id, "Second run", 5L, 5L))
        assertEquals(listOf("Second run", "Chat Game"), chatDao.observeSessions(profile.id).first().map { it.title })
        chatDao.renameSession("second-session", "Boss route", 6L)
        assertEquals("Boss route", chatDao.observeThread("second-session").first()?.session?.title)
        chatDao.deleteSession("second-session")
        assertEquals(listOf("session"), chatDao.observeSessions(profile.id).first().map { it.id })

        dao.delete(profile.id)
        assertEquals(null, chatDao.observeLatestThread(profile.id).first())
    }

    @Test
    fun personalToolsPersistOfflineAndCascadeWithGame() = runBlocking {
        val profile = GameProfileEntity(
            id = "tools-game", title = "Tools Game", platform = "OTHER", coverImageUri = null,
            spoilerLevel = "MINIMAL", currentArea = null, currentChapter = null, currentQuest = null,
            customContext = null, customSystemPrompt = null, isPinned = false, isArchived = false,
            createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L,
        )
        dao.upsert(GameProfileWithRelations(profile, emptyList(), emptyList()))
        val tools = database.personalToolsDao()
        tools.upsertSavedAnswer(SavedAnswerEntity("saved", profile.id, "message", "Question", "Answer", "[]", 2L))
        tools.upsertNote(GameNoteEntity("note", profile.id, "Route", "Go north", 2L, 2L))
        tools.upsertChecklist(
            GameChecklistEntity("list", profile.id, "Items", 2L, 2L),
            listOf(ChecklistItemEntity("item", "list", "Find key", false, 0)),
        )

        assertEquals("Answer", tools.observeSavedAnswers(profile.id).first().single().answer)
        assertEquals("Go north", tools.observeNotes(profile.id).first().single().content)
        tools.setItemChecked("item", true)
        assertEquals(true, tools.observeChecklists(profile.id).first().single().items.single().isChecked)

        dao.delete(profile.id)
        assertEquals(emptyList<SavedAnswerEntity>(), tools.observeSavedAnswers(profile.id).first())
        assertEquals(emptyList<GameNoteEntity>(), tools.observeNotes(profile.id).first())
        assertEquals(emptyList<ChecklistWithItems>(), tools.observeChecklists(profile.id).first())
    }

    @Test
    fun knowledgeCachePersistsClearsAndCascades() = runBlocking {
        val profile = GameProfileEntity(
            id = "cache-game", title = "Cache Game", platform = "OTHER", coverImageUri = null,
            spoilerLevel = "NONE", currentArea = null, currentChapter = null, currentQuest = null,
            customContext = null, customSystemPrompt = null, isPinned = false, isArchived = false,
            createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L,
        )
        dao.upsert(GameProfileWithRelations(profile, emptyList(), emptyList()))
        val cache = database.knowledgeCacheDao()
        val page = KnowledgeCacheEntity(profile.id, "https://game.wiki/api.php", "42", "Item", "Game Wiki", "https://game.wiki/index.php?curid=42", "Cached guide", 2L)
        cache.upsert(page)
        assertEquals("Cached guide", cache.get(profile.id, page.sourceApiUrl, page.pageId)?.plainText)
        assertEquals(1, cache.observe(profile.id).first().size)
        cache.clear(profile.id)
        assertEquals(0, cache.observe(profile.id).first().size)
        cache.upsert(page)
        dao.delete(profile.id)
        assertEquals(0, cache.observe(profile.id).first().size)
    }

    @Test
    fun privacyControlsClearCategoriesWithoutDeletingGame() = runBlocking {
        val profile = GameProfileEntity(
            id = "privacy-game", title = "Privacy Game", platform = "OTHER", coverImageUri = null,
            spoilerLevel = "NONE", currentArea = null, currentChapter = null, currentQuest = null,
            customContext = null, customSystemPrompt = null, isPinned = false, isArchived = false,
            createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L,
        )
        dao.upsert(GameProfileWithRelations(profile, emptyList(), emptyList()))
        database.chatDao().insertSession(ChatSessionEntity("privacy-session", profile.id, profile.title, 2L, 2L))
        database.personalToolsDao().upsertSavedAnswer(SavedAnswerEntity("privacy-answer", profile.id, "privacy-message", "Q", "A", "[]", 2L))
        database.personalToolsDao().upsertNote(GameNoteEntity("privacy-note", profile.id, "Title", "Text", 2L, 2L))
        database.personalToolsDao().upsertChecklist(GameChecklistEntity("privacy-list", profile.id, "List", 2L, 2L), emptyList())
        database.knowledgeCacheDao().upsert(KnowledgeCacheEntity(profile.id, "https://game.wiki/api.php", "1", "Page", "Wiki", "https://game.wiki/page", "Text", 2L))
        val privacy = database.privacyDao()

        assertEquals(1, privacy.gameCount().first())
        assertEquals(1, privacy.conversationCount().first())
        assertEquals(1, privacy.savedAnswerCount().first())
        assertEquals(1, privacy.noteCount().first())
        assertEquals(1, privacy.checklistCount().first())
        assertEquals(1, privacy.wikiPageCount().first())

        privacy.clearConversations()
        privacy.clearPersonalTools()
        privacy.clearWikiCache()

        assertEquals(1, privacy.gameCount().first())
        assertEquals(0, privacy.conversationCount().first())
        assertEquals(0, privacy.savedAnswerCount().first())
        assertEquals(0, privacy.noteCount().first())
        assertEquals(0, privacy.checklistCount().first())
        assertEquals(0, privacy.wikiPageCount().first())
    }

    @Test
    fun backupMergeUpdatesProfileWithoutCascadingExistingData() = runBlocking {
        val original = GameProfileEntity(
            id = "backup-game", title = "Old title", platform = "OTHER", coverImageUri = null,
            spoilerLevel = "NONE", currentArea = null, currentChapter = null, currentQuest = null,
            customContext = null, customSystemPrompt = null, isPinned = false, isArchived = false,
            createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L,
        )
        dao.upsert(GameProfileWithRelations(original, emptyList(), emptyList()))
        database.chatDao().insertSession(ChatSessionEntity("existing-session", original.id, original.title, 2L, 2L))
        val imported = original.copy(title = "Imported title", updatedAtEpochMillis = 3L)

        database.backupDao().importData(
            BackupData(
                games = listOf(imported), packages = emptyList(), wikiSources = emptyList(), sessions = emptyList(),
                messages = emptyList(), citations = emptyList(), savedAnswers = emptyList(), notes = emptyList(),
                checklists = emptyList(), checklistItems = emptyList(),
            ),
        )

        assertEquals("Imported title", dao.observeById(original.id).first()?.profile?.title)
        assertEquals("existing-session", database.chatDao().observeLatestThread(original.id).first()?.session?.id)
    }
}
