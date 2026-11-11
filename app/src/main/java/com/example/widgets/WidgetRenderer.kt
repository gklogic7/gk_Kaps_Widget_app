package com.example.widgets

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.data.MediaFormat
import com.example.data.StudioPreferences
import com.example.data.WidgetConfigEntity
import com.example.utils.BitmapUtils
import com.example.utils.DimensionUtils
import java.io.File
import kotlin.math.roundToInt

object WidgetRenderer {

    /**
     * Renders a WidgetConfigEntity into a native Android RemoteViews instance.
     *
     * Key behaviors:
     * 1. Pure Desktop Art: No click PendingIntent is attached, so tapping the widget on the home screen
     *    never accidentally opens an activity.
     * 2. True Alpha Preservation: Renders ARGB_8888 bitmaps into a transparent container with 0dp padding.
     * 3. Dynamic Launcher Resizing: Inspects AppWidgetManager options to rasterize SVGs and scale bitmaps
     *    at the exact home-screen widget dimensions.
     * 4. Battery-Conscious Animation: Uses ViewFlipper frames with configurable interval and automatically
     *    falls back to a static frame when Power Save Mode / Doze is active.
     */
    fun buildRemoteViews(
        context: Context,
        config: WidgetConfigEntity,
        options: Bundle?,
        prefs: StudioPreferences
    ): RemoteViews {
        val sourceFile = File(config.localFilePath)
        val isMissing = !sourceFile.exists() || config.sourceMissing

        val (widthDp, heightDp) = DimensionUtils.resolveWidgetDimensionsDp(
            options = options,
            fallbackWidthDp = config.targetWidthDp,
            fallbackHeightDp = config.targetHeightDp
        )

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatterySaverActive = powerManager?.isPowerSaveMode == true
        val shouldAnimate = config.isAnimationEnabled &&
            config.mediaFormat.isAnimated &&
            !(prefs.pauseOnBatterySaver && isBatterySaverActive) &&
            !isMissing

        if (!shouldAnimate) {
            val (pxW, pxH) = DimensionUtils.dpToSafeWidgetPx(
                context = context,
                widthDp = widthDp,
                heightDp = heightDp,
                maxDimensionPx = prefs.maxWidgetResolutionPx,
                frameCount = 1
            )
            val views = RemoteViews(context.packageName, R.layout.widget_kaps_static)
            if (isMissing) {
                views.setViewVisibility(R.id.widget_status_badge, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_status_badge, View.GONE)
            }

            val bitmap = when (config.mediaFormat) {
                MediaFormat.SVG_STATIC, MediaFormat.SVG_ANIMATED -> {
                    val phase = (config.selectedFrameIndex.coerceAtLeast(0) / 8f).coerceIn(0f, 1f)
                    val rawSvg = SvgRenderer.renderSvgToBitmap(sourceFile, pxW, pxH, phase)
                    BitmapUtils.composeWidgetBitmap(rawSvg, pxW, pxH, config)
                }
                MediaFormat.GIF_ANIMATED, MediaFormat.GIF_STATIC, MediaFormat.WEBP_ANIMATED -> {
                    val gifRes = GifRenderer.decodeGifFrames(
                        file = sourceFile,
                        targetWidthPx = pxW,
                        targetHeightPx = pxH,
                        config = config.copy(isAnimationEnabled = false),
                        maxFrames = 8
                    )
                    gifRes.frames.firstOrNull()
                        ?: StaticImageRenderer.renderStaticImage(sourceFile, pxW, pxH, config)
                }
                else -> {
                    StaticImageRenderer.renderStaticImage(sourceFile, pxW, pxH, config)
                }
            }

            views.setImageViewBitmap(R.id.widget_image_view, bitmap)
            return views
        }

        // Animated GIF / Animated WebP / Animated SVG path via native ViewFlipper
        val maxFrames = 6
        val (pxW, pxH) = DimensionUtils.dpToSafeWidgetPx(
            context = context,
            widthDp = widthDp,
            heightDp = heightDp,
            maxDimensionPx = prefs.maxWidgetResolutionPx,
            frameCount = maxFrames
        )

        val framesAndDelay: Pair<List<android.graphics.Bitmap>, Int> = when (config.mediaFormat) {
            MediaFormat.SVG_ANIMATED -> {
                val svgFrames = (0 until maxFrames).map { idx ->
                    val phase = idx.toFloat() / maxFrames.toFloat()
                    val rawSvg = SvgRenderer.renderSvgToBitmap(sourceFile, pxW, pxH, phase)
                    BitmapUtils.composeWidgetBitmap(rawSvg, pxW, pxH, config)
                }
                val baseInterval = (1000f / prefs.maxAnimationFps.coerceIn(2, 15)).roundToInt()
                val interval = (baseInterval / config.animationSpeedMultiplier.coerceIn(0.25f, 3f))
                    .roundToInt()
                    .coerceIn(90, 1500)
                svgFrames to interval
            }
            else -> {
                val res = GifRenderer.decodeGifFrames(
                    file = sourceFile,
                    targetWidthPx = pxW,
                    targetHeightPx = pxH,
                    config = config,
                    maxFrames = maxFrames
                )
                val minIntervalFromFps = (1000f / prefs.maxAnimationFps.coerceIn(2, 20)).roundToInt()
                val safeInterval = maxOf(res.frameDelayMs, minIntervalFromFps).coerceIn(80, 2000)
                res.frames to safeInterval
            }
        }

        val frames = framesAndDelay.first
        if (frames.size <= 1) {
            val staticViews = RemoteViews(context.packageName, R.layout.widget_kaps_static)
            frames.firstOrNull()?.let { staticViews.setImageViewBitmap(R.id.widget_image_view, it) }
            return staticViews
        }

        val animViews = RemoteViews(context.packageName, R.layout.widget_kaps_animated)
        animViews.removeAllViews(R.id.widget_view_flipper)
        animViews.setInt(R.id.widget_view_flipper, "setFlipInterval", framesAndDelay.second)

        for (frameBmp in frames) {
            val frameView = RemoteViews(context.packageName, R.layout.widget_flipper_frame)
            frameView.setImageViewBitmap(R.id.frame_image, frameBmp)
            animViews.addView(R.id.widget_view_flipper, frameView)
        }

        return animViews
    }
}
