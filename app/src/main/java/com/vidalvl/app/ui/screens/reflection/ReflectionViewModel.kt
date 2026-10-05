package com.vidalvl.app.ui.screens.reflection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidalvl.app.data.local.GameRepository
import com.vidalvl.app.data.local.VidalvlDatabase
import com.vidalvl.app.domain.model.Achievement
import com.vidalvl.app.domain.model.AchievementFact
import com.vidalvl.app.domain.model.AchievementProgress
import com.vidalvl.app.domain.model.AchievementRules
import com.vidalvl.app.domain.model.AchievementStats
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.CompletionFact
import com.vidalvl.app.domain.model.DayGroup
import com.vidalvl.app.domain.model.GameRules
import com.vidalvl.app.domain.model.ItemFact
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.model.ReflectionRules
import com.vidalvl.app.domain.model.WeekSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class AchievementRowUi(
    val achievement: Achievement,
    /** Null while locked. */
    val unlockedAtMillis: Long?,
    /** Null for achievements decided by a moment (perfect day, coming back). */
    val progress: AchievementProgress?
) {
    val unlocked: Boolean get() = unlockedAtMillis != null
}

data class ReflectionUiState(
    val week: WeekSummary,
    val timeline: List<DayGroup>,
    val achievements: List<AchievementRowUi>
) {
    val unlockedCount: Int get() = achievements.count { it.unlocked }
}

class ReflectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(VidalvlDatabase.getInstance(application))

    val state: StateFlow<ReflectionUiState> = combine(
        repository.observeHistory(),
        repository.observeInventory(),
        repository.observeAchievements(),
        repository.observeAttributes()
    ) { history, inventory, unlocked, attributeStates ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).toEpochDay()

        val completions = history.map {
            CompletionFact(it.completedAtMillis, it.dayEpoch, it.title, it.attribute, it.xpEarned, it.type)
        }
        val week = ReflectionRules.weekSummary(completions, today)
        val unlockedAt = unlocked.mapNotNull { row ->
            Achievement.entries.firstOrNull { it.name == row.id }?.let { it to row.unlockedAtMillis }
        }.toMap()

        val stats = AchievementStats(
            totalCompletions = completions.size,
            weeklyCompletions = completions.count { it.type == MissionType.WEEKLY },
            bestStreak = week.bestStreak,
            totalLevel = GameRules.levelFor(attributeStates.sumOf { it.xp }).level,
            attributeLevels = Attribute.entries.associateWith { attribute ->
                GameRules.levelFor(attributeStates.firstOrNull { it.attribute == attribute }?.xp ?: 0).level
            },
            itemCount = inventory.size
        )

        val rows = Achievement.entries.map { achievement ->
            AchievementRowUi(achievement, unlockedAt[achievement], AchievementRules.progress(achievement, stats))
        }
        // Unlocked first (latest on top), then the locked ones closest to being earned.
        val ordered = rows.filter { it.unlocked }.sortedByDescending { it.unlockedAtMillis } +
                rows.filter { !it.unlocked }.sortedByDescending { it.progress?.fraction ?: -1f }

        ReflectionUiState(
            week = week,
            timeline = ReflectionRules.timeline(
                completions = completions,
                items = inventory.map { ItemFact(it.itemId, it.acquiredAtMillis) },
                achievements = unlockedAt.map { (achievement, millis) -> AchievementFact(achievement, millis) },
                zone = zone
            ),
            achievements = ordered
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReflectionUiState(
            week = ReflectionRules.weekSummary(emptyList(), LocalDate.now().toEpochDay()),
            timeline = emptyList(),
            achievements = emptyList()
        )
    )
}
