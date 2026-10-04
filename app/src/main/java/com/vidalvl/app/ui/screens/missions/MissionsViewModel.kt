package com.vidalvl.app.ui.screens.missions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidalvl.app.data.local.AttributeStateEntity
import com.vidalvl.app.data.local.GameRepository
import com.vidalvl.app.data.local.MissionCompletionEntity
import com.vidalvl.app.data.local.MissionEntity
import com.vidalvl.app.data.local.VidalvlDatabase
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.GameRules
import com.vidalvl.app.domain.model.LevelInfo
import com.vidalvl.app.domain.model.MissionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MissionItem(val mission: MissionEntity, val completed: Boolean)

data class AttributeUi(val attribute: Attribute, val level: LevelInfo, val glow: Float)

data class MissionsUiState(
    val daily: List<MissionItem> = emptyList(),
    val weekly: List<MissionItem> = emptyList(),
    val attributes: List<AttributeUi> = emptyList(),
    val overallLevel: LevelInfo = GameRules.levelFor(0)
)

class MissionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(VidalvlDatabase.getInstance(application))

    val uiState: StateFlow<MissionsUiState> = combine(
        repository.observeMissions(MissionType.DAILY),
        repository.observeCompletions(MissionType.DAILY),
        repository.observeMissions(MissionType.WEEKLY),
        repository.observeCompletions(MissionType.WEEKLY),
        repository.observeAttributes()
    ) { daily, dailyDone, weekly, weeklyDone, states ->
        val today = LocalDate.now().toEpochDay()
        MissionsUiState(
            daily = daily.toItems(dailyDone),
            weekly = weekly.toItems(weeklyDone),
            overallLevel = GameRules.levelFor(states.sumOf { it.xp }),
            attributes = Attribute.entries.map { attribute ->
                val state = states.firstOrNull { it.attribute == attribute }
                    ?: AttributeStateEntity(attribute)
                AttributeUi(
                    attribute = attribute,
                    level = GameRules.levelFor(state.xp),
                    glow = GameRules.glowFor(state.lastCompletedDayEpoch, today)
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MissionsUiState())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun toggle(item: MissionItem) {
        viewModelScope.launch {
            if (item.completed) repository.undoMission(item.mission)
            else repository.completeMission(item.mission)
        }
    }
}

private fun List<MissionEntity>.toItems(done: List<MissionCompletionEntity>): List<MissionItem> {
    val doneIds = done.map { it.missionId }.toSet()
    return map { MissionItem(it, it.id in doneIds) }
}