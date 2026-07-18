package com.gameside.data.knowledge

import com.gameside.data.database.KnowledgeCacheDao
import com.gameside.data.database.KnowledgeCacheEntity
import com.gameside.domain.game.GameProfile
import com.gameside.domain.knowledge.GameKnowledgeProvider
import com.gameside.domain.knowledge.KnowledgeCacheRepository
import com.gameside.domain.knowledge.KnowledgeDocument
import com.gameside.domain.knowledge.KnowledgeSearchResult
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CachingGameKnowledgeProvider @Inject constructor(
    private val remote: MediaWikiGameKnowledgeProvider,
    private val dao: KnowledgeCacheDao,
) : GameKnowledgeProvider, KnowledgeCacheRepository {
    override suspend fun search(game: GameProfile, query: String): List<KnowledgeSearchResult> = remote.search(game, query)

    override suspend fun retrieve(result: KnowledgeSearchResult): KnowledgeDocument {
        dao.get(result.gameProfileId, result.sourceApiUrl, result.id)?.let { cached ->
            if (Duration.between(Instant.ofEpochMilli(cached.retrievedAtEpochMillis), Instant.now()) < MAX_AGE) return cached.toDomain()
        }
        return remote.retrieve(result).also { dao.upsert(it.toEntity()) }
    }

    override fun observeCached(gameProfileId: String): Flow<List<KnowledgeDocument>> =
        dao.observe(gameProfileId).map { values -> values.map { it.toDomain() } }

    override suspend fun clear(gameProfileId: String) = dao.clear(gameProfileId)

    private fun KnowledgeDocument.toEntity() = KnowledgeCacheEntity(
        result.gameProfileId, result.sourceApiUrl, result.id, result.title, result.sourceName, result.url,
        plainText, retrievedAt.toEpochMilli(),
    )

    private fun KnowledgeCacheEntity.toDomain() = KnowledgeDocument(
        KnowledgeSearchResult(pageId, gameProfileId, title, sourceName, sourceApiUrl, url, ""),
        plainText, Instant.ofEpochMilli(retrievedAtEpochMillis),
    )

    private companion object { val MAX_AGE: Duration = Duration.ofDays(7) }
}
