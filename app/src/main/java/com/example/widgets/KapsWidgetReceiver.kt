package com.example.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.KapsApplication
import kotlinx.coroutines.launch

/**
 * Restores all active home-screen widgets after device reboot, app update,
 * or Power Save / Doze mode transitions.
 */
class KapsWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
        ) {
            val app = context.applicationContext as? KapsApplication ?: return
            val pendingResult = goAsync()
            app.applicationScope.launch {
                try {
                    app.repository.ensureStarterLibrarySeeded()
                    app.repository.verifyAllSources()
                    WidgetUpdater.updateAllActiveWidgets(context, app.repository)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
