package com.wchsoftworks.daltonpick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.colorengine.ColorLocalization
import com.wchsoftworks.daltonpick.colorengine.ColorNamer
import com.wchsoftworks.daltonpick.colorengine.Lang
import com.wchsoftworks.daltonpick.colorengine.Srgb

/** Known colors with the name the lens should give each one: practice for users, a check for QA (spec §4.5). */
@Composable
fun TestChartScreen(onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.test_chart_title), onBack = onBack) {
        TestChart()
    }
}

@Composable
fun TestChart() {
    Hint(stringResource(R.string.test_chart_intro))
    SectionTitle(stringResource(R.string.chart_basics))
    SwatchGrid(BASICS)
    SectionTitle(stringResource(R.string.chart_confusions))
    Hint(stringResource(R.string.chart_confusions_hint))
    for (pair in CONFUSION_PAIRS) SwatchGrid(pair.toList(), columns = 2)
    SectionTitle(stringResource(R.string.chart_gradient))
    Hint(stringResource(R.string.chart_gradient_hint))
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF0000), Color(0xFF00FF00)))),
    )
}

@Composable
private fun SwatchGrid(colors: List<Int>, columns: Int = 3) {
    val lang = Lang.fromTag(LocalConfiguration.current.locales[0].toLanguageTag())
    val namer = remember { ColorNamer() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in colors.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (rgb in row) {
                    val label = remember(rgb, lang) { ColorLocalization.label(namer.match(rgb), lang) }
                    val description = if (label.detail.isEmpty()) label.name else "${label.name}, ${label.detail}"
                    Column(
                        Modifier
                            .weight(1f)
                            .clearAndSetSemantics { contentDescription = description },
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF000000 or rgb.toLong()))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        )
                        Text(
                            label.name,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                        Text(
                            Srgb.toHex(rgb),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                // Keep the last, shorter row's swatches the same width as the others.
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private val BASICS = listOf(
    0xFF0000, 0x00FF00, 0x0000FF,
    0xFFFF00, 0x00FFFF, 0xFF00FF,
    0x000000, 0x808080, 0xFFFFFF,
    0x000080, 0x808000, 0xD8A1A4,
    0x228B22, 0x964B00, 0xFFC0CB,
)

/** Pairs that people with color vision deficiency often mix up. */
private val CONFUSION_PAIRS = listOf(
    0xF44336 to 0x4CAF50, // red / green (protan, deutan)
    0x8BC34A to 0xFF9800, // yellow-green / orange
    0x795548 to 0x556B2F, // brown / olive
    0x2196F3 to 0x9C27B0, // blue / purple (tritan, also protan)
    0xF48FB1 to 0xB0BEC5, // pink / blue-gray
    0x009688 to 0x9E9E9E, // teal / gray
)
