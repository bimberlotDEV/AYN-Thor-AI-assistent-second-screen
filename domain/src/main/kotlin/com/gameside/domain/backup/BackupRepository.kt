package com.gameside.domain.backup

data class ImportResult(
    val games: Int,
    val conversations: Int,
    val personalItems: Int,
)

interface BackupRepository {
    suspend fun exportTo(destinationUri: String)
    suspend fun importFrom(sourceUri: String): ImportResult
}
