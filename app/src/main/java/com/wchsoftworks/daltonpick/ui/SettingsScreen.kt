package com.wchsoftworks.daltonpick.ui

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.settings.Edge
import com.wchsoftworks.daltonpick.settings.Settings

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    val s = state.settings
    val update = state.update
    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        SectionTitle(stringResource(R.string.settings_tab))
        SwitchRow(stringResource(R.string.setting_tab_visible), s.tabVisible, { v -> update { it.copy(tabVisible = v) } })
        Text(stringResource(R.string.setting_edge), style = MaterialTheme.typography.bodyLarge)
        Segmented(
            options = listOf(Edge.LEFT to stringResource(R.string.edge_left), Edge.RIGHT to stringResource(R.string.edge_right)),
            selected = s.edge,
            onSelect = { e -> update { it.copy(edge = e) } },
        )
        SwitchRow(
            stringResource(R.string.setting_auto_dim), s.autoDim, { v -> update { it.copy(autoDim = v) } },
            subtitle = stringResource(R.string.setting_auto_dim_hint),
        )

        SectionTitle(stringResource(R.string.settings_lens))
        IntSliderRow(
            stringResource(R.string.setting_lens_size, s.lensSizeDp), s.lensSizeDp, Settings.LENS_SIZE_RANGE_DP, step = 10,
        ) { v -> update { it.copy(lensSizeDp = v) } }
        IntSliderRow(stringResource(R.string.setting_zoom, s.zoom), s.zoom, Settings.ZOOM_RANGE, step = 1) { v ->
            update { it.copy(zoom = v) }
        }
        Text(stringResource(R.string.setting_sample), style = MaterialTheme.typography.bodyLarge)
        Segmented(
            options = Settings.SAMPLE_SIZES.map { it to stringResource(R.string.sample_size_option, it) },
            selected = s.sampleSize,
            onSelect = { v -> update { it.copy(sampleSize = v) } },
        )
        Hint(stringResource(R.string.setting_sample_hint))

        SectionTitle(stringResource(R.string.settings_label))
        SwitchRow(stringResource(R.string.setting_show_hex), s.showHex, { v -> update { it.copy(showHex = v) } })
        SwitchRow(stringResource(R.string.setting_haptics), s.haptics, { v -> update { it.copy(haptics = v) } })

        LanguageSection()

        SectionTitle(stringResource(R.string.setting_developer))
        SwitchRow(stringResource(R.string.setting_show_debug), s.showDebug, { v -> update { it.copy(showDebug = v) } })
        SwitchRow(stringResource(R.string.setting_secure_test), state.secureTest, state.setSecureTest)
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}

/**
 * Per-app language (spec §4.5). Android 13+ stores it for us and applies it to the overlay too.
 * Older versions follow the system language, so the section is hidden there.
 */
@Composable
private fun LanguageSection() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val manager = remember { context.getSystemService(LocaleManager::class.java) }
    var current by remember { mutableStateOf(manager.applicationLocales.toLanguageTags()) }
    // Language names are written in their own language, as system pickers do.
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "en" to "English",
        "es" to "Español",
        "pt-BR" to "Português (Brasil)",
    )
    SectionTitle(stringResource(R.string.settings_language))
    Column(Modifier.selectableGroup()) {
        for ((tag, name) in options) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = current == tag,
                        role = Role.RadioButton,
                        onClick = {
                            current = tag
                            // The activity restarts in the new language.
                            manager.applicationLocales = if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = current == tag, onClick = null)
                Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}
