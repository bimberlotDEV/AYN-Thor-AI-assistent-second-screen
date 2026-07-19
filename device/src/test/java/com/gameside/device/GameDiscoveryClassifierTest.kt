package com.gameside.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDiscoveryClassifierTest {
    @Test
    fun `known emulator packages and labels are detected`() {
        assertTrue(EmulatorCatalog.isEmulator("org.ppsspp.ppsspp", "PPSSPP"))
        assertTrue(EmulatorCatalog.isEmulator("org.dolphinemu.dolphinemu", "Dolphin Emulator"))
        assertTrue(EmulatorCatalog.isEmulator("com.retroarch", "RetroArch"))
        assertFalse(EmulatorCatalog.isEmulator("com.einnovation.temu", "Temu"))
    }

    @Test
    fun `rom extensions choose an installed compatible emulator`() {
        val installed = setOf("org.ppsspp.ppsspp", "org.dolphinemu.dolphinemu", "com.retroarch")
        assertEquals("org.ppsspp.ppsspp", EmulatorCatalog.packageFor("cso", "PSP/Game.cso", installed))
        assertEquals("org.dolphinemu.dolphinemu", EmulatorCatalog.packageFor("rvz", "GameCube/Game.rvz", installed))
        assertEquals("com.retroarch", EmulatorCatalog.packageFor("gba", "GBA/Game.gba", installed))
    }

    @Test
    fun `missing compatible emulator leaves rom import launch optional`() {
        assertEquals(null, EmulatorCatalog.packageFor("xci", "Switch/Game.xci", setOf("org.ppsspp.ppsspp")))
    }
}
