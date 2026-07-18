package com.gameside.domain.game

import java.time.Instant

data class GameProfile(
    val id: String,
    val title: String,
    val packageNames: List<String>,
    val platform: GamePlatform,
    val coverImageUri: String?,
    val preferredWikiSources: List<String>,
    val spoilerLevel: SpoilerLevel,
    val playerProgress: PlayerProgress?,
    val customSystemPrompt: String?,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class GamePlatform {
    ANDROID,
    EMULATED,
    PC_STREAMING,
    CONSOLE,
    OTHER,
}

enum class SpoilerLevel {
    NONE,
    MINIMAL,
    MODERATE,
    FULL,
}

data class PlayerProgress(
    val currentArea: String?,
    val currentChapter: String?,
    val currentQuest: String?,
    val customContext: String?,
)
