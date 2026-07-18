package com.gameside.domain.knowledge

import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.SpoilerLevel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class KnowledgeRetriever(private val provider: GameKnowledgeProvider) {
    suspend fun retrieve(game: GameProfile, question: String, limit: Int = 3): RetrievedKnowledge = coroutineScope {
        val queryTerms = tokens("${game.title} $question")
        val questionTerms = tokens(question).filterNot(STOP_WORDS::contains).toSet()
        val candidates = provider.search(game, question)
            .take(SEARCH_LIMIT)
            .map { result -> async { runCatching { provider.retrieve(result) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()
            .filter { it.plainText.isNotBlank() }
            .filter { document ->
                questionTerms.isEmpty() || tokens("${document.result.title} ${document.plainText}").any(questionTerms::contains)
            }
            .map { document -> document to score(document, game, queryTerms, questionTerms) }
            .sortedByDescending { it.second }
        val bestScore = candidates.firstOrNull()?.second ?: 0
        val effectiveLimit = if (game.spoilerLevel == SpoilerLevel.NONE) 1 else limit
        val documents = candidates
            .filter { (_, score) -> score * SCORE_RATIO_DENOMINATOR >= bestScore * SCORE_RATIO_NUMERATOR }
            .take(effectiveLimit)
            .map { it.first }
        if (documents.isEmpty()) return@coroutineScope RetrievedKnowledge.Empty

        val citations = documents.map { document ->
            SourceCitation(
                title = document.result.title,
                sourceName = document.result.sourceName,
                url = document.result.url,
                excerpt = document.plainText.clean().take(excerptLimit(game.spoilerLevel)),
                retrievedAt = document.retrievedAt,
            )
        }
        val context = citations.mapIndexed { index, citation ->
            "[${index + 1}] ${citation.title} (${citation.sourceName})\n${citation.excerpt}"
        }.joinToString("\n\n")
        RetrievedKnowledge(context, citations)
    }

    private fun tokens(text: String): Set<String> = TOKEN.findAll(text.lowercase())
        .map(MatchResult::value)
        .filter { it.length >= 3 }
        .toSet()

    private fun score(
        document: KnowledgeDocument,
        game: GameProfile,
        queryTerms: Set<String>,
        questionTerms: Set<String>,
    ): Int {
        val title = document.result.title.lowercase()
        val titleTokens = tokens(title)
        val allTokens = tokens("$title ${document.plainText}")
        return allTokens.count(queryTerms::contains) +
            titleTokens.count(questionTerms::contains) * 3 +
            if (title == game.title.lowercase()) 10 else 0
    }

    private fun String.clean() = replace(Regex("\\s+"), " ").trim()

    private fun excerptLimit(level: SpoilerLevel): Int = when (level) {
        SpoilerLevel.NONE -> 400
        SpoilerLevel.MINIMAL -> 650
        SpoilerLevel.MODERATE -> 850
        SpoilerLevel.FULL -> MAX_EXCERPT_CHARS
    }

    private companion object {
        val TOKEN = Regex("[\\p{L}\\p{N}']+")
        val STOP_WORDS = setOf(
            "about", "after", "before", "does", "from", "give", "have", "help", "into", "what", "when", "where",
            "which", "with", "without", "waar", "wanneer", "welke", "heeft", "voor", "naar", "over", "zonder",
        )
        const val SEARCH_LIMIT = 5
        const val MAX_EXCERPT_CHARS = 1_000
        const val SCORE_RATIO_NUMERATOR = 2
        const val SCORE_RATIO_DENOMINATOR = 3
    }
}
