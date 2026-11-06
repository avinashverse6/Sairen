package com.sairen.player.ui.theme

import com.sairen.player.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeResolutionTest {

    @Test
    fun `system mode follows the system setting`() {
        assertEquals(true, shouldUseDarkTheme(ThemeMode.SYSTEM, true))
        assertEquals(false, shouldUseDarkTheme(ThemeMode.SYSTEM, false))
    }

    @Test
    fun `explicit modes override the system setting`() {
        assertEquals(true, shouldUseDarkTheme(ThemeMode.DARK, false))
        assertEquals(false, shouldUseDarkTheme(ThemeMode.LIGHT, true))
    }
}
