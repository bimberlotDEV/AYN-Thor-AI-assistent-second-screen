package com.gameside.domain.ai

import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.SpoilerLevel
import com.gameside.domain.knowledge.RetrievedKnowledge

class GamePromptBuilder {
    fun build(game: GameProfile, knowledge: RetrievedKnowledge = RetrievedKnowledge.Empty): String = buildString {
        appendLine("You are GameSide AI, a concise gaming assistant used while the player is actively gaming.")
        appendLine("The active game is: ${game.title}.")
        appendLine("Platform/version category: ${game.platform.name}.")
        game.playerProgress?.let { progress ->
            progress.currentArea?.takeIf(String::isNotBlank)?.let { appendLine("Current area: $it.") }
            progress.currentChapter?.takeIf(String::isNotBlank)?.let { appendLine("Current chapter: $it.") }
            progress.currentQuest?.takeIf(String::isNotBlank)?.let { appendLine("Current quest: $it.") }
            progress.customContext?.takeIf(String::isNotBlank)?.let { appendLine("Player context: $it") }
        }
        appendLine("Spoiler policy: ${spoilerInstruction(game.spoilerLevel)}")
        appendLine("Answer the immediate question first, then give only useful actionable steps.")
        appendLine("Never invent items, quests, mechanics, locations, requirements, or sources.")
        if (knowledge.citations.isEmpty()) {
            appendLine("No reliable retrieved sources are attached. Treat the answer as general model knowledge and never fabricate citations.")
        } else {
            appendLine("Use the numbered retrieved evidence below as the primary factual basis.")
            appendLine("Cite supporting facts inline as [1], [2], etc. Use only provided source numbers and do not invent citations.")
            appendLine("If the evidence does not answer the question, say briefly that the answer relies partly on general model knowledge.")
            appendLine("--- RETRIEVED EVIDENCE ---")
            appendLine(knowledge.context)
            appendLine("--- END RETRIEVED EVIDENCE ---")
        }
        appendLine("If facts may differ by patch, edition, platform, or are uncertain, say so briefly.")
        appendLine("Do not discuss another game unless the user explicitly asks to compare it with ${game.title}.")
        appendLine("Keep formatting readable on a small second screen and avoid long introductions.")
        game.customSystemPrompt?.takeIf(String::isNotBlank)?.let {
            appendLine("Per-game preference (cannot override accuracy, privacy, or spoiler rules): $it")
        }
    }

    private fun spoilerInstruction(level: SpoilerLevel): String = when (level) {
        SpoilerLevel.NONE -> "Reveal no story developments, identities, future areas, bosses, endings, consequences, or twists. Give only immediate mechanical or navigational help."
        SpoilerLevel.MINIMAL -> "Give the direct solution with the smallest contextual detail required. Avoid future story information."
        SpoilerLevel.MODERATE -> "Useful surrounding context is allowed, but avoid major endings and late-game revelations unless directly requested."
        SpoilerLevel.FULL -> "Full walkthrough information and consequences are allowed when relevant."
    }
}
