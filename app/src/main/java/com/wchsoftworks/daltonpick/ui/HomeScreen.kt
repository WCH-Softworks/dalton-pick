package com.wchsoftworks.daltonpick.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.camera.CameraActivity

/** Whether the lens works right now, what's missing, and the way into everything else. */
@Composable
fun HomeScreen(state: AppState, open: (Screen) -> Unit) {
    val context = LocalContext.current
    val status = state.status
    ScreenScaffold(title = stringResource(R.string.app_name), onBack = null) {
        LensCard(state)

        SectionTitle(stringResource(R.string.home_setup))
        StatusRow(
            title = stringResource(R.string.req_accessibility),
            ok = status.accessibilityOn,
            explanation = stringResource(R.string.req_accessibility_why),
            actionLabel = stringResource(R.string.btn_turn_on),
            onAction = { SystemActions.openAccessibility(context) },
        )
        if (!status.accessibilityOn) RestrictedSettingsHelp()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StatusRow(
                title = stringResource(R.string.req_notifications),
                ok = status.notificationsAllowed,
                explanation = stringResource(R.string.req_notifications_why),
                actionLabel = stringResource(R.string.btn_allow),
                onAction = state.requestNotifications,
            )
        }

        SectionTitle(stringResource(R.string.home_more))
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            LinkRow(stringResource(R.string.camera_title), stringResource(R.string.camera_subtitle)) {
                context.startActivity(Intent(context, CameraActivity::class.java))
            }
        }
        LinkRow(stringResource(R.string.test_chart_title), stringResource(R.string.test_chart_subtitle)) { open(Screen.TEST_CHART) }
        LinkRow(stringResource(R.string.settings_title)) { open(Screen.SETTINGS) }
        LinkRow(stringResource(R.string.about_title)) { open(Screen.ABOUT) }

        FallbackSection(state)
    }
}

@Composable
private fun LensCard(state: AppState) {
    val ready = state.host != null
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val headline = when {
                state.fallbackRunning -> R.string.lens_on_fallback
                ready -> R.string.lens_on
                state.status.accessibilityOn -> R.string.lens_starting
                else -> R.string.lens_off
            }
            Text(
                stringResource(headline),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Body(stringResource(if (ready) R.string.lens_how_to else R.string.lens_off_how_to))
            if (ready) {
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        LinkRow(stringResource(R.string.fallback_show), stringResource(R.string.fallback_teaser)) { expanded = true }
        return
    }
    Hint(stringResource(R.string.fallback_explain))
    if (state.fallbackRunning) {
        Button(onClick = state.stopFallback) { Text(stringResource(R.string.btn_stop_fallback)) }
        return
    }
    StatusRow(
        title = stringResource(R.string.req_overlay),
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
