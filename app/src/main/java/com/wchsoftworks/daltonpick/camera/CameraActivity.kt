package com.wchsoftworks.daltonpick.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.wchsoftworks.daltonpick.settings.SettingsRepository
import com.wchsoftworks.daltonpick.ui.DaltonTheme
import kotlinx.coroutines.launch

/** Camera mode (spec §4.4): the same crosshair and label, on the live camera instead of the screen. */
class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            // Counts real opens (not rotations), for the lighting hint.
            lifecycleScope.launch { SettingsRepository.get(this@CameraActivity).update { it.copy(cameraOpens = it.cameraOpens + 1) } }
        }
        setContent {
            DaltonTheme {
                CameraScreen(onClose = ::finish)
            }
        }
    }
}
