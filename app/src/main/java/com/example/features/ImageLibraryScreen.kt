package com.example.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MediaItemEntity
import com.example.data.StudioPreferences
import com.example.data.WidgetConfigEntity
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.SignalCrimson

private enum class LibraryFilter(val label: String) {
    ALL("All Media"),
    STATIC("Static PNG / WebP / JPG"),
    ANIMATED("Animated GIF / WebP"),
    VECTOR("Vector & Anim SVG")
}

@Composable
fun ImageLibraryScreen(
    mediaItems: List<MediaItemEntity>,
    preferences: StudioPreferences,
    onImportFileSaf: () -> Unit,
    onUseMediaForWidget: (MediaItemEntity) -> Unit,
    onDeleteMedia: (MediaItemEntity) -> Unit,
    onVerifySources: () -> Unit,
    onRestoreStarters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }

    val filteredItems = mediaItems.filter { item ->
        when (selectedFilter) {
            LibraryFilter.ALL -> true
            LibraryFilter.STATIC -> !item.mediaFormat.isAnimated && !item.mediaFormat.isVector
            LibraryFilter.ANIMATED -> item.mediaFormat.isAnimated && !item.mediaFormat.isVector
            LibraryFilter.VECTOR -> item.mediaFormat.isVector
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("image_library_grid")
    ) {
        // Header & SAF Import Action Bar
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DotMatrixMark()
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "IMAGE, GIF & SVG LIBRARY",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Zero-permission Storage Access Framework (SAF) • Local sandbox preserves alpha channels offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onImportFileSaf,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SignalCrimson,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("library_import_saf_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import File", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filter chips & utility buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LibraryFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onVerifySources,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Files")
                        }
                        OutlinedButton(
                            onClick = onRestoreStarters,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Samples")
                        }
                    }
                }
            }
        }

        items(filteredItems, key = { it.id }) { media ->
            MediaLibraryCard(
                media = media,
                highContrastCheckerboard = preferences.checkerboardHighContrast,
                onUseForWidget = { onUseMediaForWidget(media) },
                onDelete = { onDeleteMedia(media) }
            )
        }
    }
}

@Composable
private fun MediaLibraryCard(
    media: MediaItemEntity,
    highContrastCheckerboard: Boolean,
    onUseForWidget: () -> Unit,
    onDelete: () -> Unit
) {
    val previewConfig = WidgetConfigEntity(
        widgetId = -999,
        title = media.displayName,
        mediaId = media.id,
        sourceUri = media.originalUri,
        localFilePath = media.localFilePath,
        mediaFormat = media.mediaFormat,
        isAnimationEnabled = media.mediaFormat.isAnimated,
        totalFrames = media.frameCount
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("media_card_${media.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                FormatBadge(format = media.mediaFormat)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (media.isSourceAccessible) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (media.isSourceAccessible) EmeraldVerified else AmberWarning,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (media.isBuiltInSample) "STARTER" else if (media.isSourceAccessible) "READY" else "MISSING",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (media.isSourceAccessible) EmeraldVerified else AmberWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LiveWidgetPreviewBox(
                config = previewConfig,
                showCheckerboard = true,
                highContrastCheckerboard = highContrastCheckerboard,
                previewResolutionPx = 220,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = media.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val kb = (media.fileSizeBytes / 1024L).coerceAtLeast(1L)
            Text(
                text = "${media.widthPx}×${media.heightPx} px • ${kb} KB • ${if (media.hasTransparency) "Alpha Channel" else "Opaque"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (media.svgCompatibilityNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = media.svgCompatibilityNotes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onUseForWidget,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("use_media_button_${media.id}")
                ) {
                    Text("Use in Widget")
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove from library",
                        tint = SignalCrimson
                    )
                }
            }
        }
    }
}
