package com.gameside.device

import android.view.Display
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionRestorePolicyTest {
    @Test
    fun `session remains active when activity is displaced until explicit stop`() {
        val started = CompanionSessionReducer.start(CompanionSessionState(), displayId = 2)
        val visible = CompanionSessionReducer.visible(started, displayId = 2)
        val displaced = CompanionSessionReducer.hidden(visible)
        assertTrue(displaced.sessionActive)
        assertEquals(CompanionSessionStatus.temporarilyDisplaced, displaced.status)
        val stopped = CompanionSessionReducer.stop(displaced)
        assertFalse(stopped.sessionActive)
        assertEquals(CompanionSessionStatus.inactive, stopped.status)
        assertTrue(stopped.generation > displaced.generation)
    }

    @Test
    fun `cooldown prevents rapid task ping pong`() {
        val limiter = RestoreAttemptLimiter()
        assertEquals(RestoreLimitDecision.Allowed, limiter.decision(10_000))
        limiter.record(10_000)
        assertEquals(RestoreLimitDecision.Cooldown, limiter.decision(14_999))
        assertEquals(RestoreLimitDecision.Allowed, limiter.decision(15_000))
    }

    @Test
    fun `three automatic restores per minute reaches limit`() {
        val limiter = RestoreAttemptLimiter()
        listOf(0L, 5_000L, 10_000L).forEach { limiter.record(it) }
        assertEquals(RestoreLimitDecision.LimitReached, limiter.decision(15_000))
        assertEquals(RestoreLimitDecision.Allowed, limiter.decision(60_000))
    }

    @Test
    fun `manual restore remains available after automatic limit`() {
        val limiter = RestoreAttemptLimiter()
        listOf(0L, 5_000L, 10_000L).forEach { limiter.record(it) }
        assertEquals(RestoreLimitDecision.Allowed, limiter.decision(15_000, manual = true))
    }

    @Test
    fun `only non system non launcher primary apps trigger restore`() {
        val homes = setOf("com.ayn.launcher")
        assertTrue(CompanionWindowPolicy.isEligiblePrimaryApp("com.example.game", "com.gameside.ai", homes))
        assertFalse(CompanionWindowPolicy.isEligiblePrimaryApp("com.gameside.ai", "com.gameside.ai", homes))
        assertFalse(CompanionWindowPolicy.isEligiblePrimaryApp("com.ayn.launcher", "com.gameside.ai", homes))
        assertFalse(CompanionWindowPolicy.isEligiblePrimaryApp("com.android.systemui", "com.gameside.ai", homes))
        assertFalse(CompanionWindowPolicy.isEligiblePrimaryApp("android", "com.gameside.ai", homes))
    }

    @Test
    fun `display validation keeps preferred or falls back after reconnect`() {
        assertEquals(2, CompanionWindowPolicy.selectSecondaryDisplay(2, listOf(Display.DEFAULT_DISPLAY, 2, 4)))
        assertEquals(4, CompanionWindowPolicy.selectSecondaryDisplay(2, listOf(Display.DEFAULT_DISPLAY, 4)))
        assertEquals(null, CompanionWindowPolicy.selectSecondaryDisplay(2, listOf(Display.DEFAULT_DISPLAY)))
    }
}
