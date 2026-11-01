package com.wchsoftworks.daltonpick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wchsoftworks.daltonpick.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the notification's Hide/Show button. The running host reacts to the setting change. */
class OverlayActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_TAB) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                SettingsRepository.get(context).update { it.copy(tabVisible = !it.tabVisible) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TAB = "com.wchsoftworks.daltonpick.TOGGLE_TAB"
    }
}
