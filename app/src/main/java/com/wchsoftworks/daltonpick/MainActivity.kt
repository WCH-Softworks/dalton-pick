package com.wchsoftworks.daltonpick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.wchsoftworks.daltonpick.ui.DaltonApp
import com.wchsoftworks.daltonpick.ui.DaltonTheme
import com.wchsoftworks.daltonpick.ui.Screen

/** The main app (spec §4.5). The lens itself lives in the overlay services, not here. */
class MainActivity : ComponentActivity() {
    /** A screen asked for from outside, e.g. the notification's Settings button. */
    private val requested = mutableStateOf<Screen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) requested.value = screenFrom(intent)
        setContent {
            DaltonTheme {
                DaltonApp(requested = requested.value, onRequestHandled = { requested.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requested.value = screenFrom(intent)
    }

    private fun screenFrom(intent: Intent?): Screen? =
        intent?.getStringExtra(EXTRA_SCREEN)?.let { name -> Screen.entries.firstOrNull { it.name == name } }

    companion object {
        private const val EXTRA_SCREEN = "screen"

        fun intent(context: Context, screen: Screen? = null): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .apply { if (screen != null) putExtra(EXTRA_SCREEN, screen.name) }
    }
}
