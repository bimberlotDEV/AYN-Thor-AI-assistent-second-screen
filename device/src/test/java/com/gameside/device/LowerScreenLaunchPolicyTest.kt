package com.gameside.device

import android.view.Display
import org.junit.Assert.assertEquals
import org.junit.Test

class LowerScreenLaunchPolicyTest {
    @Test
    fun `primary launch redirects to available lower display`() {
        assertEquals(2, LowerScreenLaunchPolicy.targetDisplay(Display.DEFAULT_DISPLAY, listOf(0, 2)))
    }

    @Test
    fun `launch already on lower display stays there`() {
        assertEquals(null, LowerScreenLaunchPolicy.targetDisplay(2, listOf(0, 2)))
    }

    @Test
    fun `single screen device stays on current display`() {
        assertEquals(null, LowerScreenLaunchPolicy.targetDisplay(Display.DEFAULT_DISPLAY, listOf(0)))
    }
}
