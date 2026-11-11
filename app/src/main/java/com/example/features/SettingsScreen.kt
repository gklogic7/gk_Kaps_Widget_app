package com.example.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.StudioPreferences
import com.example.data.StudioThemeMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    preferences: StudioPreferences,
    storageBytesUsed: Long,
    onSelectThemeMode: (StudioThemeMode) -> Unit,
    onToggleHighContrastCheckerboard: (Boolean) -> Unit,
    onUpdateMaxFps: (Int) -> Unit,
    onTogglePauseOnBatterySaver: (Boolean) -> Unit,
    onUpdateMaxResolutionPx: (Int) -> Unit,
    onPurgeCache: () -> Unit,
    onRestoreStarters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("settings_screen")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotMatrixMark()
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "STUDIO & PERFORMANCE SETTINGS",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // 1. Visual Theme Posture
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Studio Visual Theme", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("High-Contrast Alpha Checkerboard", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Enhances visibility of semi-transparent anti-aliased edges inside the editor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.checkerboardHighContrast,
                            onCheckedChange = onToggleHighContrastCheckerboard
                        )
                    }
                }
            }

            // 2. Battery Efficiency & Animation Scheduling
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Battery Efficiency & Widget Animation", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Animated GIF / SVG Frame Rate Cap", style = MaterialTheme.typography.bodyMedium)
                        Text("${preferences.maxAnimationFps} FPS", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = preferences.maxAnimationFps.toFloat(),
                        onValueChange = { onUpdateMaxFps(it.roundToInt()) },
                        valueRange = 2f..16f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pause Animation in Battery Saver / Doze Mode", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Automatically switches animated widgets to their static frame when Android Power Save is active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.pauseOnBatterySaver,
                            onCheckedChange = onTogglePauseOnBatterySaver
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Maximum Static Widget Render Resolution", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(512 to "512 px (Ultra Light)", 720 to "720 px (Balanced)", 960 to "960 px (Tablet Pro)").forEach { (resPx, label) ->
                            FilterChip(
                                selected = preferences.maxWidgetResolutionPx == resPx,
                                onClick = { onUpdateMaxResolutionPx(resPx) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // 3. Offline Privacy, Memory & Cache Control
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Offline Privacy & Memory Footprint", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    val mbUsed = String.format("%.2f", storageBytesUsed / (1024f * 1024f))
                    Text(
                        text = "Current Local Media & Render Cache: $mbUsed MB • 100% Offline • Zero Cloud Uploads • Zero Analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onPurgeCache,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("purge_cache_button")
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Purge Bitmap Cache")
                        }

                        OutlinedButton(
                            onClick = onRestoreStarters,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("restore_samples_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Starter Samples")
                        }
                    }
                }
            }
        }
    }
}
