package com.example.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MediaItemEntity
import com.example.data.StudioPreferences
import com.example.data.WidgetConfigEntity
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.SignalCrimson
import com.example.utils.DimensionUtils

@Composable
fun HomeScreen(
    widgets: List<WidgetConfigEntity>,
    mediaItems: List<MediaItemEntity>,
    preferences: StudioPreferences,
    onCreateNewWidget: () -> Unit,
    onEditWidget: (WidgetConfigEntity) -> Unit,
    onDuplicateWidget: (WidgetConfigEntity) -> Unit,
    onDeleteWidget: (Int) -> Unit,
    onPinWidget: (WidgetConfigEntity) -> Unit,
    onToggleGuide: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var guideExpanded by rememberSaveable { mutableStateOf(preferences.showPlacementGuide) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 290.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("home_widgets_grid")
    ) {
        // 1. Studio Hero Banner Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            StudioHeroCard(
                widgetCount = widgets.size,
                mediaCount = mediaItems.size,
                onCreateNewWidget = onCreateNewWidget
            )
        }

        // 2. Collapsible Home-Screen Placement & Resize Instructions
        item(span = { GridItemSpan(maxLineSpan) }) {
            PlacementInstructionsCard(
                expanded = guideExpanded,
                onToggle = {
                    guideExpanded = !guideExpanded
                    onToggleGuide(guideExpanded)
                }
            )
        }

        // 3. Section Header
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixMark()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MY WIDGETS (${widgets.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Text(
                    text = "PURE DESKTOP ART MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Empty state or Widget Cards
        if (widgets.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyWidgetsCard(onCreateNewWidget = onCreateNewWidget)
            }
        } else {
            items(widgets, key = { it.widgetId }) { widget ->
                WidgetGalleryCard(
                    config = widget,
                    highContrastCheckerboard = preferences.checkerboardHighContrast,
                    onEdit = { onEditWidget(widget) },
                    onDuplicate = { onDuplicateWidget(widget) },
                    onDelete = { onDeleteWidget(widget.widgetId) },
                    onPin = { onPinWidget(widget) }
                )
            }
        }
    }
}

@Composable
private fun StudioHeroCard(
    widgetCount: Int,
    mediaCount: Int,
    onCreateNewWidget: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_kaps_studio_hero_1794371889218),
                    contentDescription = "KAPS Widget Studio Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color(0xCC111216),
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SignalCrimson)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KAPS • OFFLINE WIDGET STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transparent PNG, GIF & Vector SVG Widgets",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$widgetCount configured widgets • $mediaCount library graphics • True ARGB_8888 alpha preservation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onCreateNewWidget,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.testTag("create_new_widget_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Widget", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlacementInstructionsCard(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SignalCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "How to Place & Resize KAPS Widgets on Your Home Screen",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.testTag("toggle_instructions_button")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle placement instructions"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    InstructionStepRow("01", "Tap 'Create Widget' to import any PNG, JPG, WebP, Animated GIF, or SVG file and customize transparency, scale, or speed.")
                    InstructionStepRow("02", "Tap 'Place on Home Screen' inside KAPS, OR long-press an empty area on your Android tablet/phone home screen and choose Widgets -> KAPS.")
                    InstructionStepRow("03", "Long-press any placed KAPS widget on your home screen to drag its launcher resize handles freely — SVGs and images automatically re-render at the new grid size.")
                    InstructionStepRow("04", "Placed widgets operate in Pure Desktop Art Mode (no accidental app launches when tapping your wallpaper). Edit anytime from KAPS.")
                }
            }
        }
    }
}

@Composable
private fun InstructionStepRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = SignalCrimson,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WidgetGalleryCard(
    config: WidgetConfigEntity,
    highContrastCheckerboard: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    val (cols, rows) = DimensionUtils.estimateGridCells(config.targetWidthDp, config.targetHeightDp)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("widget_card_${config.widgetId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top bar: Format badge + Placed status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                FormatBadge(format = config.mediaFormat)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (config.isPlacedOnHomeScreen || config.widgetId > 0) {
                        Surface(
                            color = EmeraldVerified.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE #${config.widgetId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldVerified,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "STUDIO PRESET",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Checkerboard Thumbnail Preview
            LiveWidgetPreviewBox(
                config = config,
                showCheckerboard = true,
                highContrastCheckerboard = highContrastCheckerboard,
                previewResolutionPx = 240,
                showDimensionGuides = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Technical Metadata
            Text(
                text = config.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${config.sizePreset.label} (${cols}×${rows} grid • ${config.targetWidthDp}×${config.targetHeightDp}dp) • ${config.scaleMode.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (config.sourceMissing) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Warning: Source media file moved or missing. Tap Edit to relink.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SignalCrimson
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_widget_${config.widgetId}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = onPin,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_widget_${config.widgetId}")
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Place")
                }

                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.testTag("duplicate_widget_${config.widgetId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate widget",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_widget_${config.widgetId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete widget",
                        tint = SignalCrimson
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWidgetsCard(onCreateNewWidget: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Widgets,
                contentDescription = null,
                tint = SignalCrimson,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Widgets Configured Yet",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Create your first transparent PNG sticker, animated GIF, or sharp vector SVG widget.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onCreateNewWidget,
                colors = ButtonDefaults.buttonColors(containerColor = SignalCrimson)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create First Widget")
            }
        }
    }
}
