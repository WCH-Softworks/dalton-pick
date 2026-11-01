package com.wchsoftworks.daltonpick

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.wchsoftworks.daltonpick.overlay.OverlayRuntime
import com.wchsoftworks.daltonpick.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Quick Settings tile that shows or hides the edge tab (spec §4.1). */
class ToggleTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        scope.launch {
            val visible = SettingsRepository.get(applicationContext).flow.first().tabVisible
            val tile = qsTile ?: return@launch
            val running = OverlayRuntime.active != null
            tile.state = if (running && visible) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.subtitle = getString(
                when {
                    !running -> R.string.tile_needs_setup
                    visible -> R.string.tile_shown
                    else -> R.string.tile_hidden
                },
            )
            tile.updateTile()
        }
    }

    override fun onClick() {
        if (OverlayRuntime.active == null) {
            openApp()
            return
        }
        scope.launch {
            SettingsRepository.get(applicationContext).update { it.copy(tabVisible = !it.tabVisible) }
            onStartListening()
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
