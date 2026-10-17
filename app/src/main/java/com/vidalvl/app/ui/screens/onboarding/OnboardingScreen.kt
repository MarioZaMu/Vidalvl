package com.vidalvl.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.AvatarConfig
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.GoalRules
import com.vidalvl.app.domain.model.PlayerGoals
import com.vidalvl.app.ui.avatar.AvatarView
import com.vidalvl.app.ui.components.PixelCornerShape
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.ui.components.pixelShadow

private const val LAST_STEP = 2
private const val WORDMARK_RATIO = 1032f / 480f

/** First launch: a welcome, "why are you here?" and "how much time do you have?". */
@Composable
fun OnboardingScreen(onFinish: (PlayerGoals) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var goals by rememberSaveable { mutableStateOf<Set<Goal>>(emptySet()) }
    var minutes by rememberSaveable { mutableIntStateOf(GoalRules.DEFAULT_MINUTES) }

    val canAdvance = when (step) {
        1 -> GoalRules.canContinue(goals)
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (step > 0) {
            Text(
                text = stringResource(R.string.onboarding_step, step, LAST_STEP),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboardingStep"
        ) { current ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (current) {
                    0 -> Welcome()
                    1 -> {
                        StepTitle(stringResource(R.string.onboarding_goals_title))
                        Text(
                            stringResource(R.string.onboarding_goals_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.goals_counter, goals.size, GoalRules.MAX_GOALS),
                            style = MaterialTheme.typography.labelLarge
                        )
                        GoalPicker(goals) { goals = GoalRules.toggle(goals, it) }
                    }

                    else -> {
                        StepTitle(stringResource(R.string.onboarding_time_title))
                        Text(
                            stringResource(R.string.onboarding_time_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TimePicker(minutes) { minutes = it }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) {
                SecondaryButton(
                    text = stringResource(R.string.onboarding_back),
                    onClick = { step-- },
                    modifier = Modifier.weight(1f)
                )
            }
            PrimaryButton(
                text = stringResource(
                    when (step) {
                        0 -> R.string.onboarding_start
                        LAST_STEP -> R.string.onboarding_finish
                        else -> R.string.onboarding_next
                    }
                ),
                onClick = {
                    if (step == LAST_STEP) onFinish(PlayerGoals(goals, minutes)) else step++
                },
                modifier = Modifier.weight(1f),
                enabled = canAdvance
            )
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun Welcome() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val cardShape = remember { PixelCornerShape(unit = 3.dp, steps = 2) }
        // The wordmark has its own navy background, so it sits in a card that works on both themes.
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.vidalvl_wordmark),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(WORDMARK_RATIO)
                .pixelShadow(cardShape, MaterialTheme.colorScheme.outline)
                .clip(cardShape)
        )
        AvatarView(config = AvatarConfig(), modifier = Modifier.size(150.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_text),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
