package com.wchsoftworks.daltonpick.overlay

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import android.widget.Toast
import com.wchsoftworks.daltonpick.OverlayNotification
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.ToggleTileService
import com.wchsoftworks.daltonpick.capture.ScreenCapturer
import com.wchsoftworks.daltonpick.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
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
    /** This runtime ended on its own (replaced by another host, or its windows were refused): stop the host. */
    private val onEnded: () -> Unit = {},
) {
    private val repo = SettingsRepository.get(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stopped = false
    private val controller = OverlayController(
        context,
        windowType,
        capturer,
        persist = { transform -> scope.launch { repo.update(transform) } },
        onWindowsLost = {
            Toast.makeText(context, R.string.overlay_lost, Toast.LENGTH_LONG).show()
            stop()
            onEnded()
        },
    )

    fun start() {
        // Only one host at a time: turning on the accessibility service retires the fallback.
        active?.let {
            it.stop()
            it.onEnded()
        }
        active = this
        runningHost.value = capturer.name
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
        if (stopped) return
        stopped = true
        if (active === this) {
            active = null
            runningHost.value = null
        }
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

        /** The running host's capture backend ([ScreenCapturer.name]), or null; the main app watches this. */
        val runningHost = MutableStateFlow<String?>(null)

        fun refreshTile(context: Context) {
            TileService.requestListeningState(context, ComponentName(context, ToggleTileService::class.java))
        }
    }
}
