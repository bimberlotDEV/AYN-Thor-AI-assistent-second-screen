package com.gameside.data.controller

import com.gameside.data.database.QuickQuestionDao
import com.gameside.data.database.QuickQuestionFavoriteEntity
import com.gameside.domain.controller.QuestionCategory
import com.gameside.domain.controller.QuickQuestionFavorite
import com.gameside.domain.controller.QuickQuestionRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomQuickQuestionRepository @Inject constructor(
    private val dao: QuickQuestionDao,
) : QuickQuestionRepository {
    override fun observeFavorites(gameProfileId: String): Flow<List<QuickQuestionFavorite>> =
        dao.observe(gameProfileId).map { values -> values.map { it.toDomain() } }

    override suspend fun saveFavorite(favorite: QuickQuestionFavorite) = dao.upsert(favorite.toEntity())
    override suspend fun deleteFavorite(id: String) = dao.delete(id)

    private fun QuickQuestionFavorite.toEntity() = QuickQuestionFavoriteEntity(
        id, gameProfileId, label, question, category.name, position, createdAt.toEpochMilli(),
    )

    private fun QuickQuestionFavoriteEntity.toDomain() = QuickQuestionFavorite(
        id, gameProfileId, label, question, QuestionCategory.valueOf(category), position,
        Instant.ofEpochMilli(createdAtEpochMillis),
    )
}
