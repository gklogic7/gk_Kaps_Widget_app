package com.example.features

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KapsApplication
import com.example.core.WidgetRepository
import com.example.data.BackgroundMode
import com.example.data.LoopMode
import com.example.data.MediaFormat
import com.example.data.MediaItemEntity
import com.example.data.ScaleMode
import com.example.data.SizePreset
import com.example.data.StudioPreferences
import com.example.data.StudioThemeMode
import com.example.data.WidgetConfigEntity
import com.example.domain.CreateWidgetUseCase
import com.example.domain.DeleteWidgetUseCase
import com.example.domain.ImportMediaUseCase
import com.example.domain.ResizeWidgetUseCase
import com.example.domain.UpdateWidgetUseCase
import com.example.widgets.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudioSection(val title: String) {
    MY_WIDGETS("My Widgets"),
    CREATE_WIDGET("Create Widget"),
    IMAGE_LIBRARY("Image Library"),
    SETTINGS("Settings")
}

enum class EditorInspectorTab(val title: String) {
    MEDIA_AND_SIZE("Size & Grid"),
    SCALE_AND_POS("Scale & Crop"),
    BACKGROUND_ALPHA("Alpha & BG"),
    ANIMATION_SVG("Anim & SVG")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val kapsApp = application as KapsApplication
    val repository: WidgetRepository = kapsApp.repository

    private val createWidgetUseCase = CreateWidgetUseCase(repository, application)
    private val updateWidgetUseCase = UpdateWidgetUseCase(repository, application)
    private val deleteWidgetUseCase = DeleteWidgetUseCase(repository)
    private val resizeWidgetUseCase = ResizeWidgetUseCase(repository, application)
    private val importMediaUseCase = ImportMediaUseCase(repository)

