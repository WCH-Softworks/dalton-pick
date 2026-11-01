package com.wchsoftworks.daltonpick.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wchsoftworks.daltonpick.AppStatus
import com.wchsoftworks.daltonpick.ProjectionOverlayService
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.overlay.OverlayRuntime
import com.wchsoftworks.daltonpick.settings.Settings
import com.wchsoftworks.daltonpick.settings.SettingsRepository
import kotlinx.coroutines.launch

enum class Screen { HOME, SETTINGS, TEST_CHART, ABOUT, LICENSES }

/** Everything a screen may read or do, so the screens themselves stay plain composables. */
class AppState(
    val settings: Settings,
    val status: AppStatus,
    /** The running capture backend ("A11y", "Projection") or null when the lens is off. */
    val host: String?,
    val update: ((Settings) -> Settings) -> Unit,
    val requestNotifications: () -> Unit,
    val startFallback: () -> Unit,
    val stopFallback: () -> Unit,
    val secureTest: Boolean,
    val setSecureTest: (Boolean) -> Unit,
) {
    val fallbackRunning get() = host != null && host != "A11y"
}

/**
 * Root of the main app (spec §4.5): first-run onboarding, then a home screen with a small
 * back stack of sub-screens.
 */
@Composable
fun DaltonApp(requested: Screen?, onRequestHandled: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.flow.collectAsStateWithLifecycle(initialValue = null)
    val host by OverlayRuntime.runningHost.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf(AppStatus.read(context)) }
    var stack by rememberSaveable { mutableStateOf(listOf(Screen.HOME)) }
    var secureTest by rememberSaveable { mutableStateOf(false) }

    // Permissions can change in system settings while we're away.
    LifecycleResumeEffect(Unit) {
        status = AppStatus.read(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(requested) {
        if (requested != null) {
            stack = listOf(Screen.HOME, requested)
            onRequestHandled()
        }
    }

    // Developer switch: make this screen behave like a banking app, to test "Content hidden".
    DisposableEffect(secureTest) {
        val window = (context as? Activity)?.window
        if (secureTest) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        status = AppStatus.read(context)
    }
    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            ProjectionOverlayService.start(context, result.resultCode, data)
        } else {
            Toast.makeText(context, R.string.projection_denied, Toast.LENGTH_LONG).show()
        }
    }

    val current = settings ?: return // DataStore answers within a frame; draw nothing until then.
    val state = AppState(
        settings = current,
        status = status,
        host = host,
        update = { transform -> scope.launch { repo.update(transform) } },
        requestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        startFallback = {
            when {
                status.accessibilityOn -> Toast.makeText(context, R.string.accessibility_active, Toast.LENGTH_LONG).show()
                !status.overlayAllowed -> SystemActions.openOverlayPermission(context)
                else -> projectionLauncher.launch(SystemActions.screenCaptureIntent(context))
            }
        },
        stopFallback = { ProjectionOverlayService.stop(context) },
        secureTest = secureTest,
        setSecureTest = { secureTest = it },
    )

    if (!current.onboardingDone) {
        OnboardingScreen(state)
        return
    }

    val open: (Screen) -> Unit = { stack = stack + it }
    val back: () -> Unit = { stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1, onBack = back)

    when (stack.last()) {
        Screen.HOME -> HomeScreen(state, open)
        Screen.SETTINGS -> SettingsScreen(state, back)
        Screen.TEST_CHART -> TestChartScreen(back)
        Screen.ABOUT -> AboutScreen(back, open)
        Screen.LICENSES -> LicensesScreen(back)
    }
}
