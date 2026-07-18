package com.gameside.domain.knowledge

import com.gameside.domain.game.GameProfile
import java.time.Instant

data class KnowledgeSearchResult(
    val id: String,
    val title: String,
    val sourceName: String,
    val sourceApiUrl: String,
    val url: String,
    val snippet: String,
)

data class KnowledgeDocument(
    val result: KnowledgeSearchResult,
    val plainText: String,
    val retrievedAt: Instant,
)

data class SourceCitation(
    val title: String,
    val sourceName: String,
    val url: String,
    val excerpt: String,
    val retrievedAt: Instant,
)

data class RetrievedKnowledge(
    val context: String,
    val citations: List<SourceCitation>,
) {
    companion object { val Empty = RetrievedKnowledge("", emptyList()) }
}

interface GameKnowledgeProvider {
    suspend fun search(game: GameProfile, query: String): List<KnowledgeSearchResult>
    suspend fun retrieve(result: KnowledgeSearchResult): KnowledgeDocument
}
