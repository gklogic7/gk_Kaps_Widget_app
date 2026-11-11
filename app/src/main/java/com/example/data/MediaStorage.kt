package com.example.data

import android.content.Context
import com.example.utils.ImageUtils
import java.io.File

class MediaStorage(private val context: Context) {

    val mediaDir: File
        get() = File(context.filesDir, "kaps_media").apply { if (!exists()) mkdirs() }

    val frameCacheDir: File
        get() = File(context.cacheDir, "kaps_widget_frames").apply { if (!exists()) mkdirs() }

    fun allocateMediaFile(extension: String): File {
        val cleanExt = extension.trimStart('.').lowercase().ifEmpty { "png" }
        val fileName = "kaps_${System.currentTimeMillis()}_${(1000..9999).random()}.$cleanExt"
        return File(mediaDir, fileName)
    }

    fun getCacheSizeBytes(): Long {
        val frameBytes = frameCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val mediaBytes = mediaDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return frameBytes + mediaBytes
    }

    fun clearFrameCache() {
        frameCacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * Ensures the 4 built-in starter samples (Transparent PNG, Animated GIF, Static SVG, Animated SVG)
     * exist in internal storage and returns their MediaItemEntity descriptors.
     */
    fun generateStarterSampleFiles(): List<MediaItemEntity> {
        val pngFile = File(mediaDir, "starter_kaps_bot_transparent.png")
        if (!pngFile.exists()) {
            ImageUtils.writeStarterTransparentPng(pngFile)
        }

        val gifFile = File(mediaDir, "starter_orbit_pulse_animated.gif")
        if (!gifFile.exists()) {
            ImageUtils.writeStarterAnimatedGif(gifFile)
        }

        val svgStaticFile = File(mediaDir, "starter_isometric_prism.svg")
        if (!svgStaticFile.exists()) {
            ImageUtils.writeStarterStaticSvg(svgStaticFile)
        }

        val svgAnimFile = File(mediaDir, "starter_kinetic_radar_anim.svg")
        if (!svgAnimFile.exists()) {
            ImageUtils.writeStarterAnimatedSvg(svgAnimFile)
        }

        return listOf(
            MediaItemEntity(
                displayName = "KAPS Bot Sticker (Alpha PNG)",
                originalUri = "internal://starter/png",
                localFilePath = pngFile.absolutePath,
                mimeType = "image/png",
                mediaFormat = MediaFormat.PNG,
                widthPx = 512,
                heightPx = 512,
                fileSizeBytes = pngFile.length(),
                frameCount = 1,
                hasTransparency = true,
                isBuiltInSample = true,
                isSourceAccessible = true
            ),
            MediaItemEntity(
                displayName = "Orbit Pulse Beacon (Animated GIF)",
                originalUri = "internal://starter/gif",
                localFilePath = gifFile.absolutePath,
                mimeType = "image/gif",
                mediaFormat = MediaFormat.GIF_ANIMATED,
                widthPx = 128,
                heightPx = 128,
                fileSizeBytes = gifFile.length(),
                frameCount = 6,
                defaultFrameDelayMs = 140,
                hasTransparency = true,
                isBuiltInSample = true,
                isSourceAccessible = true
            ),
            MediaItemEntity(
                displayName = "Isometric Prism Emblem (Vector SVG)",
                originalUri = "internal://starter/svg_static",
                localFilePath = svgStaticFile.absolutePath,
                mimeType = "image/svg+xml",
                mediaFormat = MediaFormat.SVG_STATIC,
                widthPx = 240,
                heightPx = 240,
                fileSizeBytes = svgStaticFile.length(),
                frameCount = 1,
                hasTransparency = true,
                isBuiltInSample = true,
                isSourceAccessible = true,
                svgCompatibilityNotes = "100% compatible crisp vector paths & gradients (9 elements)."
            ),
            MediaItemEntity(
                displayName = "Kinetic Radar Chrono (Animated SVG)",
                originalUri = "internal://starter/svg_anim",
                localFilePath = svgAnimFile.absolutePath,
                mimeType = "image/svg+xml",
                mediaFormat = MediaFormat.SVG_ANIMATED,
                widthPx = 240,
                heightPx = 240,
                fileSizeBytes = svgAnimFile.length(),
                frameCount = 8,
                defaultFrameDelayMs = 160,
                hasTransparency = true,
                isBuiltInSample = true,
                isSourceAccessible = true,
                svgCompatibilityNotes = "Supported animations: <animateTransform rotate>, <animate opacity>"
            )
        )
    }
}
