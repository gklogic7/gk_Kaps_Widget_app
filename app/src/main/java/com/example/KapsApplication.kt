package com.example

import android.app.Application
import com.example.core.FileAccess
import com.example.core.WidgetRepository
import com.example.data.MediaStorage
import com.example.data.WidgetDatabase
import com.example.data.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KapsApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: WidgetDatabase by lazy { WidgetDatabase.getInstance(this) }
    val preferences: WidgetPreferences by lazy { WidgetPreferences(this) }
    val mediaStorage: MediaStorage by lazy { MediaStorage(this) }
    val fileAccess: FileAccess by lazy { FileAccess(this, mediaStorage) }

    val repository: WidgetRepository by lazy {
        WidgetRepository(
            widgetDao = database.widgetDao(),
            mediaDao = database.mediaDao(),
            preferences = preferences,
            fileAccess = fileAccess,
            mediaStorage = mediaStorage
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.ensureStarterLibrarySeeded()
        }
    }
}
