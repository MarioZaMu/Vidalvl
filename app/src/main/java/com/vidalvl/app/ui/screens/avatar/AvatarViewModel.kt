package com.vidalvl.app.ui.screens.avatar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidalvl.app.data.local.AttributeStateEntity
import com.vidalvl.app.data.local.AvatarProfileEntity
import com.vidalvl.app.data.local.GameRepository
import com.vidalvl.app.data.local.VidalvlDatabase
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.AvatarConfig
import com.vidalvl.app.domain.model.CatalogItem
import com.vidalvl.app.domain.model.GameRules
import com.vidalvl.app.domain.model.ItemCatalog
import com.vidalvl.app.domain.model.ItemSlot
import com.vidalvl.app.ui.screens.missions.AttributeUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface AvatarUiState {
    /** Room has not answered yet; avoids flashing the creator for a user who already has an avatar. */
    data object Loading : AvatarUiState
    data object NotCreated : AvatarUiState
    data class Created(
        val config: AvatarConfig,
        val ownedIds: Set<String>,
        val equipped: Map<ItemSlot, CatalogItem>,
        val attributes: List<AttributeUi>,
        val overallLevel: Int
    ) : AvatarUiState
}

class AvatarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(VidalvlDatabase.getInstance(application))

    val uiState: StateFlow<AvatarUiState> = combine(
        repository.observeAvatar(),
        repository.observeInventory(),
        repository.observeEquipped(),
        repository.observeAttributes()
    ) { profile, inventory, equipped, states ->
        val today = LocalDate.now().toEpochDay()
        if (profile == null) {
            AvatarUiState.NotCreated
        } else {
            AvatarUiState.Created(
                config = AvatarConfig(
                    profile.gender, profile.skinTone, profile.hairStyle, profile.hairColor,
                    profile.eyeStyle, profile.name
                ),
                ownedIds = inventory.map { it.itemId }.toSet(),
                equipped = equipped
                    .mapNotNull { row -> ItemCatalog.byId(row.itemId)?.let { row.slot to it } }
                    .toMap(),
                attributes = Attribute.entries.map { attribute ->
                    val state = states.firstOrNull { it.attribute == attribute } ?: AttributeStateEntity(attribute)
                    AttributeUi(
                        attribute = attribute,
                        level = GameRules.levelFor(state.xp),
                        glow = GameRules.glowFor(state.lastCompletedDayEpoch, today)
                    )
                },
                overallLevel = GameRules.levelFor(states.sumOf { it.xp }).level
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AvatarUiState.Loading)

    fun save(config: AvatarConfig) {
        viewModelScope.launch {
            repository.saveAvatar(
                AvatarProfileEntity(
                    gender = config.gender,
                    skinTone = config.skinTone,
                    hairStyle = config.hairStyle,
                    hairColor = config.hairColor,
                    eyeStyle = config.eyeStyle,
                    name = config.name
                )
            )
        }
    }

    /** Equips the item, or takes it off if it is already the one worn in its slot. */
    fun toggleEquip(item: CatalogItem) {
        val current = (uiState.value as? AvatarUiState.Created)?.equipped?.get(item.slot)
        viewModelScope.launch {
            if (current?.id == item.id) repository.unequip(item.slot)
            else repository.equip(item.slot, item.id)
        }
    }
}
