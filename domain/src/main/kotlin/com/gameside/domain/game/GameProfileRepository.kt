package com.gameside.domain.game

import kotlinx.coroutines.flow.Flow

interface GameProfileRepository {
    fun observeAll(includeArchived: Boolean = false): Flow<List<GameProfile>>
    fun observeById(id: String): Flow<GameProfile?>
    suspend fun upsert(profile: GameProfile)
    suspend fun delete(id: String)
}
