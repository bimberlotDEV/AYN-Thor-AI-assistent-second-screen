package com.gameside.domain.controller

import com.gameside.domain.game.SpoilerLevel
import java.time.Instant

enum class QuestionCategory(val label: String) {
    NAVIGATION("Navigation"),
    BOSS("Boss / combat"),
    ITEM("Item / build"),
    QUEST("Quest / NPC"),
    PUZZLE("Puzzle / mechanic"),
    SETTINGS("Settings / performance"),
}

data class QuestionTemplate(
    val id: String,
    val category: QuestionCategory,
    val label: String,
    val pattern: String,
    val needsKeyword: Boolean = false,
)

data class QuickQuestionFavorite(
    val id: String,
    val gameProfileId: String,
    val label: String,
    val question: String,
    val category: QuestionCategory,
    val position: Int,
    val createdAt: Instant,
)

interface QuickQuestionRepository {
    fun observeFavorites(gameProfileId: String): kotlinx.coroutines.flow.Flow<List<QuickQuestionFavorite>>
    suspend fun saveFavorite(favorite: QuickQuestionFavorite)
    suspend fun deleteFavorite(id: String)
}

object QuickQuestionComposer {
    val templates = listOf(
        QuestionTemplate("navigation_next", QuestionCategory.NAVIGATION, "Where do I go next?", "Where should I go next and what is my immediate objective?"),
        QuestionTemplate("navigation_location", QuestionCategory.NAVIGATION, "Find a location", "How do I reach %s?", true),
        QuestionTemplate("boss_strategy", QuestionCategory.BOSS, "Boss strategy", "Give me a practical strategy for %s.", true),
        QuestionTemplate("boss_weakness", QuestionCategory.BOSS, "Weaknesses", "What are %s's weaknesses and the best way to exploit them?", true),
        QuestionTemplate("item_find", QuestionCategory.ITEM, "Find an item", "Where and how can I get %s?", true),
        QuestionTemplate("item_build", QuestionCategory.ITEM, "Use in a build", "How should I use %s in an effective build?", true),
        QuestionTemplate("quest_next", QuestionCategory.QUEST, "Next quest step", "What is the next step for %s?", true),
        QuestionTemplate("quest_consequence", QuestionCategory.QUEST, "Choice consequences", "Explain the consequences of the choice involving %s.", true),
        QuestionTemplate("puzzle_hint", QuestionCategory.PUZZLE, "Give me a hint", "Give me a progressive hint for %s without immediately revealing the full solution.", true),
        QuestionTemplate("puzzle_explain", QuestionCategory.PUZZLE, "Explain a mechanic", "Explain how %s works and give one practical example.", true),
        QuestionTemplate("settings_performance", QuestionCategory.SETTINGS, "Improve performance", "Recommend settings that improve performance while keeping the game readable."),
        QuestionTemplate("settings_controls", QuestionCategory.SETTINGS, "Control advice", "Recommend practical controls and accessibility settings for this game."),
    )

    fun compose(template: QuestionTemplate, keyword: String, spoilerLevel: SpoilerLevel): String {
        val cleanKeyword = keyword.trim().replace(Regex("\\s+"), " ").take(120)
        require(!template.needsKeyword || cleanKeyword.isNotEmpty()) { "This question needs a keyword." }
        val base = if (template.needsKeyword) template.pattern.format(cleanKeyword) else template.pattern
        val spoilerInstruction = when (spoilerLevel) {
            SpoilerLevel.NONE -> "Do not reveal story or discovery spoilers."
            SpoilerLevel.MINIMAL -> "Use minimal spoilers and reveal only what I need now."
            SpoilerLevel.MODERATE -> "Moderate spoilers are acceptable, but warn before major reveals."
            SpoilerLevel.FULL -> "Full spoilers are acceptable."
        }
        return "$base $spoilerInstruction"
    }

    fun followUps(category: QuestionCategory?): List<String> = when (category) {
        QuestionCategory.NAVIGATION -> listOf("Give me the next step only.", "What should I prepare before going there?", "Give me more detail with minimal spoilers.")
        QuestionCategory.BOSS -> listOf("Which attacks should I punish?", "What equipment should I prepare?", "Turn this strategy into a checklist.")
        QuestionCategory.ITEM -> listOf("What are the requirements?", "Is there an alternative?", "How does this fit my current progression?")
        QuestionCategory.QUEST -> listOf("Give me the next step only.", "Are there missable rewards?", "Warn me before major spoilers.")
        QuestionCategory.PUZZLE -> listOf("Give me a smaller hint.", "Now give me the full solution.", "Explain why the solution works.")
        QuestionCategory.SETTINGS -> listOf("Prioritize stable performance.", "Prioritize image quality.", "Give me a short settings checklist.")
        null -> listOf("Explain that more simply.", "Give me the next practical step.", "Summarize this as a checklist.")
    }
}
