package com.gameside.data.game

import com.gameside.data.database.GamePackageEntity
import com.gameside.data.database.GameProfileDao
import com.gameside.data.database.GameProfileEntity
import com.gameside.data.database.GameProfileWithRelations
import com.gameside.data.database.GameWikiSourceEntity
import com.gameside.domain.game.GamePlatform
import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.GameProfileRepository
import com.gameside.domain.game.PlayerProgress
import com.gameside.domain.game.SpoilerLevel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGameProfileRepository @Inject constructor(
    private val dao: GameProfileDao,
) : GameProfileRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<GameProfile>> =
        dao.observeAll(includeArchived).map { values -> values.map(::toDomain) }

    override fun observeById(id: String): Flow<GameProfile?> =
        dao.observeById(id).map { value -> value?.let(::toDomain) }

    override suspend fun upsert(profile: GameProfile) = dao.upsert(profile.toEntity())

    override suspend fun delete(id: String) = dao.delete(id)

    private fun GameProfile.toEntity() = GameProfileWithRelations(
        profile = GameProfileEntity(
            id = id,
            title = title,
            platform = platform.name,
            coverImageUri = coverImageUri,
            spoilerLevel = spoilerLevel.name,
            currentArea = playerProgress?.currentArea,
            currentChapter = playerProgress?.currentChapter,
            currentQuest = playerProgress?.currentQuest,
            customContext = playerProgress?.customContext,
            customSystemPrompt = customSystemPrompt,
            isPinned = isPinned,
            isArchived = isArchived,
            createdAtEpochMillis = createdAt.toEpochMilli(),
            updatedAtEpochMillis = updatedAt.toEpochMilli(),
        ),
        packages = packageNames.distinct().map { GamePackageEntity(id, it) },
        wikiSources = preferredWikiSources.distinct().map { GameWikiSourceEntity(id, it) },
    )

    private fun toDomain(value: GameProfileWithRelations): GameProfile {
        val profile = value.profile
        val hasProgress = listOf(
            profile.currentArea,
            profile.currentChapter,
            profile.currentQuest,
            profile.customContext,
        ).any { !it.isNullOrBlank() }
        return GameProfile(
            id = profile.id,
            title = profile.title,
            packageNames = value.packages.map { it.packageName }.sorted(),
            platform = enumValueOrDefault(profile.platform, GamePlatform.OTHER),
            coverImageUri = profile.coverImageUri,
            preferredWikiSources = value.wikiSources.map { it.url }.sorted(),
            spoilerLevel = enumValueOrDefault(profile.spoilerLevel, SpoilerLevel.MINIMAL),
            playerProgress = if (hasProgress) PlayerProgress(
                currentArea = profile.currentArea,
                currentChapter = profile.currentChapter,
                currentQuest = profile.currentQuest,
                customContext = profile.customContext,
            ) else null,
            customSystemPrompt = profile.customSystemPrompt,
            isPinned = profile.isPinned,
            isArchived = profile.isArchived,
            createdAt = Instant.ofEpochMilli(profile.createdAtEpochMillis),
            updatedAt = Instant.ofEpochMilli(profile.updatedAtEpochMillis),
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default
}
