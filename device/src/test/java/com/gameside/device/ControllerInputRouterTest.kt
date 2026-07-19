package com.gameside.device

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControllerInputRouterTest {
    private val router = ControllerInputRouter()

    @Test fun mapsGamepadFaceAndShoulderButtons() {
        assertEquals(ControllerCommand.OPEN_QUICK, router.commandFor(KeyEvent.KEYCODE_BUTTON_X, 0, true))
        assertEquals(ControllerCommand.OPEN_KEYWORD, router.commandFor(KeyEvent.KEYCODE_BUTTON_Y, 0, true))
        assertEquals(ControllerCommand.PREVIOUS_TAB, router.commandFor(KeyEvent.KEYCODE_BUTTON_L1, 0, true))
        assertEquals(ControllerCommand.NEXT_TAB, router.commandFor(KeyEvent.KEYCODE_BUTTON_R1, 0, true))
    }

    @Test fun ignoresNonControllerAndRepeatedEvents() {
        assertNull(router.commandFor(KeyEvent.KEYCODE_BUTTON_X, 0, false))
        assertNull(router.commandFor(KeyEvent.KEYCODE_BUTTON_X, 1, true))
    }

    @Test fun longPressPolicyRequiresMatchingEnabledKeyAndThreshold() {
        assertEquals(true, ControllerShortcutPolicy.shouldLaunch(108, 108, true, 900, 0, 800, true))
        assertEquals(false, ControllerShortcutPolicy.shouldLaunch(108, 108, true, 700, 0, 800, true))
        assertEquals(false, ControllerShortcutPolicy.shouldLaunch(108, 109, true, 900, 0, 800, true))
        assertEquals(false, ControllerShortcutPolicy.shouldLaunch(108, 108, false, 900, 0, 800, true))
    }

}
