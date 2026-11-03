package com.wchsoftworks.daltonpick.ui

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wchsoftworks.daltonpick.Links
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.colorengine.Lang

/** Version, privacy, licenses and the Ko-fi button (spec §4.5, §7). */
@Composable
fun AboutScreen(state: AppState, onBack: () -> Unit, open: (Screen) -> Unit) {
    val context = LocalContext.current
    val lang = Lang.fromTag(LocalConfiguration.current.locales[0].toLanguageTag())
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    var versionTaps by remember { mutableIntStateOf(0) }
    ScreenScaffold(title = stringResource(R.string.about_title), onBack = onBack) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppMark(88.dp)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            // Tapping the version a few times unlocks the Developer section in Settings, as on Android itself.
            Hint(
                stringResource(R.string.about_version, version ?: "?"),
                Modifier.clickable(interactionSource = null, indication = null) {
                    versionTaps++
                    if (versionTaps == DEVELOPER_TAPS && !state.settings.developerOptions) {
                        state.update { it.copy(developerOptions = true) }
                        Toast.makeText(context, R.string.developer_unlocked, Toast.LENGTH_LONG).show()
                    }
                },
            )
            Text(
                stringResource(R.string.about_free),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        SectionTitle(stringResource(R.string.about_support))
        Group {
            GroupText(stringResource(R.string.about_support_text))
            Button(
                onClick = { SystemActions.openUrl(context, Links.KOFI) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.btn_kofi))
            }
            GroupText(stringResource(R.string.about_support_nothing_unlocked))
        }

        SectionTitle(stringResource(R.string.about_more))
        Group {
            LinkRow(stringResource(R.string.about_privacy), stringResource(R.string.about_privacy_summary)) {
                SystemActions.openUrl(context, Links.privacyPolicy(lang))
            }
            LinkRow(stringResource(R.string.about_source), Links.SOURCE) { SystemActions.openUrl(context, Links.SOURCE) }
            LinkRow(stringResource(R.string.licenses_title)) { open(Screen.LICENSES) }
            LinkRow(stringResource(R.string.about_color_correction), stringResource(R.string.about_color_correction_summary)) {
                SystemActions.openAccessibilitySettingsList(context)
            }
        }
    }
}

private const val DEVELOPER_TAPS = 7

/** DaltonPick's own MIT license, then the libraries it ships with. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ScreenScaffold(title = stringResource(R.string.licenses_title), onBack = onBack) {
        SectionTitle("DaltonPick")
        Text(MIT_LICENSE, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)

        SectionTitle(stringResource(R.string.licenses_libraries))
        Hint(stringResource(R.string.licenses_apache))
        for (library in LIBRARIES) Body("• $library")
        Group {
            LinkRow(stringResource(R.string.licenses_apache_link), Links.APACHE_2) { SystemActions.openUrl(context, Links.APACHE_2) }
        }
    }
}

private val LIBRARIES = listOf(
    "AndroidX Activity",
    "AndroidX Compose (UI, Material 3)",
    "AndroidX DataStore",
    "AndroidX DynamicAnimation",
    "AndroidX Lifecycle",
    "Kotlin standard library",
    "kotlinx.coroutines",
)

// Same text as LICENSE in the repository.
private const val MIT_LICENSE = """MIT License

Copyright (c) 2026 Guilherme Corneto

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."""
