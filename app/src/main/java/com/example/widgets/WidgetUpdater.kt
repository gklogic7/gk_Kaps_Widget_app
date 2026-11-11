package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.core.WidgetRepository
import com.example.data.WidgetConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {

    suspend fun updateSingleWidget(
        context: Context,
        appWidgetId: Int,
        repository: WidgetRepository
    ) = withContext(Dispatchers.IO) {
        if (appWidgetId <= 0) return@withContext
        val manager = AppWidgetManager.getInstance(context)
        val config = repository.getWidgetById(appWidgetId)
            ?: repository.getAllWidgetsSnapshot().firstOrNull()
            ?: return@withContext

        val prefs = repository.preferences.getPreferencesSnapshot()
        val options = runCatching { manager.getAppWidgetOptions(appWidgetId) }.getOrNull()
        val remoteViews = WidgetRenderer.buildRemoteViews(context, config, options, prefs)
        manager.updateAppWidget(appWidgetId, remoteViews)
    }

    suspend fun updateAllActiveWidgets(
        context: Context,
        repository: WidgetRepository
    ) = withContext(Dispatchers.IO) {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, KapsWidgetProvider::class.java)
        val activeIds = runCatching { manager.getAppWidgetIds(provider) }.getOrDefault(IntArray(0))
        val prefs = repository.preferences.getPreferencesSnapshot()
        val allSaved = repository.getAllWidgetsSnapshot()

        for (id in activeIds) {
            val config = repository.getWidgetById(id) ?: allSaved.firstOrNull() ?: continue
            val options = runCatching { manager.getAppWidgetOptions(id) }.getOrNull()
            val views = WidgetRenderer.buildRemoteViews(context, config, options, prefs)
            manager.updateAppWidget(id, views)
        }
    }

    /**
     * Requests the Android launcher to pin the widget directly onto the home screen
     * if supported by the launcher (Android 8.0+ requestPinAppWidget).
     */
    fun requestPinWidgetToHomeScreen(
        context: Context,
        config: WidgetConfigEntity
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false

        val provider = ComponentName(context, KapsWidgetProvider::class.java)
        val callbackIntent = Intent(context, KapsWidgetProvider::class.java).apply {
            action = KapsWidgetProvider.ACTION_REFRESH_WIDGET
            putExtra(KapsWidgetProvider.EXTRA_PRESET_WIDGET_ID, config.widgetId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val successCallback = PendingIntent.getBroadcast(
            context,
            config.widgetId.hashCode(),
            callbackIntent,
            flags
        )
        return runCatching {
            manager.requestPinAppWidget(provider, null, successCallback)
        }.getOrDefault(false)
    }
}
