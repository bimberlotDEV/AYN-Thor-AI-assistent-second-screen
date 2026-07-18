package com.gameside.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Upsert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GameProfileDao {
    @Transaction
    @Query(
        """
        SELECT * FROM game_profiles
        WHERE (:includeArchived = 1 OR isArchived = 0)
        ORDER BY isPinned DESC, title COLLATE NOCASE ASC
        """,
    )
    abstract fun observeAll(includeArchived: Boolean): Flow<List<GameProfileWithRelations>>

    @Transaction
    @Query("SELECT * FROM game_profiles WHERE id = :id")
    abstract fun observeById(id: String): Flow<GameProfileWithRelations?>

    @Upsert
    protected abstract suspend fun insertProfile(profile: GameProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPackages(packages: List<GamePackageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertWikiSources(sources: List<GameWikiSourceEntity>)

    @Query("DELETE FROM game_packages WHERE gameProfileId = :gameId")
    protected abstract suspend fun deletePackages(gameId: String)

    @Query("DELETE FROM game_wiki_sources WHERE gameProfileId = :gameId")
    protected abstract suspend fun deleteWikiSources(gameId: String)

    @Query("DELETE FROM game_profiles WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM game_packages WHERE gameProfileId = :gameId")
    abstract suspend fun packageCount(gameId: String): Int

    @Transaction
    open suspend fun upsert(value: GameProfileWithRelations) {
        insertProfile(value.profile)
        deletePackages(value.profile.id)
        deleteWikiSources(value.profile.id)
        if (value.packages.isNotEmpty()) insertPackages(value.packages)
        if (value.wikiSources.isNotEmpty()) insertWikiSources(value.wikiSources)
    }
}
