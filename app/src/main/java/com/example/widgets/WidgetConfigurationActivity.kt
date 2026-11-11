package com.example.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.KapsApplication
import com.example.data.WidgetConfigEntity
import com.example.features.FormatBadge
import com.example.features.LiveWidgetPreviewBox
import com.example.ui.theme.KapsTheme
import com.example.ui.theme.SignalCrimson
import kotlinx.coroutines.launch

/**
 * Native Android AppWidget Configuration Activity (`android.appwidget.action.APPWIDGET_CONFIGURE`).
 * Invoked automatically when the user adds a KAPS widget from the Android Home Screen Widget Picker
 * or long-presses an existing widget to reconfigure it.
 */
class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Default result to CANCELED if user backs out before picking a widget
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as KapsApplication
        val repository = app.repository

        setContent {
            val prefs by repository.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.StudioPreferences()
            )
            val savedWidgets by repository.allWidgetsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            val mediaList by repository.allMediaFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                repository.ensureStarterLibrarySeeded()
            }

            val safLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        val imported = repository.importMediaFromUri(uri)
                        if (imported != null) {
                            val newConfig = WidgetConfigEntity(
                                widgetId = appWidgetId,
                                title = imported.displayName.substringBeforeLast('.'),
                                mediaId = imported.id,
                                sourceUri = imported.originalUri,
                                localFilePath = imported.localFilePath,
                                mediaFormat = imported.mediaFormat,
                                isAnimationEnabled = imported.mediaFormat.isAnimated,
                                totalFrames = imported.frameCount,
                                isPlacedOnHomeScreen = true
                            )
                            repository.saveWidgetConfig(newConfig)
                            WidgetUpdater.updateSingleWidget(this@WidgetConfigurationActivity, appWidgetId, repository)
                            completeConfigurationWithSuccess()
                        }
                    }
                }
            }

            KapsTheme(themeMode = prefs.themeMode) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Select Graphic for Home-Screen Widget #$appWidgetId",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            actions = {
                                Button(
                                    onClick = {
                                        safLauncher.launch(arrayOf("image/*", "image/svg+xml", "image/gif", "image/png", "image/webp"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SignalCrimson),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import File")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "Tap any configured KAPS widget or starter graphic below to place it directly on your home screen:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 240.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(savedWidgets, key = { "w_${it.widgetId}" }) { preset ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            FormatBadge(format = preset.mediaFormat)
                                            Text(
                                                text = "${preset.targetWidthDp}×${preset.targetHeightDp}dp",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LiveWidgetPreviewBox(
                                            config = preset,
                                            showCheckerboard = true,
                                            highContrastCheckerboard = prefs.checkerboardHighContrast,
                                            previewResolutionPx = 220,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1.3f)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    repository.bindPresetToAppWidgetId(preset, appWidgetId)
                                                    WidgetUpdater.updateSingleWidget(
                                                        this@WidgetConfigurationActivity,
                                                        appWidgetId,
                                                        repository
                                                    )
                                                    completeConfigurationWithSuccess()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SignalCrimson,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Place This Widget", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            items(mediaList, key = { "m_${it.id}" }) { media ->
                                val tempConfig = WidgetConfigEntity(
                                    widgetId = appWidgetId,
                                    title = media.displayName.substringBeforeLast('.'),
                                    mediaId = media.id,
                                    sourceUri = media.originalUri,
                                    localFilePath = media.localFilePath,
                                    mediaFormat = media.mediaFormat,
                                    isAnimationEnabled = media.mediaFormat.isAnimated,
                                    totalFrames = media.frameCount,
                                    isPlacedOnHomeScreen = true
                                )
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        FormatBadge(format = media.mediaFormat)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LiveWidgetPreviewBox(
                                            config = tempConfig,
                                            showCheckerboard = true,
                                            highContrastCheckerboard = prefs.checkerboardHighContrast,
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
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.saveWidgetConfig(tempConfig)
                                                    WidgetUpdater.updateSingleWidget(
                                                        this@WidgetConfigurationActivity,
                                                        appWidgetId,
                                                        repository
                                                    )
                                                    completeConfigurationWithSuccess()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Use Library Graphic")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun completeConfigurationWithSuccess() {
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
