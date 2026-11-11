package com.example.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BackgroundMode
import com.example.data.LoopMode
import com.example.data.MediaItemEntity
import com.example.data.ScaleMode
import com.example.data.SizePreset
import com.example.data.StudioPreferences
import com.example.data.WidgetConfigEntity
import com.example.ui.theme.BlueprintCobalt
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.SignalCrimson
import kotlin.math.roundToInt

@Composable
fun WidgetEditorScreen(
    draft: WidgetConfigEntity,
    mediaItems: List<MediaItemEntity>,
    preferences: StudioPreferences,
    activeTab: EditorInspectorTab,
    showCheckerboard: Boolean,
    onSelectTab: (EditorInspectorTab) -> Unit,
    onToggleCheckerboard: () -> Unit,
    onPickDeviceFile: () -> Unit,
    onSelectLibraryMedia: (MediaItemEntity) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateSizePreset: (SizePreset) -> Unit,
    onUpdateDimensions: (Int, Int) -> Unit,
    onUpdateMaintainAspect: (Boolean) -> Unit,
    onUpdateScaleMode: (ScaleMode) -> Unit,
    onUpdateScaleFactor: (Float) -> Unit,
    onUpdateOffset: (Float, Float) -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onUpdateBackgroundMode: (BackgroundMode) -> Unit,
    onUpdateBackgroundColor: (Long) -> Unit,
    onUpdateCornerRadius: (Int) -> Unit,
    onUpdatePreserveTransparency: (Boolean) -> Unit,
    onUpdateAnimationEnabled: (Boolean) -> Unit,
    onUpdateAnimationSpeed: (Float) -> Unit,
    onUpdateLoopMode: (LoopMode) -> Unit,
    onUpdateSelectedFrame: (Int) -> Unit,
    onResetDraft: () -> Unit,
    onSaveWidget: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideTablet = maxWidth >= 720.dp

        if (isWideTablet) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Left Pane: Sticky Live Preview Canvas + Save Bar
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                ) {
                    EditorPreviewPane(
                        draft = draft,
                        showCheckerboard = showCheckerboard,
                        highContrastCheckerboard = preferences.checkerboardHighContrast,
                        onToggleCheckerboard = onToggleCheckerboard,
                        onTogglePlayPause = { onUpdateAnimationEnabled(!draft.isAnimationEnabled) },
                        onResetDraft = onResetDraft,
                        onSaveOnly = { onSaveWidget(false) },
                        onSaveAndPin = { onSaveWidget(true) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Right Pane: Tabbed Studio Configuration Inspector
                Column(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                ) {
                    EditorInspectorPane(
                        draft = draft,
                        mediaItems = mediaItems,
                        activeTab = activeTab,
                        onSelectTab = onSelectTab,
                        onPickDeviceFile = onPickDeviceFile,
                        onSelectLibraryMedia = onSelectLibraryMedia,
                        onUpdateTitle = onUpdateTitle,
                        onUpdateSizePreset = onUpdateSizePreset,
                        onUpdateDimensions = onUpdateDimensions,
                        onUpdateMaintainAspect = onUpdateMaintainAspect,
                        onUpdateScaleMode = onUpdateScaleMode,
                        onUpdateScaleFactor = onUpdateScaleFactor,
                        onUpdateOffset = onUpdateOffset,
                        onUpdateOpacity = onUpdateOpacity,
                        onUpdateBackgroundMode = onUpdateBackgroundMode,
                        onUpdateBackgroundColor = onUpdateBackgroundColor,
                        onUpdateCornerRadius = onUpdateCornerRadius,
                        onUpdatePreserveTransparency = onUpdatePreserveTransparency,
                        onUpdateAnimationEnabled = onUpdateAnimationEnabled,
                        onUpdateAnimationSpeed = onUpdateAnimationSpeed,
                        onUpdateLoopMode = onUpdateLoopMode,
                        onUpdateSelectedFrame = onUpdateSelectedFrame,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Compact / Phone Layout: Scrollable vertical studio
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                EditorPreviewPane(
                    draft = draft,
                    showCheckerboard = showCheckerboard,
                    highContrastCheckerboard = preferences.checkerboardHighContrast,
                    onToggleCheckerboard = onToggleCheckerboard,
                    onTogglePlayPause = { onUpdateAnimationEnabled(!draft.isAnimationEnabled) },
                    onResetDraft = onResetDraft,
                    onSaveOnly = { onSaveWidget(false) },
                    onSaveAndPin = { onSaveWidget(true) },
                    modifier = Modifier.fillMaxWidth()
                )

                EditorInspectorPane(
                    draft = draft,
                    mediaItems = mediaItems,
                    activeTab = activeTab,
                    onSelectTab = onSelectTab,
                    onPickDeviceFile = onPickDeviceFile,
                    onSelectLibraryMedia = onSelectLibraryMedia,
                    onUpdateTitle = onUpdateTitle,
                    onUpdateSizePreset = onUpdateSizePreset,
                    onUpdateDimensions = onUpdateDimensions,
                    onUpdateMaintainAspect = onUpdateMaintainAspect,
                    onUpdateScaleMode = onUpdateScaleMode,
                    onUpdateScaleFactor = onUpdateScaleFactor,
                    onUpdateOffset = onUpdateOffset,
                    onUpdateOpacity = onUpdateOpacity,
                    onUpdateBackgroundMode = onUpdateBackgroundMode,
                    onUpdateBackgroundColor = onUpdateBackgroundColor,
                    onUpdateCornerRadius = onUpdateCornerRadius,
                    onUpdatePreserveTransparency = onUpdatePreserveTransparency,
                    onUpdateAnimationEnabled = onUpdateAnimationEnabled,
                    onUpdateAnimationSpeed = onUpdateAnimationSpeed,
                    onUpdateLoopMode = onUpdateLoopMode,
                    onUpdateSelectedFrame = onUpdateSelectedFrame,
                    isEmbeddedInOuterScroll = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EditorPreviewPane(
    draft: WidgetConfigEntity,
    showCheckerboard: Boolean,
    highContrastCheckerboard: Boolean,
    onToggleCheckerboard: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onResetDraft: () -> Unit,
    onSaveOnly: () -> Unit,
    onSaveAndPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixMark()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE STUDIO CANVAS",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                FormatBadge(format = draft.mediaFormat)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Live Widget Preview Box
            LiveWidgetPreviewBox(
                config = draft,
                showCheckerboard = showCheckerboard,
                highContrastCheckerboard = highContrastCheckerboard,
                previewResolutionPx = 360,
                showDimensionGuides = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .testTag("editor_live_preview_box")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Toolbar: Checkerboard toggle, Play/Pause (for animated), Reset
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = showCheckerboard,
                    onClick = onToggleCheckerboard,
                    label = { Text("Checkerboard Grid") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("toggle_checkerboard_chip")
                )

                if (draft.mediaFormat.isAnimated) {
                    FilterChip(
                        selected = draft.isAnimationEnabled,
                        onClick = onTogglePlayPause,
                        label = { Text(if (draft.isAnimationEnabled) "Playing" else "Paused") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (draft.isAnimationEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("toggle_animation_chip")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = onResetDraft,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("reset_draft_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save & Place Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSaveOnly,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_widget_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Preset", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onSaveAndPin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(48.dp)
                        .testTag("save_and_pin_button")
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Place Widget", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditorInspectorPane(
    draft: WidgetConfigEntity,
    mediaItems: List<MediaItemEntity>,
    activeTab: EditorInspectorTab,
    onSelectTab: (EditorInspectorTab) -> Unit,
    onPickDeviceFile: () -> Unit,
    onSelectLibraryMedia: (MediaItemEntity) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateSizePreset: (SizePreset) -> Unit,
    onUpdateDimensions: (Int, Int) -> Unit,
    onUpdateMaintainAspect: (Boolean) -> Unit,
    onUpdateScaleMode: (ScaleMode) -> Unit,
    onUpdateScaleFactor: (Float) -> Unit,
    onUpdateOffset: (Float, Float) -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onUpdateBackgroundMode: (BackgroundMode) -> Unit,
    onUpdateBackgroundColor: (Long) -> Unit,
    onUpdateCornerRadius: (Int) -> Unit,
    onUpdatePreserveTransparency: (Boolean) -> Unit,
    onUpdateAnimationEnabled: (Boolean) -> Unit,
    onUpdateAnimationSpeed: (Float) -> Unit,
    onUpdateLoopMode: (LoopMode) -> Unit,
    onUpdateSelectedFrame: (Int) -> Unit,
    isEmbeddedInOuterScroll: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                EditorInspectorTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("inspector_tab_${tab.name.lowercase()}")
                    )
                }
            }

            val bodyModifier = if (isEmbeddedInOuterScroll) {
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = bodyModifier
            ) {
                when (activeTab) {
                    EditorInspectorTab.MEDIA_AND_SIZE -> {
                        MediaAndSizeTabContent(
                            draft = draft,
                            mediaItems = mediaItems,
                            onPickDeviceFile = onPickDeviceFile,
                            onSelectLibraryMedia = onSelectLibraryMedia,
                            onUpdateTitle = onUpdateTitle,
                            onUpdateSizePreset = onUpdateSizePreset,
                            onUpdateDimensions = onUpdateDimensions,
                            onUpdateMaintainAspect = onUpdateMaintainAspect
                        )
                    }
                    EditorInspectorTab.SCALE_AND_POS -> {
                        ScaleAndPositionTabContent(
                            draft = draft,
                            onUpdateScaleMode = onUpdateScaleMode,
                            onUpdateScaleFactor = onUpdateScaleFactor,
                            onUpdateOffset = onUpdateOffset
                        )
                    }
                    EditorInspectorTab.BACKGROUND_ALPHA -> {
                        BackgroundAndAlphaTabContent(
                            draft = draft,
                            onUpdatePreserveTransparency = onUpdatePreserveTransparency,
                            onUpdateOpacity = onUpdateOpacity,
                            onUpdateBackgroundMode = onUpdateBackgroundMode,
                            onUpdateBackgroundColor = onUpdateBackgroundColor,
                            onUpdateCornerRadius = onUpdateCornerRadius
                        )
                    }
                    EditorInspectorTab.ANIMATION_SVG -> {
                        AnimationAndSvgTabContent(
                            draft = draft,
                            onUpdateAnimationEnabled = onUpdateAnimationEnabled,
                            onUpdateAnimationSpeed = onUpdateAnimationSpeed,
                            onUpdateLoopMode = onUpdateLoopMode,
                            onUpdateSelectedFrame = onUpdateSelectedFrame
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaAndSizeTabContent(
    draft: WidgetConfigEntity,
    mediaItems: List<MediaItemEntity>,
    onPickDeviceFile: () -> Unit,
    onSelectLibraryMedia: (MediaItemEntity) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateSizePreset: (SizePreset) -> Unit,
    onUpdateDimensions: (Int, Int) -> Unit,
    onUpdateMaintainAspect: (Boolean) -> Unit
) {
    // Step 1 & 2: Source File Selection & Title
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("1. Media Source & Label", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = onPickDeviceFile,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("editor_pick_saf_button")
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Select File (SAF)")
        }
    }

    // Quick Starter / Library Switcher
    if (mediaItems.isNotEmpty()) {
        Text(
            text = "Or switch between library & starter graphics:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(mediaItems, key = { it.id }) { item ->
                val isSelected = item.id == draft.mediaId
                Surface(
                    color = if (isSelected) SignalCrimson.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) SignalCrimson else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .clickable { onSelectLibraryMedia(item) }
                        .testTag("quick_media_${item.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        FormatBadge(format = item.mediaFormat)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = draft.title,
        onValueChange = onUpdateTitle,
        label = { Text("Widget Name") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("widget_title_input")
    )

    // Step 4: Widget Resizing Controls
    Text("2. Widget Size Preset & Dimensions", style = MaterialTheme.typography.titleMedium)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SizePreset.entries.forEach { preset ->
            val selected = draft.sizePreset == preset
            FilterChip(
                selected = selected,
                onClick = { onUpdateSizePreset(preset) },
                label = { Text("${preset.label} (${preset.gridLabel})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SignalCrimson.copy(alpha = 0.15f),
                    selectedLabelColor = SignalCrimson
                ),
                modifier = Modifier.testTag("size_preset_${preset.name.lowercase()}")
            )
        }
    }

    // Width & Height Sliders
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Target Width", style = MaterialTheme.typography.bodyMedium)
            Text("${draft.targetWidthDp} dp", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = draft.targetWidthDp.toFloat(),
            onValueChange = { newW ->
                val wInt = newW.roundToInt()
                val hInt = if (draft.maintainAspectRatio) {
                    val ratio = draft.targetHeightDp.toFloat() / draft.targetWidthDp.coerceAtLeast(1).toFloat()
                    (wInt * ratio).roundToInt().coerceIn(60, 640)
                } else {
                    draft.targetHeightDp
                }
                onUpdateDimensions(wInt, hInt)
            },
            valueRange = 60f..640f,
            modifier = Modifier.testTag("width_slider")
        )
    }

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Target Height", style = MaterialTheme.typography.bodyMedium)
            Text("${draft.targetHeightDp} dp", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = draft.targetHeightDp.toFloat(),
            onValueChange = { newH ->
                onUpdateDimensions(draft.targetWidthDp, newH.roundToInt())
            },
            valueRange = 60f..640f,
            modifier = Modifier.testTag("height_slider")
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Maintain Aspect Ratio", style = MaterialTheme.typography.titleSmall)
            Text(
                "Prevents distortion when resizing on your home screen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = draft.maintainAspectRatio,
            onCheckedChange = onUpdateMaintainAspect,
            modifier = Modifier.testTag("maintain_aspect_switch")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScaleAndPositionTabContent(
    draft: WidgetConfigEntity,
    onUpdateScaleMode: (ScaleMode) -> Unit,
    onUpdateScaleFactor: (Float) -> Unit,
    onUpdateOffset: (Float, Float) -> Unit
) {
    Text("Scaling Mode", style = MaterialTheme.typography.titleMedium)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScaleMode.entries.forEach { mode ->
            FilterChip(
                selected = draft.scaleMode == mode,
                onClick = { onUpdateScaleMode(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("scale_mode_${mode.name.lowercase()}")
            )
        }
    }

    Text(
        text = draft.scaleMode.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zoom / Scale Multiplier", style = MaterialTheme.typography.bodyMedium)
            Text("${(draft.scaleFactor * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = draft.scaleFactor,
            onValueChange = onUpdateScaleFactor,
            valueRange = 0.5f..2.0f,
            modifier = Modifier.testTag("scale_factor_slider")
        )
    }

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Horizontal Position (X Offset)", style = MaterialTheme.typography.bodyMedium)
            Text("${(draft.offsetX * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = draft.offsetX,
            onValueChange = { onUpdateOffset(it, draft.offsetY) },
            valueRange = -1.0f..1.0f,
            modifier = Modifier.testTag("offset_x_slider")
        )
    }

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Vertical Position (Y Offset)", style = MaterialTheme.typography.bodyMedium)
            Text("${(draft.offsetY * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = draft.offsetY,
            onValueChange = { onUpdateOffset(draft.offsetX, it) },
            valueRange = -1.0f..1.0f,
            modifier = Modifier.testTag("offset_y_slider")
        )
    }

    OutlinedButton(
        onClick = {
            onUpdateScaleFactor(1.0f)
            onUpdateOffset(0f, 0f)
        },
        shape = RoundedCornerShape(10.dp)
    ) {
        Text("Center & Reset Scale (100%)")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundAndAlphaTabContent(
    draft: WidgetConfigEntity,
    onUpdatePreserveTransparency: (Boolean) -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onUpdateBackgroundMode: (BackgroundMode) -> Unit,
    onUpdateBackgroundColor: (Long) -> Unit,
    onUpdateCornerRadius: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Preserve Original Alpha Transparency", style = MaterialTheme.typography.titleMedium)
            Text(
                "Zero unwanted white/black fill, zero borders or shadows over wallpaper",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = draft.preserveTransparency,
            onCheckedChange = onUpdatePreserveTransparency,
            modifier = Modifier.testTag("preserve_transparency_switch")
        )
    }

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Widget Opacity", style = MaterialTheme.typography.bodyMedium)
            Text("${(draft.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = draft.opacity,
            onValueChange = onUpdateOpacity,
            valueRange = 0.1f..1.0f,
            modifier = Modifier.testTag("opacity_slider")
        )
    }

    Text("Widget Background Mode", style = MaterialTheme.typography.titleMedium)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackgroundMode.entries.forEach { bgMode ->
            FilterChip(
                selected = draft.backgroundMode == bgMode,
                onClick = { onUpdateBackgroundMode(bgMode) },
                label = { Text(bgMode.label) },
                modifier = Modifier.testTag("bg_mode_${bgMode.name.lowercase()}")
            )
        }
    }

    if (draft.backgroundMode in listOf(BackgroundMode.SOLID_COLOR, BackgroundMode.CUSTOM_COLOR, BackgroundMode.ROUNDED)) {
        Text("Backdrop Color Palette", style = MaterialTheme.typography.titleSmall)
        val studioColors = listOf(
            0xFFFFFFFFL to "Alabaster",
            0xFF111216L to "Obsidian",
            0xFFE11D48L to "Crimson",
            0xFF2563EBL to "Cobalt",
            0xFF10B981L to "Emerald",
            0xFFF59E0BL to "Amber",
            0xFF8B5CF6L to "Violet",
            0xDD15171EL to "Frosted Dark"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            studioColors.forEach { (hex, _) ->
                val isSelected = draft.backgroundColorHex == hex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) SignalCrimson else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onUpdateBackgroundColor(hex) }
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected color",
                            tint = if (hex == 0xFFFFFFFFL) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (draft.backgroundMode == BackgroundMode.ROUNDED) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rounded Corner Radius", style = MaterialTheme.typography.bodyMedium)
                Text("${draft.cornerRadiusDp} dp", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = draft.cornerRadiusDp.toFloat(),
                onValueChange = { onUpdateCornerRadius(it.roundToInt()) },
                valueRange = 4f..64f
            )
        }
    }
}

@Composable
private fun AnimationAndSvgTabContent(
    draft: WidgetConfigEntity,
    onUpdateAnimationEnabled: (Boolean) -> Unit,
    onUpdateAnimationSpeed: (Float) -> Unit,
    onUpdateLoopMode: (LoopMode) -> Unit,
    onUpdateSelectedFrame: (Int) -> Unit
) {
    // Animated GIF / Animated SVG Controls
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Enable Home-Screen Animation", style = MaterialTheme.typography.titleMedium)
            Text(
                if (draft.mediaFormat.isAnimated) {
                    "Uses battery-safe native ViewFlipper frame scheduling"
                } else {
                    "Current format (${draft.mediaFormat.badgeText}) is a static graphic"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = draft.isAnimationEnabled && draft.mediaFormat.isAnimated,
            onCheckedChange = onUpdateAnimationEnabled,
            enabled = draft.mediaFormat.isAnimated,
            modifier = Modifier.testTag("enable_animation_switch")
        )
    }

    if (draft.mediaFormat.isAnimated || draft.totalFrames > 1) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Animation Playback Speed", style = MaterialTheme.typography.bodyMedium)
                Text("${String.format("%.1f", draft.animationSpeedMultiplier)}×", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = draft.animationSpeedMultiplier,
                onValueChange = onUpdateAnimationSpeed,
                valueRange = 0.5f..2.0f,
                modifier = Modifier.testTag("animation_speed_slider")
            )
        }

        Text("Loop Behavior", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LoopMode.entries.forEach { mode ->
                FilterChip(
                    selected = draft.loopMode == mode,
                    onClick = { onUpdateLoopMode(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (draft.isAnimationEnabled) "Starting Frame Index" else "Static Fallback Frame Selection",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Frame ${draft.selectedFrameIndex + 1} / ${draft.totalFrames.coerceAtLeast(1)}", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = draft.selectedFrameIndex.toFloat(),
                onValueChange = { onUpdateSelectedFrame(it.roundToInt()) },
                valueRange = 0f..(draft.totalFrames - 1).coerceAtLeast(1).toFloat(),
                steps = (draft.totalFrames - 2).coerceAtLeast(0),
                modifier = Modifier.testTag("starting_frame_slider")
            )
        }
    }

    // SVG Vector & Animation Compatibility Report Box
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "SVG & GIF Home-Screen Engine Diagnostics",
                style = MaterialTheme.typography.titleSmall,
                color = BlueprintCobalt
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (draft.svgCompatibilityNotes.isNotBlank()) {
                Text(
                    text = draft.svgCompatibilityNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldVerified
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = "• Vector SVGs rasterize dynamically at exact target widget pixel dimensions upon placement and launcher resize.\n" +
                    "• Supported Animated SVG subset: <animate>, <animateTransform> (rotate, scale, translate), and inline CSS opacity/transform.\n" +
                    "• Android home-screen widgets use bounded frame buffers and automatically pause during Battery Saver / Doze mode.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
