package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.example.data.BackgroundMode
import com.example.data.ScaleMode
import com.example.data.WidgetConfigEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapUtils {

    /**
     * Renders the source bitmap into the target widget resolution applying:
     * - Background mode (Transparent, Original, Solid, Custom, Rounded, None)
     * - Scale mode (Fit Inside, Fill, Crop, Center, Stretch)
     * - Aspect ratio lock, user scale multiplier, X/Y offset, and opacity
     * - Full alpha channel and anti-aliasing preservation (zero checkerboard in widget output)
     */
    fun composeWidgetBitmap(
        source: Bitmap,
        targetWidthPx: Int,
        targetHeightPx: Int,
        config: WidgetConfigEntity
    ): Bitmap {
        val outW = targetWidthPx.coerceAtLeast(32)
        val outH = targetHeightPx.coerceAtLeast(32)
        val output = AlphaUtils.createTransparentBitmap(outW, outH)
        val canvas = Canvas(output)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }

        val cornerPx = (config.cornerRadiusDp * (outW.toFloat() / config.targetWidthDp.coerceAtLeast(1)))
            .coerceIn(0f, min(outW, outH) / 2f)

        // 1. Draw Background if requested (never draw unwanted background in TRANSPARENT / NONE mode)
        val effectiveBgMode = if (!config.preserveTransparency &&
            (config.backgroundMode == BackgroundMode.TRANSPARENT || config.backgroundMode == BackgroundMode.NONE)
        ) {
            BackgroundMode.SOLID_COLOR
        } else {
            config.backgroundMode
        }

        when (effectiveBgMode) {
            BackgroundMode.TRANSPARENT, BackgroundMode.NONE, BackgroundMode.ORIGINAL -> {
                // Keep pure transparent canvas; ORIGINAL keeps only what's inside the source bitmap
            }
            BackgroundMode.SOLID_COLOR, BackgroundMode.CUSTOM_COLOR -> {
                bgPaint.color = config.backgroundColorHex.toInt()
                bgPaint.alpha = (bgPaint.alpha * config.opacity.coerceIn(0.05f, 1f)).roundToInt()
                canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), bgPaint)
            }
            BackgroundMode.ROUNDED -> {
                bgPaint.color = config.backgroundColorHex.toInt()
                bgPaint.alpha = (bgPaint.alpha * config.opacity.coerceIn(0.05f, 1f)).roundToInt()
                val rect = RectF(0f, 0f, outW.toFloat(), outH.toFloat())
                val radius = if (cornerPx > 2f) cornerPx else min(outW, outH) * 0.14f
                canvas.drawRoundRect(rect, radius, radius, bgPaint)
            }
        }

        // 2. Compute transformation matrix for source image
        val srcW = source.width.toFloat().coerceAtLeast(1f)
        val srcH = source.height.toFloat().coerceAtLeast(1f)
        val dstW = outW.toFloat()
        val dstH = outH.toFloat()

        val matrix = Matrix()
        val userScale = config.scaleFactor.coerceIn(0.25f, 3.0f)

        if (!config.maintainAspectRatio || config.scaleMode == ScaleMode.STRETCH) {
            val sx = (dstW / srcW) * userScale
            val sy = (dstH / srcH) * userScale
            val dx = (dstW - srcW * sx) * 0.5f + (config.offsetX * dstW * 0.4f)
            val dy = (dstH - srcH * sy) * 0.5f + (config.offsetY * dstH * 0.4f)
            matrix.postScale(sx, sy)
            matrix.postTranslate(dx, dy)
        } else {
            val baseScale = when (config.scaleMode) {
                ScaleMode.FIT_INSIDE -> min(dstW / srcW, dstH / srcH)
                ScaleMode.FILL, ScaleMode.CROP -> max(dstW / srcW, dstH / srcH)
                ScaleMode.CENTER -> min(1.0f, min(dstW / srcW, dstH / srcH))
                ScaleMode.STRETCH -> min(dstW / srcW, dstH / srcH)
            }
            val finalScale = baseScale * userScale
            val scaledW = srcW * finalScale
            val scaledH = srcH * finalScale
            val dx = (dstW - scaledW) * 0.5f + (config.offsetX * dstW * 0.4f)
            val dy = (dstH - scaledH) * 0.5f + (config.offsetY * dstH * 0.4f)
            matrix.postScale(finalScale, finalScale)
            matrix.postTranslate(dx, dy)
        }

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isDither = true
            alpha = (config.opacity.coerceIn(0.05f, 1.0f) * 255f).roundToInt()
        }

        if (effectiveBgMode == BackgroundMode.ROUNDED && cornerPx > 0f) {
            // Clip image smoothly to the rounded background shape
            val layerId = canvas.saveLayer(0f, 0f, dstW, dstH, null)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val radius = if (cornerPx > 2f) cornerPx else min(outW, outH) * 0.14f
            canvas.drawRoundRect(RectF(0f, 0f, dstW, dstH), radius, radius, maskPaint)
            imagePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(source, matrix, imagePaint)
            imagePaint.xfermode = null
            canvas.restoreToCount(layerId)
        } else {
            canvas.drawBitmap(source, matrix, imagePaint)
        }

        return output
    }
}
