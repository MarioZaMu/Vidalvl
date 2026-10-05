package com.vidalvl.app.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidalvl.app.data.local.GameRepository
import com.vidalvl.app.data.local.VidalvlDatabase
import com.vidalvl.app.domain.model.PlayerGoals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GoalsState {
    /** Room has not answered yet; avoids flashing the welcome screen for someone who already did it. */
    data object Loading : GoalsState
    data object Missing : GoalsState
    data class Saved(val goals: PlayerGoals) : GoalsState
}

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(VidalvlDatabase.getInstance(application))

    val state: StateFlow<GoalsState> = repository.observeGoals()
        .map { goals -> if (goals == null) GoalsState.Missing else GoalsState.Saved(goals) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsState.Loading)

    /** True only in the session where the welcome screen was completed: the app then opens on the Avatar tab. */
    var startOnAvatar: Boolean = false
        private set

    fun finishWelcome(goals: PlayerGoals) {
        startOnAvatar = true
        save(goals)
    }

    fun save(goals: PlayerGoals) {
        viewModelScope.launch { repository.saveGoals(goals) }
    }
}
