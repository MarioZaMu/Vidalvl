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
import com.vidalvl.app.domain.model.AvatarConfig
import com.vidalvl.app.domain.model.CatalogItem
import com.vidalvl.app.domain.model.GameRules
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.ItemCatalog
import com.vidalvl.app.domain.model.ItemSlot
import com.vidalvl.app.domain.model.LevelInfo
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.model.StreakRules
import com.vidalvl.app.domain.suggestions.SuggestionOutcome
import com.vidalvl.app.domain.suggestions.SuggestionService
import com.vidalvl.app.BuildConfig
import com.vidalvl.app.data.local.ResourceFallbackSuggestions
import com.vidalvl.app.data.local.RoomSuggestionStore
import com.vidalvl.app.data.local.SuggestionPrefs
import com.vidalvl.app.data.local.VerificationPrefs
import com.vidalvl.app.data.remote.GeminiApi
import com.vidalvl.app.data.remote.GeminiMissionSuggester
import com.vidalvl.app.data.remote.GeminiPhotoVerifier
import com.vidalvl.app.domain.verification.LimitedPhotoVerifier
import com.vidalvl.app.domain.verification.PhotoVerifier
import com.vidalvl.app.domain.verification.VerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class MissionItem(val mission: MissionEntity, val completed: Boolean)

data class AttributeUi(val attribute: Attribute, val level: LevelInfo, val glow: Float)

data class MissionsUiState(
    val daily: List<MissionItem> = emptyList(),
    val weekly: List<MissionItem> = emptyList(),
    val attributes: List<AttributeUi> = emptyList(),
    val overallLevel: LevelInfo = GameRules.levelFor(0),
    val totalXp: Int = 0
)

