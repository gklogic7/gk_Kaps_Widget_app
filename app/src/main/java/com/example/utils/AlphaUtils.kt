package com.example.utils

import android.graphics.Bitmap
import android.graphics.Color

object AlphaUtils {

    /**
     * Creates a mutable ARGB_8888 bitmap guaranteed to start with 100% transparent pixels
     * and premultiplied alpha enabled for accurate anti-aliased compositing.
     */
    fun createTransparentBitmap(width: Int, height: Int): Bitmap {
        val safeW = width.coerceAtLeast(1)
        val safeH = height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(true)
        bitmap.eraseColor(Color.TRANSPARENT)
        return bitmap
    }

    /**
     * Samples pixels across the bitmap (corners, edges, and grid) to determine if the
     * image contains transparent or semi-transparent pixels.
     */
    fun detectTransparency(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return false

        // Check corners first (most transparent stickers/logos have transparent corners)
        val corners = listOf(
            0 to 0,
            (w - 1) to 0,
            0 to (h - 1),
            (w - 1) to (h - 1)
        )
        for ((cx, cy) in corners) {
            val alpha = Color.alpha(bitmap.getPixel(cx, cy))
            if (alpha < 250) return true
        }

        // Sample an 8x8 grid across the image
        val stepX = (w / 8).coerceAtLeast(1)
        val stepY = (h / 8).coerceAtLeast(1)
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                if (Color.alpha(bitmap.getPixel(x, y)) < 250) {
                    return true
                }
                x += stepX
            }
            y += stepY
        }
        return false
    }
}
