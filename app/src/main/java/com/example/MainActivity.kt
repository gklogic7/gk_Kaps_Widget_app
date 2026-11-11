package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.features.DotMatrixMark
import com.example.features.HomeScreen
import com.example.features.ImageLibraryScreen
import com.example.features.SettingsScreen
import com.example.features.StudioSection
import com.example.features.StudioViewModel
import com.example.features.WidgetEditorScreen
import com.example.ui.theme.KapsTheme
import com.example.ui.theme.SignalCrimson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StudioViewModel = viewModel()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            KapsTheme(themeMode = preferences.themeMode) {
                KapsStudioApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KapsStudioApp(
    viewModel: StudioViewModel
) {
    val widgets by viewModel.widgets.collectAsStateWithLifecycle()
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val activeDraft by viewModel.activeDraft.collectAsStateWithLifecycle()
    val activeInspectorTab by viewModel.activeInspectorTab.collectAsStateWithLifecycle()
    val showCheckerboard by viewModel.showCheckerboardInPreview.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val storageBytesUsed by viewModel.storageBytesUsed.collectAsStateWithLifecycle()

    // Ensure Back button returns to My Widgets when in sub-sections
    BackHandler(enabled = currentSection != StudioSection.MY_WIDGETS) {
        viewModel.selectSection(StudioSection.MY_WIDGETS)
    }

    val safMimeTypes = arrayOf(
        "image/*",
        "image/png",
        "image/jpeg",
        "image/webp",
        "image/bmp",
        "image/gif",
        "image/svg+xml"
    )

    val editorSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromSafUri(uri, openInEditorAfterImport = true)
        }
    }

    val librarySafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromSafUri(uri, openInEditorAfterImport = false)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                KapsStudioTopBar(
                    currentSection = currentSection,
                    statusMessage = statusMessage,
                    onDismissStatus = viewModel::dismissStatusMessage
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        StudioSection.entries.forEach { section ->
                            NavigationBarItem(
                                selected = currentSection == section,
                                onClick = { viewModel.selectSection(section) },
                                icon = {
                                    Icon(
                                        imageVector = sectionIcon(section),
                                        contentDescription = section.title
                                    )
                                },
                                label = { Text(section.title) },
                                modifier = Modifier.testTag("nav_tab_${section.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_navigation_rail")
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StudioSection.entries.forEach { section ->
                            NavigationRailItem(
                                selected = currentSection == section,
                                onClick = { viewModel.selectSection(section) },
                                icon = {
                                    Icon(
                                        imageVector = sectionIcon(section),
                                        contentDescription = section.title
                                    )
                                },
                                label = { Text(section.title) },
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .testTag("rail_tab_${section.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentSection) {
                        StudioSection.MY_WIDGETS -> {
                            HomeScreen(
                                widgets = widgets,
                                mediaItems = mediaItems,
                                preferences = preferences,
                                onCreateNewWidget = { viewModel.startNewWidgetWizard() },
                                onEditWidget = { viewModel.openWidgetForEdit(it) },
                                onDuplicateWidget = { viewModel.duplicateWidget(it) },
                                onDeleteWidget = { viewModel.deleteWidget(it) },
                                onPinWidget = { viewModel.requestPinExistingWidget(it) },
                                onToggleGuide = { viewModel.setShowPlacementGuide(it) }
                            )
                        }
                        StudioSection.CREATE_WIDGET -> {
                            WidgetEditorScreen(
                                draft = activeDraft,
                                mediaItems = mediaItems,
                                preferences = preferences,
                                activeTab = activeInspectorTab,
                                showCheckerboard = showCheckerboard,
                                onSelectTab = viewModel::selectInspectorTab,
                                onToggleCheckerboard = viewModel::toggleCheckerboardPreview,
                                onPickDeviceFile = { editorSafLauncher.launch(safMimeTypes) },
                                onSelectLibraryMedia = viewModel::applyMediaToDraft,
                                onUpdateTitle = viewModel::updateDraftTitle,
                                onUpdateSizePreset = viewModel::updateDraftSizePreset,
                                onUpdateDimensions = viewModel::updateDraftDimensions,
                                onUpdateMaintainAspect = viewModel::updateDraftMaintainAspectRatio,
                                onUpdateScaleMode = viewModel::updateDraftScaleMode,
                                onUpdateScaleFactor = viewModel::updateDraftScaleFactor,
                                onUpdateOffset = viewModel::updateDraftOffset,
                                onUpdateOpacity = viewModel::updateDraftOpacity,
                                onUpdateBackgroundMode = viewModel::updateDraftBackgroundMode,
                                onUpdateBackgroundColor = viewModel::updateDraftBackgroundColor,
                                onUpdateCornerRadius = viewModel::updateDraftCornerRadius,
                                onUpdatePreserveTransparency = viewModel::updateDraftPreserveTransparency,
                                onUpdateAnimationEnabled = viewModel::updateDraftAnimationEnabled,
                                onUpdateAnimationSpeed = viewModel::updateDraftAnimationSpeed,
                                onUpdateLoopMode = viewModel::updateDraftLoopMode,
                                onUpdateSelectedFrame = viewModel::updateDraftSelectedFrame,
                                onResetDraft = viewModel::resetDraftConfiguration,
                                onSaveWidget = { pin -> viewModel.saveDraftWidget(pin) }
                            )
                        }
                        StudioSection.IMAGE_LIBRARY -> {
                            ImageLibraryScreen(
                                mediaItems = mediaItems,
                                preferences = preferences,
                                onImportFileSaf = { librarySafLauncher.launch(safMimeTypes) },
                                onUseMediaForWidget = { viewModel.startNewWidgetWizard(it) },
                                onDeleteMedia = viewModel::deleteMediaItem,
                                onVerifySources = viewModel::verifyMediaSourcesNow,
                                onRestoreStarters = viewModel::restoreStarterAssets
                            )
                        }
                        StudioSection.SETTINGS -> {
                            SettingsScreen(
                                preferences = preferences,
                                storageBytesUsed = storageBytesUsed,
                                onSelectThemeMode = viewModel::setThemeMode,
                                onToggleHighContrastCheckerboard = viewModel::setCheckerboardHighContrast,
                                onUpdateMaxFps = viewModel::setMaxAnimationFps,
                                onTogglePauseOnBatterySaver = viewModel::setPauseOnBatterySaver,
                                onUpdateMaxResolutionPx = viewModel::setMaxWidgetResolutionPx,
                                onPurgeCache = viewModel::purgeRenderCache,
                                onRestoreStarters = viewModel::restoreStarterAssets
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KapsStudioTopBar(
    currentSection: StudioSection,
    statusMessage: String?,
    onDismissStatus: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(SignalCrimson)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KAPS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentSection.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixMark()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PNG • GIF • SVG WIDGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = statusMessage != null) {
                Surface(
                    color = SignalCrimson,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissStatus,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sectionIcon(section: StudioSection): ImageVector {
    return when (section) {
        StudioSection.MY_WIDGETS -> Icons.Default.Widgets
        StudioSection.CREATE_WIDGET -> Icons.Default.AddBox
        StudioSection.IMAGE_LIBRARY -> Icons.Default.Collections
        StudioSection.SETTINGS -> Icons.Default.Settings
    }
}
