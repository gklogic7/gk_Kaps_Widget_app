package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaFormat(val label: String, val badgeText: String, val isAnimated: Boolean, val isVector: Boolean) {
    PNG("PNG Image", "PNG", false, false),
    JPEG("JPEG Image", "JPG", false, false),
    WEBP("WebP Image", "WEBP", false, false),
    WEBP_ANIMATED("Animated WebP", "ANIM-WEBP", true, false),
    BMP("Bitmap Image", "BMP", false, false),
    GIF_STATIC("Static GIF Frame", "GIF-FRAME", false, false),
    GIF_ANIMATED("Animated GIF", "GIF", true, false),
    SVG_STATIC("Vector SVG", "SVG", false, true),
    SVG_ANIMATED("Animated SVG", "ANIM-SVG", true, true)
}

enum class SizePreset(val label: String, val defaultWidthDp: Int, val defaultHeightDp: Int, val gridLabel: String) {
    SMALL("Small", 120, 120, "2×2"),
    MEDIUM("Medium", 220, 140, "3×2"),
    LARGE("Large", 300, 220, "4×3"),
    EXTRA_LARGE("Extra Large", 320, 320, "4×4"),
    CUSTOM("Custom Ratio", 240, 200, "Custom")
}

enum class ScaleMode(val label: String, val description: String) {
    FIT_INSIDE("Fit Inside", "Shows entire graphic inside widget bounds"),
    FILL("Fill", "Fills widget area completely"),
    CROP("Crop", "Center-crops graphic to fill widget"),
    CENTER("Center", "Centers at original pixel scale without upscaling"),
    STRETCH("Stretch", "Stretches independently to edges")
}

enum class BackgroundMode(val label: String, val description: String) {
    TRANSPARENT("Fully Transparent", "Pure alpha channel over wallpaper"),
    ORIGINAL("Original Image BG", "Preserves embedded source background"),
    SOLID_COLOR("Solid Color", "Full-bleed solid backdrop"),
    CUSTOM_COLOR("Custom Color", "Custom studio tint backdrop"),
    ROUNDED("Rounded Card", "Soft iPadOS-style rounded surface"),
    NONE("No Background", "Zero background layer")
}

enum class LoopMode(val label: String) {
    CONTINUOUS("Loop Continuously"),
    PLAY_ONCE("Play Cycle / Step")
}

enum class StudioThemeMode(val label: String, val subtitle: String) {
    LIGHT_IPADOS("Clean Light iPadOS Studio", "Alabaster canvas with frosted glass cards"),
    DARK_NOTHING("Nothing OS Dark", "Obsidian monochrome with signal red accents"),
    SYSTEM("Follow System Theme", "Adapts automatically to Android appearance")
}

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey val widgetId: Int,
    val title: String,
    val mediaId: Long,
    val sourceUri: String,
    val localFilePath: String,
    val mediaFormat: MediaFormat,
    val sizePreset: SizePreset = SizePreset.MEDIUM,
    val targetWidthDp: Int = 220,
    val targetHeightDp: Int = 140,
    val maintainAspectRatio: Boolean = true,
    val scaleMode: ScaleMode = ScaleMode.FIT_INSIDE,
    val scaleFactor: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val opacity: Float = 1.0f,
    val backgroundMode: BackgroundMode = BackgroundMode.TRANSPARENT,
    val backgroundColorHex: Long = 0xFFFFFFFF,
    val cornerRadiusDp: Int = 22,
    val preserveTransparency: Boolean = true,
    val isAnimationEnabled: Boolean = true,
    val animationSpeedMultiplier: Float = 1.0f,
    val loopMode: LoopMode = LoopMode.CONTINUOUS,
    val selectedFrameIndex: Int = 0,
    val totalFrames: Int = 1,
    val isPlacedOnHomeScreen: Boolean = false,
    val sourceMissing: Boolean = false,
    val svgCompatibilityNotes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val displayName: String,
    val originalUri: String,
    val localFilePath: String,
    val mimeType: String,
    val mediaFormat: MediaFormat,
    val widthPx: Int,
    val heightPx: Int,
    val fileSizeBytes: Long,
    val frameCount: Int = 1,
    val defaultFrameDelayMs: Int = 120,
    val hasTransparency: Boolean = true,
    val isBuiltInSample: Boolean = false,
    val isSourceAccessible: Boolean = true,
    val svgCompatibilityNotes: String = "",
    val importedAt: Long = System.currentTimeMillis()
)

data class SvgCompatibilityReport(
    val isAnimated: Boolean,
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val supportedElementsCount: Int,
    val animationFeatures: List<String>,
    val unsupportedFeatures: List<String>
) {
    fun toSummaryString(): String {
        val parts = mutableListOf<String>()
        if (animationFeatures.isNotEmpty()) {
            parts.add("Supported animations: ${animationFeatures.joinToString(", ")}")
        }
        if (unsupportedFeatures.isNotEmpty()) {
            parts.add("Ignored unsupported tags: ${unsupportedFeatures.joinToString(", ")}")
        }
        if (parts.isEmpty()) {
            parts.add("100% compatible crisp vector paths & gradients ($supportedElementsCount elements).")
        }
        return parts.joinToString(" • ")
    }
}

data class StudioPreferences(
    val themeMode: StudioThemeMode = StudioThemeMode.LIGHT_IPADOS,
    val checkerboardHighContrast: Boolean = true,
    val maxAnimationFps: Int = 10,
    val pauseOnBatterySaver: Boolean = true,
    val maxWidgetResolutionPx: Int = 720,
    val showPlacementGuide: Boolean = true
)
