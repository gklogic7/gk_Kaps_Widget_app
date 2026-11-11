package com.example.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object DimensionUtils {

    /**
     * Converts dp to pixels using the current display metrics while clamping to a safe
     * resolution budget so RemoteViews never exceeds Android's Binder IPC transaction limit.
     */
    fun dpToSafeWidgetPx(
        context: Context,
        widthDp: Int,
        heightDp: Int,
        maxDimensionPx: Int = 720,
        frameCount: Int = 1
    ): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        val rawW = (widthDp.coerceAtLeast(48) * density).roundToInt()
        val rawH = (heightDp.coerceAtLeast(48) * density).roundToInt()

        // If rendering multiple frames inside a RemoteViews ViewFlipper, bound per-frame byte budget
        val effectiveCap = when {
            frameCount > 8 -> min(maxDimensionPx, 280)
            frameCount > 4 -> min(maxDimensionPx, 360)
            frameCount > 1 -> min(maxDimensionPx, 440)
            else -> maxDimensionPx.coerceIn(256, 1024)
        }

        val largestSide = max(rawW, rawH)
        if (largestSide <= effectiveCap) {
            return rawW.coerceAtLeast(64) to rawH.coerceAtLeast(64)
        }
        val scale = effectiveCap.toFloat() / largestSide.toFloat()
        val scaledW = (rawW * scale).roundToInt().coerceAtLeast(64)
        val scaledH = (rawH * scale).roundToInt().coerceAtLeast(64)
        return scaledW to scaledH
    }

    /**
     * Extracts current home-screen widget dimensions in dp from AppWidgetManager options bundle,
     * falling back to the configured target width/height when options are not yet populated.
     */
    fun resolveWidgetDimensionsDp(
        options: Bundle?,
        fallbackWidthDp: Int,
        fallbackHeightDp: Int
    ): Pair<Int, Int> {
        if (options == null) return fallbackWidthDp to fallbackHeightDp
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

        val resolvedW = when {
            maxW > 0 -> maxW
            minW > 0 -> minW
            else -> fallbackWidthDp
        }
        val resolvedH = when {
            maxH > 0 -> maxH
            minH > 0 -> minH
            else -> fallbackHeightDp
        }
        return resolvedW.coerceIn(48, 900) to resolvedH.coerceIn(48, 900)
    }

    /**
     * Estimates launcher grid cell span (e.g., 2x2, 3x2, 4x4) from dp dimensions.
     */
    fun estimateGridCells(widthDp: Int, heightDp: Int): Pair<Int, Int> {
        val cols = ((widthDp + 30) / 70).coerceIn(1, 8)
        val rows = ((heightDp + 30) / 70).coerceIn(1, 8)
        return cols to rows
    }
}
