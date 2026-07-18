package com.gameside.domain.knowledge

import com.gameside.domain.game.GamePlatform
import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.SpoilerLevel
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class KnowledgeRetrieverTest {
    @Test
    fun ranksRelevantDocumentsAndBuildsNumberedEvidence() = runTest {
        val provider = FakeProvider()
        val result = KnowledgeRetriever(provider).retrieve(game(), "Where is Moonveil?", limit = 1)

        assertEquals("Moonveil", result.citations.single().title)
        assertTrue(result.context.startsWith("[1] Moonveil"))
        assertTrue(result.context.contains("Gael Tunnel"))
    }

    @Test
    fun strictSpoilerModeLimitsEvidenceCountAndExcerptLength() = runTest {
        val base = KnowledgeSearchResult("1", "elden", "Moonveil", "Test", "https://example.test/api.php", "https://example.test/1", "Katana")
        val provider = object : GameKnowledgeProvider {
            override suspend fun search(game: GameProfile, query: String) = listOf(base, base.copy(id = "2", title = "Moonveil guide"))
            override suspend fun retrieve(result: KnowledgeSearchResult) = KnowledgeDocument(result, "Moonveil " + "detail ".repeat(200), Instant.EPOCH)
        }

        val result = KnowledgeRetriever(provider).retrieve(game().copy(spoilerLevel = SpoilerLevel.NONE), "Moonveil")

        assertEquals(1, result.citations.size)
        assertEquals(400, result.citations.single().excerpt.length)
    }

    private fun game() = GameProfile(
        id = "elden", title = "Elden Ring", packageNames = emptyList(), platform = GamePlatform.OTHER,
        coverImageUri = null, preferredWikiSources = emptyList(), spoilerLevel = SpoilerLevel.MINIMAL,
        playerProgress = null, customSystemPrompt = null, isPinned = false, isArchived = false,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )

    private class FakeProvider : GameKnowledgeProvider {
        private val results = listOf(
            KnowledgeSearchResult("1", "elden", "Unrelated", "Test", "https://example.test/api.php", "https://example.test/1", "Lore"),
            KnowledgeSearchResult("2", "elden", "Moonveil", "Test", "https://example.test/api.php", "https://example.test/2", "Katana"),
        )
        override suspend fun search(game: GameProfile, query: String) = results
        override suspend fun retrieve(result: KnowledgeSearchResult) = KnowledgeDocument(
            result, if (result.id == "2") "Moonveil is obtained in Gael Tunnel." else "An unrelated character.", Instant.EPOCH,
        )
    }
}
