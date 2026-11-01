package com.wchsoftworks.daltonpick.overlay

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.wchsoftworks.daltonpick.OverlayNotification
import com.wchsoftworks.daltonpick.ToggleTileService
import com.wchsoftworks.daltonpick.capture.ScreenCapturer
import com.wchsoftworks.daltonpick.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * What a host (the accessibility service, or the MediaProjection service) runs: an
 * [OverlayController] driven by the settings flow, plus the persistent notification and the
 * Quick Settings tile state. Hosts differ only in [windowType] and [capturer].
 */
class OverlayRuntime(
    private val context: Context,
    windowType: Int,
    private val capturer: ScreenCapturer,
    private val withStopAction: Boolean,
) {
    private val repo = SettingsRepository.get(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller = OverlayController(context, windowType, capturer) { transform ->
        scope.launch { repo.update(transform) }
    }

    fun start() {
        active?.stop()
        active = this
        scope.launch {
            repo.flow.collect { settings ->
                controller.apply(settings)
                context.getSystemService(NotificationManager::class.java)
                    .notify(OverlayNotification.ID, OverlayNotification.build(context, settings.tabVisible, withStopAction))
                refreshTile(context)
            }
        }
    }

    fun onScreenChanged() = controller.onScreenChanged()

    fun stop() {
        if (active === this) active = null
        scope.cancel()
        controller.destroy()
        capturer.release()
        context.getSystemService(NotificationManager::class.java).cancel(OverlayNotification.ID)
        refreshTile(context)
    }

    companion object {
        /** The running host, or null when neither backend is up. The tile and main screen read this. */
        @Volatile
        var active: OverlayRuntime? = null
            private set

        fun refreshTile(context: Context) {
            TileService.requestListeningState(context, ComponentName(context, ToggleTileService::class.java))
        }
    }
}
