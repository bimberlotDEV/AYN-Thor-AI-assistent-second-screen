package com.gameside.domain.game

data class DiscoveredGame(
    val stableKey: String,
    val title: String,
    val packageName: String?,
    val platform: GamePlatform,
    val sourceLabel: String,
)

interface GameDiscoveryRepository {
    suspend fun detectInstalledGamesAndEmulators(): List<DiscoveredGame>
    suspend fun scanRomTree(treeUri: String): List<DiscoveredGame>
}
