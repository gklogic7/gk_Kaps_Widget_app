package com.example.features

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.KapsImageDecoder
import com.example.data.MediaFormat
import com.example.data.WidgetConfigEntity
import com.example.ui.theme.BlueprintCobalt
import com.example.ui.theme.CheckerDarkHigh
import com.example.ui.theme.CheckerDarkSubtle
import com.example.ui.theme.CheckerLightHigh
import com.example.ui.theme.CheckerLightSubtle
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.SignalCrimson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/**
 * Editor-only transparency checkerboard backdrop.
 * Strictly used inside the KAPS Studio app so users can clearly inspect alpha channels
 * and transparent pixels; NEVER included in the actual Android home-screen widget.
 */
@Composable
fun CheckerboardBackdrop(
    modifier: Modifier = Modifier,
    showCheckerboard: Boolean = true,
    highContrast: Boolean = true,
    cellSizeDp: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    val lightColor = if (highContrast) CheckerLightHigh else CheckerLightSubtle
    val darkColor = if (highContrast) CheckerDarkHigh else CheckerDarkSubtle

    Box(modifier = modifier) {
        if (showCheckerboard) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellPx = cellSizeDp.toPx().coerceAtLeast(6f)
                val cols = ceil(size.width / cellPx).toInt()
                val rows = ceil(size.height / cellPx).toInt()
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val isEven = (r + c) % 2 == 0
                        drawRect(
                            color = if (isEven) lightColor else darkColor,
                            topLeft = Offset(c * cellPx, r * cellPx),
                            size = Size(cellPx, cellPx)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        content()
    }
}

/**
 * Live preview renderer that decodes and animates static PNG/JPG/WebP/BMP,
 * multi-frame GIFs, and static/animated SVGs directly inside Jetpack Compose.
 */
@Composable
fun LiveWidgetPreviewBox(
    config: WidgetConfigEntity,
    modifier: Modifier = Modifier,
    showCheckerboard: Boolean = true,
    highContrastCheckerboard: Boolean = true,
    previewResolutionPx: Int = 320,
    showDimensionGuides: Boolean = false
) {
    var currentPhase by remember { mutableFloatStateOf(0f) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Advance animation phase when animation is enabled on GIF / Animated SVG
    LaunchedEffect(
        config.isAnimationEnabled,
        config.mediaFormat,
        config.animationSpeedMultiplier,
        config.loopMode
    ) {
        if (config.isAnimationEnabled && config.mediaFormat.isAnimated) {
            val stepMs = (110f / config.animationSpeedMultiplier.coerceIn(0.25f, 3f)).toLong().coerceAtLeast(50L)
            var ticks = 0
            while (isActive) {
                delay(stepMs)
                ticks++
                val nextPhase = (currentPhase + 0.125f)
                if (config.loopMode == com.example.data.LoopMode.PLAY_ONCE && nextPhase >= 1f) {
                    currentPhase = 0.99f
                    break
                } else {
                    currentPhase = nextPhase % 1.0f
                }
            }
        } else {
            currentPhase = 0f
        }
    }

    // Re-render frame whenever config or animation phase changes
    LaunchedEffect(config, currentPhase, previewResolutionPx) {
        val ratio = config.targetWidthDp.toFloat().coerceAtLeast(1f) /
            config.targetHeightDp.toFloat().coerceAtLeast(1f)
        val wPx = if (ratio >= 1f) previewResolutionPx else (previewResolutionPx * ratio).toInt().coerceAtLeast(64)
        val hPx = if (ratio <= 1f) previewResolutionPx else (previewResolutionPx / ratio).toInt().coerceAtLeast(64)

        val bmp = withContext(Dispatchers.Default) {
            KapsImageDecoder.renderPreviewFrame(
                config = config,
                widthPx = wPx,
                heightPx = hPx,
                animationPhase = currentPhase
            )
        }
        renderedBitmap = bmp
    }

    CheckerboardBackdrop(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                BorderStroke(
                    width = if (showDimensionGuides) 1.5.dp else 1.dp,
                    color = if (showDimensionGuides) BlueprintCobalt.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline
                ),
                RoundedCornerShape(18.dp)
            ),
        showCheckerboard = showCheckerboard,
        highContrast = highContrastCheckerboard
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val bmp = renderedBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = config.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            if (showDimensionGuides) {
                // Blueprint resize corner handles & dimension pill
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${config.targetWidthDp}×${config.targetHeightDp} dp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (config.preserveTransparency) {
                    Surface(
                        color = EmeraldVerified.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "ALPHA ON • CHECKERBOARD PREVIEW ONLY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormatBadge(
    format: MediaFormat,
    modifier: Modifier = Modifier
) {
    val badgeColor = when {
        format.isAnimated -> SignalCrimson
        format.isVector -> BlueprintCobalt
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = badgeColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = format.badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

/**
 * Nothing OS inspired 3x2 technical dot-matrix decorative mark.
 */
@Composable
fun DotMatrixMark(
    color: Color = SignalCrimson,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}
