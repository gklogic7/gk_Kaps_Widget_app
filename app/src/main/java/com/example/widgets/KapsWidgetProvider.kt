package com.example.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.KapsApplication
import com.example.data.SizePreset
import com.example.utils.DimensionUtils
import kotlinx.coroutines.launch

class KapsWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widgets.ACTION_REFRESH_WIDGET"
        const val ACTION_STEP_FRAME = "com.example.widgets.ACTION_STEP_FRAME"
        const val EXTRA_PRESET_WIDGET_ID = "extra_preset_widget_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val app = context.applicationContext as? KapsApplication ?: return
        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                val presetId = intent.getIntExtra(EXTRA_PRESET_WIDGET_ID, 0)
                val pinnedWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val pendingResult = goAsync()
                app.applicationScope.launch {
                    try {
                        if (presetId != 0 && pinnedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            val preset = app.repository.getWidgetById(presetId)
                            if (preset != null) {
                                app.repository.bindPresetToAppWidgetId(preset, pinnedWidgetId)
                            }
                        }
                        WidgetUpdater.updateAllActiveWidgets(context, app.repository)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as? KapsApplication ?: return
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                app.repository.ensureStarterLibrarySeeded()
                for (widgetId in appWidgetIds) {
                    val existing = app.repository.getWidgetById(widgetId)
                    if (existing == null) {
                        val fallback = app.repository.getAllWidgetsSnapshot().firstOrNull()
                        if (fallback != null) {
                            app.repository.saveWidgetConfig(
                                fallback.copy(
                                    widgetId = widgetId,
                                    isPlacedOnHomeScreen = true
                                )
                            )
                        }
                    }
                    WidgetUpdater.updateSingleWidget(context, widgetId, app.repository)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Triggered whenever the user resizes the widget on their Android tablet or phone home screen.
     * Re-renders the SVG or image at the new grid dimensions immediately.
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val app = context.applicationContext as? KapsApplication ?: return
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                val existing = app.repository.getWidgetById(appWidgetId)
                if (existing != null) {
                    val (newW, newH) = DimensionUtils.resolveWidgetDimensionsDp(
                        newOptions,
                        existing.targetWidthDp,
                        existing.targetHeightDp
                    )
                    app.repository.saveWidgetConfig(
                        existing.copy(
                            sizePreset = SizePreset.CUSTOM,
                            targetWidthDp = newW,
                            targetHeightDp = newH,
                            isPlacedOnHomeScreen = true
                        )
                    )
                }
                WidgetUpdater.updateSingleWidget(context, appWidgetId, app.repository)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext as? KapsApplication ?: return
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                for (id in appWidgetIds) {
                    app.repository.deleteWidget(id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
