package com.sairen.player.ui.components

/**
 * Derives a usable accent colour from a few hundred decoded artwork pixels.
 *
 * Pure JVM maths — no Palette library, no native code, no Android types — so the
 * Now Playing gradient costs one tiny bitmap and can be unit tested directly.
 *
 * The weighting favours saturated pixels, because the average of an album cover
 * is usually a muddy grey; the result is then pushed into a luminance range that
 * stays legible under white text on a near-black background.
 */
object ArtworkPalette {

    /** Sairen's accent, used when the artwork is missing or gives nothing usable. */
    const val FALLBACK_ARGb: Int = 0xFF1DB954.toInt()

    private const val ALPHA_OPAQUE = 0xFF

    /** Minimum perceived luminance a pixel needs to influence the result. */
    private const val MIN_LUMINANCE = 26.0

    /** Maximum perceived luminance a pixel may have (near-white pixels wash the accent out). */
    private const val MAX_LUMINANCE = 236.0

    /** Channel floor so the accent never disappears into a near-black background. */
    private const val MIN_ACCENT_CHANNEL = 56

    fun accentFrom(pixels: IntArray, fallbackArgb: Int = FALLBACK_ARGb): Int {
        var red = 0.0
        var green = 0.0
        var blue = 0.0
        var weight = 0.0

        for (pixel in pixels) {
            if (((pixel ushr 24) and 0xFF) < 128) continue
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            if (luminance < MIN_LUMINANCE || luminance > MAX_LUMINANCE) continue
            val saturation = if (max == 0) 0.0 else (max - min).toDouble() / max
            val pixelWeight = 1.0 + saturation * 3.0
            red += r * pixelWeight
            green += g * pixelWeight
            blue += b * pixelWeight
            weight += pixelWeight
        }

        if (weight <= 0.0) return fallbackArgb

        return pack(
            r = (red / weight).toInt().coerceIn(0, 255),
            g = (green / weight).toInt().coerceIn(0, 255),
            b = (blue / weight).toInt().coerceIn(0, 255),
        )
    }

    private fun pack(r: Int, g: Int, b: Int): Int {
        val max = maxOf(r, g, b)
        val scale = if (max in 1 until MIN_ACCENT_CHANNEL) {
            MIN_ACCENT_CHANNEL.toDouble() / max
        } else {
            1.0
        }
        val red = (r * scale).toInt().coerceIn(0, 255)
        val green = (g * scale).toInt().coerceIn(0, 255)
        val blue = (b * scale).toInt().coerceIn(0, 255)
        return (ALPHA_OPAQUE shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
