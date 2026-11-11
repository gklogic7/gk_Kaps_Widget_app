package com.example.core

import android.graphics.Bitmap
import com.example.data.MediaFormat
import com.example.data.WidgetConfigEntity
import com.example.utils.AlphaUtils
import com.example.utils.BitmapUtils
import com.example.widgets.GifRenderer
import com.example.widgets.StaticImageRenderer
import com.example.widgets.SvgRenderer
import java.io.File

object KapsImageDecoder {

    /**
     * Renders a single preview or static widget frame for any supported format (PNG, JPG, WEBP,
     * BMP, GIF, Animated WebP, Static SVG, or Animated SVG) at the requested resolution and animation phase.
     */
    fun renderPreviewFrame(
        config: WidgetConfigEntity,
        widthPx: Int,
        heightPx: Int,
        animationPhase: Float = 0f,
        frameIndexOverride: Int? = null
    ): Bitmap {
        val file = File(config.localFilePath)
        if (!file.exists()) {
            return AlphaUtils.createTransparentBitmap(widthPx, heightPx)
        }

        val isAnimPhaseStatic = !config.isAnimationEnabled || (animationPhase == 0f && frameIndexOverride == null)
        val cacheKey = buildString {
            append(config.localFilePath)
            append("|${widthPx}x${heightPx}")
            append("|${config.scaleMode}|${config.maintainAspectRatio}|${config.scaleFactor}")
            append("|${config.offsetX}|${config.offsetY}|${config.opacity}")
            append("|${config.backgroundMode}|${config.backgroundColorHex}|${config.cornerRadiusDp}|${config.preserveTransparency}")
            if (config.mediaFormat == MediaFormat.SVG_ANIMATED && config.isAnimationEnabled) {
                val bucket = ((animationPhase * 20).toInt() % 20)
                append("|svg_p$bucket")
            } else {
                val fIdx = frameIndexOverride ?: config.selectedFrameIndex
                append("|f$fIdx")
            }
        }

        if (isAnimPhaseStatic || config.mediaFormat != MediaFormat.SVG_ANIMATED) {
            ImageCache.get(cacheKey)?.let { return it }
        }

        val rendered = when (config.mediaFormat) {
            MediaFormat.SVG_STATIC -> {
                val rawSvg = SvgRenderer.renderSvgToBitmap(file, widthPx, heightPx, 0f)
                BitmapUtils.composeWidgetBitmap(rawSvg, widthPx, heightPx, config)
            }
            MediaFormat.SVG_ANIMATED -> {
                val phase = if (config.isAnimationEnabled) animationPhase else (config.selectedFrameIndex / 8f).coerceIn(0f, 1f)
                val rawSvg = SvgRenderer.renderSvgToBitmap(file, widthPx, heightPx, phase)
                BitmapUtils.composeWidgetBitmap(rawSvg, widthPx, heightPx, config)
            }
            MediaFormat.GIF_ANIMATED, MediaFormat.GIF_STATIC, MediaFormat.WEBP_ANIMATED -> {
                val gifResult = GifRenderer.decodeGifFrames(
                    file = file,
                    targetWidthPx = widthPx,
                    targetHeightPx = heightPx,
                    config = config.copy(isAnimationEnabled = true),
                    maxFrames = 8
                )
                val frames = gifResult.frames
                if (frames.isEmpty()) {
                    AlphaUtils.createTransparentBitmap(widthPx, heightPx)
                } else {
                    val idx = if (frameIndexOverride != null) {
                        frameIndexOverride % frames.size
                    } else if (!config.isAnimationEnabled) {
                        config.selectedFrameIndex.coerceIn(0, frames.lastIndex)
                    } else {
                        ((animationPhase * frames.size).toInt()).coerceIn(0, frames.lastIndex)
                    }
                    frames[idx]
                }
            }
            else -> {
                StaticImageRenderer.renderStaticImage(file, widthPx, heightPx, config)
            }
        }

        ImageCache.put(cacheKey, rendered)
        return rendered
    }
}
