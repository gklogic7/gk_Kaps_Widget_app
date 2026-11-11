package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.kapsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kaps_studio_preferences")

class WidgetPreferences(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_CHECKERBOARD_HIGH_CONTRAST = booleanPreferencesKey("checkerboard_high_contrast")
        private val KEY_MAX_ANIMATION_FPS = intPreferencesKey("max_animation_fps")
        private val KEY_PAUSE_ON_BATTERY_SAVER = booleanPreferencesKey("pause_on_battery_saver")
        private val KEY_MAX_WIDGET_RESOLUTION_PX = intPreferencesKey("max_widget_resolution_px")
        private val KEY_SHOW_PLACEMENT_GUIDE = booleanPreferencesKey("show_placement_guide")
        private val KEY_STARTER_SEEDED = booleanPreferencesKey("starter_seeded_v1")
    }

    val preferencesFlow: Flow<StudioPreferences> = context.kapsDataStore.data.map { prefs ->
        val themeStr = prefs[KEY_THEME_MODE] ?: StudioThemeMode.LIGHT_IPADOS.name
        val themeMode = runCatching { StudioThemeMode.valueOf(themeStr) }
            .getOrDefault(StudioThemeMode.LIGHT_IPADOS)
        StudioPreferences(
            themeMode = themeMode,
            checkerboardHighContrast = prefs[KEY_CHECKERBOARD_HIGH_CONTRAST] ?: true,
            maxAnimationFps = prefs[KEY_MAX_ANIMATION_FPS] ?: 10,
            pauseOnBatterySaver = prefs[KEY_PAUSE_ON_BATTERY_SAVER] ?: true,
            maxWidgetResolutionPx = prefs[KEY_MAX_WIDGET_RESOLUTION_PX] ?: 720,
            showPlacementGuide = prefs[KEY_SHOW_PLACEMENT_GUIDE] ?: true
        )
    }

    suspend fun getPreferencesSnapshot(): StudioPreferences = preferencesFlow.first()

    suspend fun isStarterSeeded(): Boolean {
        return context.kapsDataStore.data.first()[KEY_STARTER_SEEDED] ?: false
    }

    suspend fun markStarterSeeded() {
        context.kapsDataStore.edit { it[KEY_STARTER_SEEDED] = true }
    }

    suspend fun setThemeMode(mode: StudioThemeMode) {
        context.kapsDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setCheckerboardHighContrast(highContrast: Boolean) {
        context.kapsDataStore.edit { it[KEY_CHECKERBOARD_HIGH_CONTRAST] = highContrast }
    }

    suspend fun setMaxAnimationFps(fps: Int) {
        context.kapsDataStore.edit { it[KEY_MAX_ANIMATION_FPS] = fps.coerceIn(2, 20) }
    }

    suspend fun setPauseOnBatterySaver(pause: Boolean) {
        context.kapsDataStore.edit { it[KEY_PAUSE_ON_BATTERY_SAVER] = pause }
    }

    suspend fun setMaxWidgetResolutionPx(maxPx: Int) {
        context.kapsDataStore.edit { it[KEY_MAX_WIDGET_RESOLUTION_PX] = maxPx.coerceIn(360, 1024) }
    }

    suspend fun setShowPlacementGuide(show: Boolean) {
        context.kapsDataStore.edit { it[KEY_SHOW_PLACEMENT_GUIDE] = show }
    }
}
