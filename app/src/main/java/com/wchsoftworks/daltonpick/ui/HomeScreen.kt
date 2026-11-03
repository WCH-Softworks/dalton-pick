package com.wchsoftworks.daltonpick.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.camera.CameraActivity

/** Whether the lens works right now, what's missing, and the way into everything else. */
@Composable
fun HomeScreen(state: AppState, open: (Screen) -> Unit) {
    val context = LocalContext.current
    val status = state.status
    ScreenScaffold(title = null, onBack = null) {
        Header()
        LensCard(state)

        // Only what still needs doing; once everything is on, the lens card says so.
        val askNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.notificationsAllowed
        if (!status.accessibilityOn || askNotifications) SectionTitle(stringResource(R.string.home_setup))
        if (!status.accessibilityOn) {
            StatusRow(
                title = stringResource(R.string.req_accessibility),
                icon = R.drawable.ic_lens,
                ok = false,
                explanation = stringResource(R.string.req_accessibility_why),
                actionLabel = stringResource(R.string.btn_turn_on),
                onAction = { SystemActions.openAccessibility(context) },
            )
            RestrictedSettingsHelp()
        }
        if (askNotifications) {
            // Optional: the Quick Settings tile does the same job.
            StatusRow(
                title = stringResource(R.string.req_notifications),
                icon = R.drawable.ic_notifications,
                ok = false,
                optional = true,
                explanation = stringResource(R.string.req_notifications_why),
                actionLabel = stringResource(R.string.btn_allow),
                onAction = state.requestNotifications,
            )
        }

        SectionTitle(stringResource(R.string.home_more))
        Group {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                LinkRow(stringResource(R.string.camera_title), stringResource(R.string.camera_subtitle), R.drawable.ic_camera) {
                    context.startActivity(Intent(context, CameraActivity::class.java))
                }
            }
            LinkRow(stringResource(R.string.test_chart_title), stringResource(R.string.test_chart_subtitle), R.drawable.ic_palette) {
                open(Screen.TEST_CHART)
            }
            LinkRow(stringResource(R.string.settings_title), icon = R.drawable.ic_settings) { open(Screen.SETTINGS) }
            LinkRow(stringResource(R.string.about_title), icon = R.drawable.ic_info) { open(Screen.ABOUT) }
        }

        // Only useful while the accessibility service can't be used.
        if (!status.accessibilityOn || state.fallbackRunning) FallbackSection(state)
    }
}

/** The app's mark: the launcher icon's lens on its color wheel. */
@Composable
fun AppMark(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.sweepGradient(WHEEL)),
        contentAlignment = Alignment.Center,
    ) {
        // The foreground is drawn for a 108 dp adaptive icon with a 66 dp safe zone.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(size * 108 / 66))
    }
}

// Same colors as ic_launcher_background.
private val WHEEL = listOf(0xFFE57373, 0xFFFFB74D, 0xFFFFF176, 0xFF81C784, 0xFF4FC3F7, 0xFFBA68C8, 0xFFE57373).map { Color(it) }

@Composable
private fun Header() {
    Row(
        Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark(56.dp)
        Column(Modifier.padding(start = 16.dp)) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Hint(stringResource(R.string.onb_welcome_title))
        }
    }
}

@Composable
private fun LensCard(state: AppState) {
    val ready = state.host != null
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) colors.primaryContainer else colors.surfaceBright,
            contentColor = if (ready) colors.onPrimaryContainer else colors.onSurface,
        ),
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    R.drawable.ic_lens,
                    container = if (ready) colors.primary else colors.surfaceContainerHighest,
                    tint = if (ready) colors.onPrimary else colors.onSurfaceVariant,
                )
                val headline = when {
                    state.fallbackRunning -> R.string.lens_on_fallback
                    ready -> R.string.lens_on
                    state.status.accessibilityOn -> R.string.lens_starting
                    else -> R.string.lens_off
                }
                Text(
                    stringResource(headline),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Body(
                stringResource(if (ready) R.string.lens_how_to else R.string.lens_off_how_to),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            if (ready) {
                Gap(4)
                SwitchRow(
                    title = stringResource(R.string.setting_tab_visible),
                    checked = state.settings.tabVisible,
                    onChange = { visible -> state.update { it.copy(tabVisible = visible) } },
                )
            }
        }
    }
}

/** Android 13+ blocks the accessibility switch for sideloaded apps until "restricted settings" are allowed. */
@Composable
fun RestrictedSettingsHelp() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    if (!expanded) {
        TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.restricted_question)) }
        return
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Hint(stringResource(R.string.restricted_help))
        OutlinedButton(onClick = { SystemActions.openAppInfo(context) }) { Text(stringResource(R.string.btn_app_info)) }
    }
}

/** Screen recording: the other way to capture, for phones where the accessibility service can't be used. */
@Composable
private fun FallbackSection(state: AppState) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(state.fallbackRunning) }
    SectionTitle(stringResource(R.string.fallback_title))
    if (!expanded && !state.fallbackRunning) {
        Group {
            LinkRow(stringResource(R.string.fallback_show), stringResource(R.string.fallback_teaser), R.drawable.ic_screen_record) {
                expanded = true
            }
        }
        return
    }
    Hint(stringResource(R.string.fallback_explain), Modifier.padding(horizontal = 16.dp))
    if (state.fallbackRunning) {
        Button(onClick = state.stopFallback) { Text(stringResource(R.string.btn_stop_fallback)) }
        return
    }
    StatusRow(
        title = stringResource(R.string.req_overlay),
        icon = R.drawable.ic_screen_record,
        ok = state.status.overlayAllowed,
        explanation = stringResource(R.string.req_overlay_why),
        actionLabel = stringResource(R.string.btn_allow),
        onAction = { SystemActions.openOverlayPermission(context) },
    )
    Button(onClick = state.startFallback, enabled = state.status.overlayAllowed && !state.status.accessibilityOn) {
        Text(stringResource(R.string.btn_start_projection))
    }
    if (state.status.accessibilityOn) Hint(stringResource(R.string.accessibility_active))
}
