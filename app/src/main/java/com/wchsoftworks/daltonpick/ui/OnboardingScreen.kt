package com.wchsoftworks.daltonpick.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.wchsoftworks.daltonpick.Links
import com.wchsoftworks.daltonpick.R

private enum class Step { WELCOME, LENS, NOTIFICATIONS, PRACTICE, DONE }

/**
 * First run (spec §4.5): what the app does → turn on the accessibility service → notifications →
 * practice on the test chart → done, with one low-key Ko-fi line (spec §7). Every step can be skipped.
 */
@Composable
fun OnboardingScreen(state: AppState) {
    val steps = Step.entries.filter { it != Step.NOTIFICATIONS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index.coerceIn(steps.indices)]
    val finish = { state.update { it.copy(onboardingDone = true) } }

    BackHandler(enabled = index > 0) { index-- }

    Scaffold(
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step != Step.DONE) {
                    TextButton(onClick = finish) { Text(stringResource(R.string.onboarding_skip)) }
                }
                Spacer(Modifier.weight(1f))
                ProgressDots(index, steps.size, stringResource(R.string.onboarding_progress, index + 1, steps.size))
                if (step == Step.DONE) {
                    Button(onClick = finish) { Text(stringResource(R.string.onboarding_start)) }
                } else {
                    Button(onClick = { index++ }) { Text(stringResource(R.string.onboarding_next)) }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                Step.WELCOME -> Welcome()
                Step.LENS -> TurnOnLens(state)
                Step.NOTIFICATIONS -> Notifications(state)
                Step.PRACTICE -> Practice(state)
                Step.DONE -> Done()
            }
        }
    }
}

/** Where you are in the steps: one dot per step, the current one wider. Read out as "2 of 5". */
@Composable
private fun ProgressDots(index: Int, count: Int, description: String) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val current = i == index
            Box(
                Modifier
                    .size(width = if (current) 20.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Welcome() {
    AppMark(72.dp)
    Title(stringResource(R.string.onb_welcome_title))
    Body(stringResource(R.string.onb_welcome_body))
    Body(stringResource(R.string.onb_welcome_privacy))
}

@Composable
private fun TurnOnLens(state: AppState) {
    val context = LocalContext.current
    Title(stringResource(R.string.onb_lens_title))
    Body(stringResource(R.string.onb_lens_body))
    Body(stringResource(R.string.onb_lens_warning))
    StatusRow(
        title = stringResource(R.string.req_accessibility),
        icon = R.drawable.ic_lens,
        ok = state.status.accessibilityOn,
        explanation = stringResource(R.string.onb_lens_steps),
        actionLabel = stringResource(R.string.btn_open_accessibility),
        onAction = { SystemActions.openAccessibility(context) },
    )
    if (!state.status.accessibilityOn) RestrictedSettingsHelp()
}

@Composable
private fun Notifications(state: AppState) {
    Title(stringResource(R.string.onb_notifications_title))
    Body(stringResource(R.string.onb_notifications_body))
    if (state.status.notificationsAllowed) {
        Body("✓ " + stringResource(R.string.onb_notifications_done))
    } else {
        FilledTonalButton(onClick = state.requestNotifications) { Text(stringResource(R.string.btn_allow)) }
    }
}

@Composable
private fun Practice(state: AppState) {
    Title(stringResource(R.string.onb_practice_title))
    if (state.host != null) {
        Body(stringResource(R.string.onb_practice_body))
    } else {
        Body(stringResource(R.string.onb_practice_lens_off))
    }
    TestChart()
}

@Composable
private fun Done() {
    val context = LocalContext.current
    Title(stringResource(R.string.onb_done_title))
    Body(stringResource(R.string.onb_done_body))
    // The one Ko-fi mention outside the About screen (spec §7): a line, not a popup.
    val linkText = stringResource(R.string.onb_kofi_link)
    val line = stringResource(R.string.onb_kofi_line, linkText)
    val start = line.indexOf(linkText)
    val text = buildAnnotatedString {
        append(if (start >= 0) line.substring(0, start) else "$line ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "kofi",
                styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.primary)),
                linkInteractionListener = { SystemActions.openUrl(context, Links.KOFI) },
            ),
        ) { append(linkText) }
        if (start >= 0) append(line.substring(start + linkText.length))
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
