package com.gameside.domain.controller

import com.gameside.domain.game.SpoilerLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickQuestionComposerTest {
    @Test fun composesKeywordAndSpoilerInstruction() {
        val template = QuickQuestionComposer.templates.first { it.id == "item_find" }
        val result = QuickQuestionComposer.compose(template, "  Moonveil  ", SpoilerLevel.MINIMAL)
        assertEquals("Where and how can I get Moonveil? Use minimal spoilers and reveal only what I need now.", result)
    }

    @Test fun everyCategoryOffersControllerTemplates() {
        QuestionCategory.entries.forEach { category ->
            assertTrue(QuickQuestionComposer.templates.any { it.category == category })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun keywordTemplateRejectsEmptyKeyword() {
        QuickQuestionComposer.compose(QuickQuestionComposer.templates.first { it.needsKeyword }, "", SpoilerLevel.NONE)
    }
}
