package com.vidalvl.app.ui.screens.creature

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.CreatureMood
import com.vidalvl.app.domain.model.CreatureRules
import com.vidalvl.app.domain.model.CreatureStage
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import com.vidalvl.app.ui.components.StatBar
import com.vidalvl.app.ui.creature.CreatureLook
import com.vidalvl.app.ui.creature.DragonView
import com.vidalvl.app.ui.screens.missions.AttributeUi
import com.vidalvl.app.ui.screens.missions.MissionsViewModel
import com.vidalvl.app.ui.theme.VidalvlTheme
import com.vidalvl.app.ui.theme.forAttribute
import com.vidalvl.app.ui.theme.label

@Composable
fun CreatureScreen(viewModel: MissionsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val colors = VidalvlTheme.attributeColors
    var stageOverride by remember { mutableStateOf<CreatureStage?>(null) }
    var moodOverride by remember { mutableStateOf<CreatureMood?>(null) }

    val level = state.overallLevel.level
    val averageGlow =
        if (state.attributes.isEmpty()) 0.6f
        else state.attributes.map { it.glow }.average().toFloat()
    val stage = stageOverride ?: CreatureRules.stageFor(level)
    val mood = moodOverride ?: CreatureRules.moodFor(averageGlow)
    val glow = when (moodOverride) {
        CreatureMood.HAPPY -> 1f
        CreatureMood.NEUTRAL -> 0.75f
        CreatureMood.BORED -> 0.5f
        CreatureMood.SAD -> 0.3f
        null -> averageGlow
    }

    fun strengthOf(attribute: Attribute): Float =
        state.attributes.firstOrNull { it.attribute == attribute }
            ?.let { CreatureRules.strengthOf(it.level.level) } ?: 0f

    val dominant = state.attributes.maxWithOrNull(
        compareBy<AttributeUi>({ it.level.level }, { it.level.progress })
    )
    val auraColor =
        if (dominant == null || (dominant.level.level == 1 && dominant.level.progress == 0f)) colors.xp
        else colors.forAttribute(dominant.attribute)

    val look = CreatureLook(
        stage = stage,
        mood = mood,
        glow = glow,
        body = strengthOf(Attribute.BODY),
        mind = strengthOf(Attribute.MIND),
        discipline = strengthOf(Attribute.DISCIPLINE),
        social = strengthOf(Attribute.SOCIAL),
        auraColor = auraColor,
        sparkleColor = colors.mind
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Tu compañero", style = MaterialTheme.typography.headlineLarge)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DragonView(
                look = look,
                modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(stageName(stage), style = MaterialTheme.typography.titleLarge)
            Text(
                moodText(mood),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                nextEvolutionText(CreatureRules.stageFor(level)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.attributes.forEach { item ->
            StatBar(
                label = item.attribute.label(),
                valueText = "Nv ${item.level.level}",
                progress = item.level.progress,
                color = colors.forAttribute(item.attribute)
            )
        }

        Text("Vista previa", style = MaterialTheme.typography.titleMedium)
        Text(
            "Solo para pruebas: fuerza una etapa o un ánimo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OptionRow(
            options = listOf<Pair<String, CreatureStage?>>(
                "Auto" to null,
                "Cría" to CreatureStage.HATCHLING,
                "Adolescente" to CreatureStage.CHILD,
                "Joven" to CreatureStage.YOUTH
            ),
            selected = stageOverride,
            onSelect = { stageOverride = it }
        )
        OptionRow(
            options = listOf<Pair<String, CreatureMood?>>(
                "Auto" to null,
                "Feliz" to CreatureMood.HAPPY,
                "Normal" to CreatureMood.NEUTRAL,
                "Aburrido" to CreatureMood.BORED,
                "Triste" to CreatureMood.SAD
            ),
            selected = moodOverride,
            onSelect = { moodOverride = it }
        )
    }
}

@Composable
private fun <T> OptionRow(
    options: List<Pair<String, T?>>,
    selected: T?,
    onSelect: (T?) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, value) ->
            if (value == selected) PrimaryButton(label, onClick = { onSelect(value) })
            else SecondaryButton(label, onClick = { onSelect(value) })
        }
    }
}

private fun stageName(stage: CreatureStage): String = when (stage) {
    CreatureStage.HATCHLING -> "Cría"
    CreatureStage.CHILD -> "Adolescente"
    CreatureStage.YOUTH -> "Joven"
}

private fun moodText(mood: CreatureMood): String = when (mood) {
    CreatureMood.HAPPY -> "Está feliz y lleno de energía"
    CreatureMood.NEUTRAL -> "Se siente tranquilo"
    CreatureMood.BORED -> "Se está aburriendo, hace falta actividad"
    CreatureMood.SAD -> "Está triste, te extraña"
}

private fun nextEvolutionText(stage: CreatureStage): String = when (stage) {
    CreatureStage.HATCHLING -> "Evoluciona en el nivel ${CreatureRules.CHILD_LEVEL}"
    CreatureStage.CHILD -> "Evoluciona en el nivel ${CreatureRules.YOUTH_LEVEL}"
    CreatureStage.YOUTH -> "Alcanzó su máxima evolución"
}