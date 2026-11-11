package com.example.domain

import android.content.Context
import android.net.Uri
import com.example.core.WidgetRepository
import com.example.data.MediaItemEntity
import com.example.data.SizePreset
import com.example.data.WidgetConfigEntity
import com.example.widgets.WidgetUpdater

class CreateWidgetUseCase(
    private val repository: WidgetRepository,
    private val context: Context
) {
    suspend operator fun invoke(config: WidgetConfigEntity): WidgetConfigEntity {
        val assignedId = if (config.widgetId == 0) {
            repository.allocateStudioPresetId()
        } else {
            config.widgetId
        }
        val finalConfig = config.copy(
            widgetId = assignedId,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveWidgetConfig(finalConfig)
        if (finalConfig.widgetId > 0) {
            WidgetUpdater.updateSingleWidget(context, finalConfig.widgetId, repository)
        }
        return finalConfig
    }
}

class UpdateWidgetUseCase(
    private val repository: WidgetRepository,
    private val context: Context
) {
    suspend operator fun invoke(config: WidgetConfigEntity) {
        repository.saveWidgetConfig(config)
        if (config.widgetId > 0) {
            WidgetUpdater.updateSingleWidget(context, config.widgetId, repository)
        }
    }
}

class DeleteWidgetUseCase(
    private val repository: WidgetRepository
) {
    suspend operator fun invoke(widgetId: Int) {
        repository.deleteWidget(widgetId)
    }
}

class ResizeWidgetUseCase(
    private val repository: WidgetRepository,
    private val context: Context
) {
    suspend operator fun invoke(widgetId: Int, newWidthDp: Int, newHeightDp: Int) {
        val existing = repository.getWidgetById(widgetId) ?: return
        val updated = existing.copy(
            sizePreset = SizePreset.CUSTOM,
            targetWidthDp = newWidthDp.coerceIn(48, 900),
            targetHeightDp = newHeightDp.coerceIn(48, 900),
            updatedAt = System.currentTimeMillis()
        )
        repository.saveWidgetConfig(updated)
        if (widgetId > 0) {
            WidgetUpdater.updateSingleWidget(context, widgetId, repository)
        }
    }
}

class ImportMediaUseCase(
    private val repository: WidgetRepository
) {
    suspend operator fun invoke(uri: Uri): MediaItemEntity? {
        return repository.importMediaFromUri(uri)
    }
}
