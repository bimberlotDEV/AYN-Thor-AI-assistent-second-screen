package com.gameside.domain.ai

import com.gameside.domain.game.GamePlatform
import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.SpoilerLevel
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains

class GamePromptBuilderTest {
    @Test
    fun promptScopesAnswersAndEnforcesNoSpoilers() {
        val game = GameProfile(
            id = "game", title = "Example Game", packageNames = emptyList(),
            platform = GamePlatform.EMULATED, coverImageUri = null, preferredWikiSources = emptyList(),
            spoilerLevel = SpoilerLevel.NONE, playerProgress = null, customSystemPrompt = null,
            isPinned = false, isArchived = false, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        )

        val prompt = GamePromptBuilder().build(game)

        assertContains(prompt, "active game is: Example Game")
        assertContains(prompt, "Reveal no story developments")
        assertContains(prompt, "Never invent")
    }
}