    val widgets: StateFlow<List<WidgetConfigEntity>> = repository.allWidgetsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val mediaItems: StateFlow<List<MediaItemEntity>> = repository.allMediaFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val preferences: StateFlow<StudioPreferences> = repository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudioPreferences()
    )

    private val _currentSection = MutableStateFlow(StudioSection.MY_WIDGETS)
    val currentSection: StateFlow<StudioSection> = _currentSection.asStateFlow()

    private val _activeInspectorTab = MutableStateFlow(EditorInspectorTab.MEDIA_AND_SIZE)
    val activeInspectorTab: StateFlow<EditorInspectorTab> = _activeInspectorTab.asStateFlow()

    private val _showCheckerboardInPreview = MutableStateFlow(true)
    val showCheckerboardInPreview: StateFlow<Boolean> = _showCheckerboardInPreview.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _storageBytesUsed = MutableStateFlow(0L)
    val storageBytesUsed: StateFlow<Long> = _storageBytesUsed.asStateFlow()

    private val _activeDraft = MutableStateFlow(createEmptyDraft())
    val activeDraft: StateFlow<WidgetConfigEntity> = _activeDraft.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureStarterLibrarySeeded()
            repository.verifyAllSources()
            refreshStorageStats()
            val firstMedia = repository.allMediaFlow.stateIn(viewModelScope).value.firstOrNull()
            if (_activeDraft.value.localFilePath.isEmpty() && firstMedia != null) {
                applyMediaToDraft(firstMedia)
            }
        }
    }

    fun selectSection(section: StudioSection) {
        _currentSection.value = section
        if (section == StudioSection.CREATE_WIDGET && _activeDraft.value.localFilePath.isEmpty()) {
            mediaItems.value.firstOrNull()?.let { applyMediaToDraft(it) }
        }
        if (section == StudioSection.SETTINGS) {
            refreshStorageStats()
        }
    }

    fun selectInspectorTab(tab: EditorInspectorTab) {
        _activeInspectorTab.value = tab
    }

    fun toggleCheckerboardPreview() {
        _showCheckerboardInPreview.update { !it }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    fun startNewWidgetWizard(preselectedMedia: MediaItemEntity? = null) {
        val targetMedia = preselectedMedia ?: mediaItems.value.firstOrNull()
        val draft = createEmptyDraft()
        _activeDraft.value = draft
        if (targetMedia != null) {
            applyMediaToDraft(targetMedia)
        }
        _activeInspectorTab.value = EditorInspectorTab.MEDIA_AND_SIZE
        _currentSection.value = StudioSection.CREATE_WIDGET
    }

    fun openWidgetForEdit(config: WidgetConfigEntity) {
        _activeDraft.value = config
        _activeInspectorTab.value = EditorInspectorTab.MEDIA_AND_SIZE
        _currentSection.value = StudioSection.CREATE_WIDGET
    }

    fun duplicateWidget(config: WidgetConfigEntity) {
        viewModelScope.launch {
            val newId = repository.allocateStudioPresetId()
            val copy = config.copy(
                widgetId = newId,
                title = "${config.title} Copy",
                isPlacedOnHomeScreen = false,
                updatedAt = System.currentTimeMillis()
            )
            createWidgetUseCase(copy)
            _statusMessage.value = "Duplicated '${config.title}'"
        }
    }

    fun deleteWidget(widgetId: Int) {
        viewModelScope.launch {
            deleteWidgetUseCase(widgetId)
            _statusMessage.value = "Widget removed"
        }
    }

    fun importFromSafUri(uri: Uri, openInEditorAfterImport: Boolean = true) {
        viewModelScope.launch {
            val imported = importMediaUseCase(uri)
            if (imported != null) {
                refreshStorageStats()
                _statusMessage.value = "Imported ${imported.displayName} (${imported.mediaFormat.badgeText})"
                if (openInEditorAfterImport) {
                    applyMediaToDraft(imported)
                    _currentSection.value = StudioSection.CREATE_WIDGET
                }
            } else {
                _statusMessage.value = "Could not read selected file. Please verify file permissions."
            }
        }
    }

    fun applyMediaToDraft(media: MediaItemEntity) {
        _activeDraft.update { current ->
            current.copy(
                title = if (current.title.isBlank() || current.title == "Untitled Widget") {
                    media.displayName.substringBeforeLast('.')
                } else {
                    current.title
                },
                mediaId = media.id,
                sourceUri = media.originalUri,
                localFilePath = media.localFilePath,
                mediaFormat = media.mediaFormat,
                totalFrames = media.frameCount.coerceAtLeast(1),
                selectedFrameIndex = 0,
                isAnimationEnabled = media.mediaFormat.isAnimated,
                svgCompatibilityNotes = media.svgCompatibilityNotes,
                sourceMissing = !media.isSourceAccessible
            )
        }
    }

    fun updateDraftTitle(title: String) {
        _activeDraft.update { it.copy(title = title) }
    }

    fun updateDraftSizePreset(preset: SizePreset) {
        _activeDraft.update { current ->
            if (preset == SizePreset.CUSTOM) {
                current.copy(sizePreset = SizePreset.CUSTOM)
            } else {
                current.copy(
                    sizePreset = preset,
                    targetWidthDp = preset.defaultWidthDp,
                    targetHeightDp = preset.defaultHeightDp
                )
            }
        }
    }

    fun updateDraftDimensions(widthDp: Int, heightDp: Int) {
        _activeDraft.update { current ->
            current.copy(
                sizePreset = SizePreset.CUSTOM,
                targetWidthDp = widthDp.coerceIn(48, 800),
                targetHeightDp = heightDp.coerceIn(48, 800)
            )
        }
    }

    fun updateDraftMaintainAspectRatio(maintain: Boolean) {
        _activeDraft.update { it.copy(maintainAspectRatio = maintain) }
    }

    fun updateDraftScaleMode(mode: ScaleMode) {
        _activeDraft.update { it.copy(scaleMode = mode) }
    }

    fun updateDraftScaleFactor(scale: Float) {
        _activeDraft.update { it.copy(scaleFactor = scale.coerceIn(0.4f, 2.5f)) }
    }

    fun updateDraftOffset(offsetX: Float, offsetY: Float) {
        _activeDraft.update {
            it.copy(
                offsetX = offsetX.coerceIn(-1f, 1f),
                offsetY = offsetY.coerceIn(-1f, 1f)
            )
        }
    }

    fun updateDraftOpacity(opacity: Float) {
        _activeDraft.update { it.copy(opacity = opacity.coerceIn(0.1f, 1.0f)) }
    }

    fun updateDraftBackgroundMode(mode: BackgroundMode) {
        _activeDraft.update {
            val preserveAlpha = mode == BackgroundMode.TRANSPARENT || mode == BackgroundMode.NONE
            it.copy(
                backgroundMode = mode,
                preserveTransparency = if (mode == BackgroundMode.TRANSPARENT || mode == BackgroundMode.NONE) true else it.preserveTransparency
            )
        }
    }

    fun updateDraftBackgroundColor(colorHex: Long) {
        _activeDraft.update { it.copy(backgroundColorHex = colorHex) }
    }

    fun updateDraftCornerRadius(radiusDp: Int) {
        _activeDraft.update { it.copy(cornerRadiusDp = radiusDp.coerceIn(0, 80)) }
    }

    fun updateDraftPreserveTransparency(preserve: Boolean) {
        _activeDraft.update {
            it.copy(
                preserveTransparency = preserve,
                backgroundMode = if (preserve) BackgroundMode.TRANSPARENT else BackgroundMode.SOLID_COLOR
            )
        }
    }

    fun updateDraftAnimationEnabled(enabled: Boolean) {
        _activeDraft.update { it.copy(isAnimationEnabled = enabled) }
    }

    fun updateDraftAnimationSpeed(multiplier: Float) {
        _activeDraft.update { it.copy(animationSpeedMultiplier = multiplier.coerceIn(0.25f, 2.5f)) }
    }

    fun updateDraftLoopMode(loopMode: LoopMode) {
        _activeDraft.update { it.copy(loopMode = loopMode) }
    }

    fun updateDraftSelectedFrame(frameIndex: Int) {
        _activeDraft.update {
            it.copy(selectedFrameIndex = frameIndex.coerceIn(0, (it.totalFrames - 1).coerceAtLeast(0)))
        }
    }

    fun resetDraftConfiguration() {
        val currentMediaId = _activeDraft.value.mediaId
        val media = mediaItems.value.find { it.id == currentMediaId } ?: mediaItems.value.firstOrNull()
        val resetDraft = createEmptyDraft().copy(widgetId = _activeDraft.value.widgetId)
        _activeDraft.value = resetDraft
        if (media != null) {
            applyMediaToDraft(media)
        }
        _statusMessage.value = "Configuration reset to studio defaults"
    }

    fun saveDraftWidget(requestHomePin: Boolean = false) {
        viewModelScope.launch {
            val current = _activeDraft.value
            if (current.localFilePath.isBlank()) {
                _statusMessage.value = "Please select an image, GIF, or SVG first."
                return@launch
            }
            val saved = if (current.widgetId == 0) {
                createWidgetUseCase(current)
            } else {
                updateWidgetUseCase(current)
                current
            }
            _activeDraft.value = saved

            if (requestHomePin) {
                val pinRequested = WidgetUpdater.requestPinWidgetToHomeScreen(getApplication(), saved)
                _statusMessage.value = if (pinRequested) {
                    "Widget saved! Confirm placement in your Android launcher prompt."
                } else {
                    "Widget saved! Long-press your home screen -> Widgets -> KAPS to place it."
                }
            } else {
                _statusMessage.value = "Saved '${saved.title}' to My Widgets."
            }
            _currentSection.value = StudioSection.MY_WIDGETS
        }
    }

    fun requestPinExistingWidget(config: WidgetConfigEntity) {
        val ok = WidgetUpdater.requestPinWidgetToHomeScreen(getApplication(), config)
        _statusMessage.value = if (ok) {
            "Confirm home-screen placement in the system dialog."
        } else {
            "Long-press your Android home screen -> Widgets -> KAPS and select '${config.title}'."
        }
    }

    fun deleteMediaItem(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.deleteMediaItem(item)
            refreshStorageStats()
            _statusMessage.value = "Removed '${item.displayName}' from library"
        }
    }

    fun verifyMediaSourcesNow() {
        viewModelScope.launch {
            repository.verifyAllSources()
            _statusMessage.value = "Verified all media file paths and SAF permissions"
        }
    }

    fun restoreStarterAssets() {
        viewModelScope.launch {
            repository.restoreStarterSamples()
            refreshStorageStats()
            _statusMessage.value = "Restored built-in PNG, GIF, and SVG starter samples"
        }
    }

    fun setThemeMode(mode: StudioThemeMode) {
        viewModelScope.launch {
            repository.preferences.setThemeMode(mode)
        }
    }

    fun setCheckerboardHighContrast(highContrast: Boolean) {
        viewModelScope.launch {
            repository.preferences.setCheckerboardHighContrast(highContrast)
        }
    }

    fun setMaxAnimationFps(fps: Int) {
        viewModelScope.launch {
            repository.preferences.setMaxAnimationFps(fps)
            WidgetUpdater.updateAllActiveWidgets(getApplication(), repository)
        }
    }

    fun setPauseOnBatterySaver(pause: Boolean) {
        viewModelScope.launch {
            repository.preferences.setPauseOnBatterySaver(pause)
            WidgetUpdater.updateAllActiveWidgets(getApplication(), repository)
        }
    }

    fun setMaxWidgetResolutionPx(maxPx: Int) {
        viewModelScope.launch {
            repository.preferences.setMaxWidgetResolutionPx(maxPx)
            WidgetUpdater.updateAllActiveWidgets(getApplication(), repository)
        }
    }

    fun setShowPlacementGuide(show: Boolean) {
        viewModelScope.launch {
            repository.preferences.setShowPlacementGuide(show)
        }
    }

    fun purgeRenderCache() {
        viewModelScope.launch {
            repository.purgeCaches()
            refreshStorageStats()
            _statusMessage.value = "Cleared bitmap & frame caches"
        }
    }

    private fun refreshStorageStats() {
        _storageBytesUsed.value = repository.getStorageBytesUsed()
    }

    private fun createEmptyDraft(): WidgetConfigEntity {
        return WidgetConfigEntity(
            widgetId = 0,
            title = "Untitled Widget",
            mediaId = 0L,
            sourceUri = "",
            localFilePath = "",
            mediaFormat = MediaFormat.PNG,
            sizePreset = SizePreset.MEDIUM,
            targetWidthDp = 220,
            targetHeightDp = 140,
            maintainAspectRatio = true,
            scaleMode = ScaleMode.FIT_INSIDE,
            scaleFactor = 1.0f,
            offsetX = 0.0f,
            offsetY = 0.0f,
            opacity = 1.0f,
            backgroundMode = BackgroundMode.TRANSPARENT,
            backgroundColorHex = 0xFFFFFFFF,
            cornerRadiusDp = 22,
            preserveTransparency = true,
            isAnimationEnabled = true,
            animationSpeedMultiplier = 1.0f,
            loopMode = LoopMode.CONTINUOUS,
            selectedFrameIndex = 0,
            totalFrames = 1
        )
    }
}