/** What the header shows about the player. [name] is blank until the user picks one. */
data class PlayerUi(
    val name: String = "",
    val config: AvatarConfig = AvatarConfig(),
    val equipped: Map<ItemSlot, CatalogItem> = emptyMap(),
    val streak: Int = 0,
    val itemCount: Int = 0,
    val goals: Set<Goal> = emptySet(),
    /** Days since the last completed mission; null if there never was one. */
    val daysSinceLast: Int? = null
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
            totalXp = states.sumOf { it.xp },
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

    val player: StateFlow<PlayerUi> = combine(
        repository.observeAvatar(),
        repository.observeEquipped(),
        repository.observeInventory(),
        repository.observeAllCompletions(),
        repository.observeGoals()
    ) { avatar, equipped, inventory, completions, goals ->
        val today = LocalDate.now().toEpochDay()
        PlayerUi(
            goals = goals?.goals.orEmpty(),
            daysSinceLast = completions.maxOfOrNull { it.dayEpoch }?.let { (today - it).toInt() },
            name = avatar?.name.orEmpty(),
            config = avatar?.let {
                AvatarConfig(it.gender, it.skinTone, it.hairStyle, it.hairColor, it.eyeStyle, it.name)
            } ?: AvatarConfig(),
            equipped = equipped
                .mapNotNull { row -> ItemCatalog.byId(row.itemId)?.let { row.slot to it } }
                .toMap(),
            streak = StreakRules.currentStreak(
                completions.map { it.dayEpoch }.toSet(),
                LocalDate.now().toEpochDay()
            ),
            itemCount = inventory.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUi())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    private val _newItems = MutableStateFlow<List<CatalogItem>>(emptyList())

    /** Items just earned and not yet acknowledged by the user. */
    val newItems: StateFlow<List<CatalogItem>> = _newItems

    fun toggle(item: MissionItem) {
        viewModelScope.launch {
            if (item.completed) {
                repository.undoMission(item.mission)
            } else {
                val earned = repository.completeMission(item.mission)
                if (earned.isNotEmpty()) _newItems.value = earned
            }
        }
    }

    fun createMission(
        title: String,
        description: String,
        attribute: Attribute,
        type: MissionType,
        requiresPhoto: Boolean
    ) {
        viewModelScope.launch {
            repository.createMission(title, description, attribute, type, requiresPhoto)
        }
    }

    fun updateMission(mission: MissionEntity, title: String, description: String, requiresPhoto: Boolean) {
        viewModelScope.launch { repository.updateMissionDetails(mission.id, title, description, requiresPhoto) }
    }

    private val verificationPrefs = VerificationPrefs(application)
    private val geminiApi = GeminiApi.create()
    private fun appLanguage() = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH).ifBlank { "English" }

    /** Gemini behind the daily cap. The cap counts only real verdicts, never network errors. */
    private val verifier: PhotoVerifier = LimitedPhotoVerifier(
        inner = GeminiPhotoVerifier(
            api = geminiApi,
            apiKey = BuildConfig.GEMINI_API_KEY,
            model = BuildConfig.GEMINI_MODEL,
            language = ::appLanguage
        ),
        usage = verificationPrefs,
        today = { LocalDate.now().toEpochDay() }
    )

    private val suggestionPrefs = SuggestionPrefs(application)

    /** Daily and weekly suggested missions: Gemini when the user agreed, built-in ideas otherwise. */
    private val suggestionService = SuggestionService(
        store = RoomSuggestionStore(VidalvlDatabase.getInstance(application)),
        suggester = GeminiMissionSuggester(geminiApi, BuildConfig.GEMINI_API_KEY, BuildConfig.GEMINI_MODEL),
        fallback = ResourceFallbackSuggestions(application),
        settings = suggestionPrefs,
        language = ::appLanguage,
        today = { LocalDate.now().toEpochDay() }
    )

    private val _generating = MutableStateFlow(false)

    /** True while today's suggestions are being made. */
    val generating: StateFlow<Boolean> = _generating

    private val _needsSuggestionChoice = MutableStateFlow(false)

    /** True when the user must still choose between AI and built-in ideas. */
    val needsSuggestionChoice: StateFlow<Boolean> = _needsSuggestionChoice

    private val _aiEnabled = MutableStateFlow(suggestionPrefs.aiEnabled)

    /** Null until the user has chosen. */
    val aiEnabled: StateFlow<Boolean?> = _aiEnabled

    /** Makes today's (and this week's) suggestions if they do not exist yet. Safe to call often. */
    fun refreshSuggestions() {
        viewModelScope.launch {
            _generating.value = true
            try {
                _needsSuggestionChoice.value = suggestionService.ensureCurrent() == SuggestionOutcome.NEEDS_CONSENT
            } finally {
                _generating.value = false
            }
        }
    }

    /** First-time choice: AI (the user accepts that goals and mission titles go to Google) or built-in ideas. */
    fun chooseSuggestions(useAi: Boolean) {
        setAiEnabled(useAi)
        _needsSuggestionChoice.value = false
        refreshSuggestions()
    }

    fun setAiEnabled(useAi: Boolean) {
        suggestionPrefs.aiEnabled = useAi
        _aiEnabled.value = useAi
    }

    private val _consentGiven = MutableStateFlow(verificationPrefs.consentGiven)

    /** Whether the user already accepted that photos are sent to Google for checking. */
    val consentGiven: StateFlow<Boolean> = _consentGiven

    fun giveConsent() {
        verificationPrefs.consentGiven = true
        _consentGiven.value = true
    }

    suspend fun verifyPhoto(photoJpeg: ByteArray, mission: MissionEntity): VerificationResult =
        verifier.verify(photoJpeg, mission.title, mission.description)

    fun deleteMission(mission: MissionEntity) {
        viewModelScope.launch { repository.deleteMission(mission.id) }
    }

    fun dismissNewItems() {
        _newItems.value = emptyList()
    }

    /** Debug tool, see ProfileScreen. */
    fun debugGrantAllItems(onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repository.debugGrantAllItems()) }
    }

    /** Debug tool, see ProfileScreen. Returns how many items it granted. */
    fun debugAddXp(attribute: Attribute, amount: Int, onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repository.debugAddXp(attribute, amount).size) }
    }
}

private fun List<MissionEntity>.toItems(done: List<MissionCompletionEntity>): List<MissionItem> {
    val doneIds = done.map { it.missionId }.toSet()
    return map { MissionItem(it, it.id in doneIds) }
}