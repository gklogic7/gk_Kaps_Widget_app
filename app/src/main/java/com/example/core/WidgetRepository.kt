package com.example.core

import android.net.Uri
import com.example.data.BackgroundMode
import com.example.data.LoopMode
import com.example.data.MediaDao
import com.example.data.MediaFormat
import com.example.data.MediaItemEntity
import com.example.data.MediaStorage
import com.example.data.ScaleMode
import com.example.data.SizePreset
import com.example.data.StudioPreferences
import com.example.data.WidgetConfigEntity
import com.example.data.WidgetDao
import com.example.data.WidgetPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class WidgetRepository(
    private val widgetDao: WidgetDao,
    private val mediaDao: MediaDao,
    val preferences: WidgetPreferences,
    private val fileAccess: FileAccess,
    private val mediaStorage: MediaStorage
) {

    val allWidgetsFlow: Flow<List<WidgetConfigEntity>> = widgetDao.observeAllWidgets()
    val allMediaFlow: Flow<List<MediaItemEntity>> = mediaDao.observeAllMedia()
    val preferencesFlow: Flow<StudioPreferences> = preferences.preferencesFlow

    /**
     * Seeds the 4 built-in starter media items (Transparent PNG, Animated GIF, Static SVG, Animated SVG)
     * and 2 initial studio widget presets on first launch so the user can test immediately.
     */
    suspend fun ensureStarterLibrarySeeded() = withContext(Dispatchers.IO) {
        if (preferences.isStarterSeeded() && mediaDao.getAllMediaSnapshot().isNotEmpty()) {
            return@withContext
        }

        val starterItems = mediaStorage.generateStarterSampleFiles()
        val insertedMedia = mutableListOf<MediaItemEntity>()
        for (item in starterItems) {
            val id = mediaDao.insertMedia(item)
            insertedMedia.add(item.copy(id = id))
        }

        if (widgetDao.getAllWidgetsSnapshot().isEmpty() && insertedMedia.size >= 4) {
            val pngItem = insertedMedia[0]
            val svgAnimItem = insertedMedia[3]

            // Studio preset 1: Transparent PNG Bot Sticker
            widgetDao.upsertWidget(
                WidgetConfigEntity(
                    widgetId = -101,
                    title = "KAPS Bot Sticker",
                    mediaId = pngItem.id,
                    sourceUri = pngItem.originalUri,
                    localFilePath = pngItem.localFilePath,
                    mediaFormat = pngItem.mediaFormat,
                    sizePreset = SizePreset.SMALL,
                    targetWidthDp = 140,
                    targetHeightDp = 140,
                    maintainAspectRatio = true,
                    scaleMode = ScaleMode.FIT_INSIDE,
                    backgroundMode = BackgroundMode.TRANSPARENT,
                    preserveTransparency = true,
                    totalFrames = 1
                )
            )

            // Studio preset 2: Animated SVG Kinetic Radar
            widgetDao.upsertWidget(
                WidgetConfigEntity(
                    widgetId = -102,
                    title = "Kinetic Radar Chrono",
                    mediaId = svgAnimItem.id,
                    sourceUri = svgAnimItem.originalUri,
                    localFilePath = svgAnimItem.localFilePath,
                    mediaFormat = svgAnimItem.mediaFormat,
                    sizePreset = SizePreset.MEDIUM,
                    targetWidthDp = 200,
                    targetHeightDp = 200,
                    maintainAspectRatio = true,
                    scaleMode = ScaleMode.FIT_INSIDE,
                    backgroundMode = BackgroundMode.TRANSPARENT,
                    preserveTransparency = true,
                    isAnimationEnabled = true,
                    animationSpeedMultiplier = 1.0f,
                    loopMode = LoopMode.CONTINUOUS,
                    totalFrames = 8,
                    svgCompatibilityNotes = svgAnimItem.svgCompatibilityNotes
                )
            )
        }

        preferences.markStarterSeeded()
    }

    suspend fun importMediaFromUri(uri: Uri): MediaItemEntity? = withContext(Dispatchers.IO) {
        val entity = fileAccess.importFromSafUri(uri) ?: return@withContext null
        val id = mediaDao.insertMedia(entity)
        entity.copy(id = id)
    }

    suspend fun verifyAllSources() = withContext(Dispatchers.IO) {
        val allMedia = mediaDao.getAllMediaSnapshot()
        for (item in allMedia) {
            val accessible = fileAccess.verifyMediaAccessibility(item)
            if (accessible != item.isSourceAccessible) {
                mediaDao.updateMedia(item.copy(isSourceAccessible = accessible))
            }
        }
        val allWidgets = widgetDao.getAllWidgetsSnapshot()
        for (w in allWidgets) {
            val missing = !File(w.localFilePath).exists()
            if (missing != w.sourceMissing) {
                widgetDao.upsertWidget(w.copy(sourceMissing = missing))
            }
        }
    }

    suspend fun getWidgetById(widgetId: Int): WidgetConfigEntity? = withContext(Dispatchers.IO) {
        widgetDao.getWidgetById(widgetId)
    }

    suspend fun getAllWidgetsSnapshot(): List<WidgetConfigEntity> = withContext(Dispatchers.IO) {
        widgetDao.getAllWidgetsSnapshot()
    }

    suspend fun getMediaById(id: Long): MediaItemEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    suspend fun allocateStudioPresetId(): Int = withContext(Dispatchers.IO) {
        val minId = widgetDao.getMinWidgetId() ?: -100
        if (minId >= 0) -101 else (minId - 1)
    }

    suspend fun saveWidgetConfig(config: WidgetConfigEntity) = withContext(Dispatchers.IO) {
        widgetDao.upsertWidget(config.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun bindPresetToAppWidgetId(presetConfig: WidgetConfigEntity, appWidgetId: Int): WidgetConfigEntity =
        withContext(Dispatchers.IO) {
            val bound = presetConfig.copy(
                widgetId = appWidgetId,
                isPlacedOnHomeScreen = true,
                updatedAt = System.currentTimeMillis()
            )
            widgetDao.upsertWidget(bound)
            // If it was a temporary negative studio preset, optionally keep or remove;
            // removing the negative draft avoids duplicate entries once placed on home screen.
            if (presetConfig.widgetId < 0) {
                widgetDao.deleteWidgetById(presetConfig.widgetId)
            }
            bound
        }

    suspend fun deleteWidget(widgetId: Int) = withContext(Dispatchers.IO) {
        widgetDao.deleteWidgetById(widgetId)
    }

    suspend fun deleteMediaItem(item: MediaItemEntity) = withContext(Dispatchers.IO) {
        if (!item.isBuiltInSample) {
            runCatching { File(item.localFilePath).delete() }
        }
        mediaDao.deleteMediaById(item.id)
    }

    suspend fun restoreStarterSamples() = withContext(Dispatchers.IO) {
        val existing = mediaDao.getAllMediaSnapshot().filter { it.isBuiltInSample }
        existing.forEach { mediaDao.deleteMediaById(it.id) }
        val starterItems = mediaStorage.generateStarterSampleFiles()
        starterItems.forEach { mediaDao.insertMedia(it) }
    }

    fun getStorageBytesUsed(): Long = mediaStorage.getCacheSizeBytes() + ImageCache.currentSizeBytes()

    suspend fun purgeCaches() = withContext(Dispatchers.IO) {
        ImageCache.evictAll()
        mediaStorage.clearFrameCache()
    }
}
