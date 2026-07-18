package com.gameside.domain.display

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DisplaySelectionPolicyTest {
    @Test
    fun `selects smallest eligible non-default display`() {
        val primary = display(id = 0, width = 1920, height = 1080, isDefault = true)
        val external = display(id = 2, width = 1600, height = 900)
        val lower = display(id = 4, width = 1240, height = 1080)

        assertEquals(4, DisplaySelectionPolicy.preferred(listOf(primary, external, lower))?.id)
    }

    @Test
    fun `saved signature wins over size heuristic`() {
        val smaller = display(id = 2, width = 800, height = 600, name = "Small")
        val saved = display(id = 7, width = 1240, height = 1080, name = "Thor lower")

        assertEquals(7, DisplaySelectionPolicy.preferred(listOf(smaller, saved), saved.signature)?.id)
    }

    @Test
    fun `returns null when no secondary activity host exists`() {
        val primary = display(id = 0, width = 1080, height = 2312, isDefault = true)
        val privateDisplay = display(id = 3, width = 800, height = 600, canHost = false)

        assertNull(DisplaySelectionPolicy.preferred(listOf(primary, privateDisplay)))
    }

    private fun display(
        id: Int,
        width: Int,
        height: Int,
        name: String = "Display $id",
        isDefault: Boolean = false,
        canHost: Boolean = true,
    ) = DeviceDisplayInfo(
        id = id,
        name = name,
        widthPixels = width,
        heightPixels = height,
        densityDpi = 320,
        refreshRateHz = 60f,
        rotationDegrees = 0,
        isDefault = isDefault,
        isPresentation = !isDefault,
        isPrivate = false,
        isSecure = true,
        hasTouch = true,
        canHostActivities = canHost,
    )
}
