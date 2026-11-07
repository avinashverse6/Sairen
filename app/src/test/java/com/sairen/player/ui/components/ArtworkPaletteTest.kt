package com.sairen.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteTest {

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun redOf(argb: Int) = (argb shr 16) and 0xFF
    private fun greenOf(argb: Int) = (argb shr 8) and 0xFF
    private fun blueOf(argb: Int) = argb and 0xFF
    private fun alphaOf(argb: Int) = (argb ushr 24) and 0xFF

    @Test
    fun `empty input falls back to the brand accent`() {
        assertEquals(ArtworkPalette.FALLBACK_ARGb, ArtworkPalette.accentFrom(IntArray(0)))
    }

    @Test
    fun `fully transparent artwork falls back`() {
        val pixels = intArrayOf(argb(0, 200, 40, 40), argb(0, 40, 200, 40))
        assertEquals(ArtworkPalette.FALLBACK_ARGb, ArtworkPalette.accentFrom(pixels))
    }

    @Test
    fun `a saturated green cover yields a green accent`() {
        val pixels = IntArray(64) { argb(255, 20, 160, 60) }
        val accent = ArtworkPalette.accentFrom(pixels)
        assertEquals(255, alphaOf(accent))
        assertTrue("green should dominate", greenOf(accent) > redOf(accent))
        assertTrue("green should dominate blue", greenOf(accent) > blueOf(accent))
    }

    @Test
    fun `near-black and near-white pixels are ignored`() {
        val pixels = intArrayOf(
            argb(255, 4, 4, 4),
            argb(255, 250, 250, 250),
            argb(255, 8, 8, 8),
        )
        assertEquals(ArtworkPalette.FALLBACK_ARGb, ArtworkPalette.accentFrom(pixels))
    }

    @Test
    fun `a very dark but valid cover is lifted above the background`() {
        // Mid-dark grey survives the luminance filter but has no channel above 56,
        // so the accent has to be lifted to stay visible on a near-black surface.
        val pixels = IntArray(16) { argb(255, 50, 50, 50) }
        val accent = ArtworkPalette.accentFrom(pixels)
        val brightest = maxOf(redOf(accent), greenOf(accent), blueOf(accent))
        assertEquals(56, brightest)
    }

    @Test
    fun `the result is always opaque`() {
        val pixels = IntArray(8) { argb(128, 200, 120, 40) }
        assertEquals(255, alphaOf(ArtworkPalette.accentFrom(pixels)))
    }

    @Test
    fun `a custom fallback is honoured`() {
        assertEquals(0xFF112233.toInt(), ArtworkPalette.accentFrom(IntArray(0), 0xFF112233.toInt()))
    }
}
