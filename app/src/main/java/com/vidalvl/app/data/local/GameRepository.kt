package com.vidalvl.app.data.local

import androidx.room.withTransaction
import com.vidalvl.app.domain.model.Achievement
import com.vidalvl.app.domain.model.AchievementRules
import com.vidalvl.app.domain.model.AchievementStats
import com.vidalvl.app.domain.model.AchievementUnlock
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.CatalogItem
import com.vidalvl.app.domain.model.GameRules
import com.vidalvl.app.domain.model.GoalRules
import com.vidalvl.app.domain.model.ItemCatalog
import com.vidalvl.app.domain.model.ItemSlot
import com.vidalvl.app.domain.model.ItemSource
import com.vidalvl.app.domain.model.MissionRules
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.model.PlayerGoals
import com.vidalvl.app.domain.model.RewardRules
import com.vidalvl.app.domain.model.StreakRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/** What completing a mission earned: level or weekly items, and achievements (with their prizes). */
data class CompletionResult(
    val items: List<CatalogItem>,
    val achievements: List<AchievementUnlock>
)

private const val MAX_ACHIEVEMENT_PASSES = 3

class GameRepository(private val db: VidalvlDatabase) {
    private val missionDao = db.missionDao()
    private val completionDao = db.completionDao()
    private val attributeDao = db.attributeDao()
    private val avatarDao = db.avatarDao()
    private val inventoryDao = db.inventoryDao()
    private val equippedDao = db.equippedDao()
    private val goalsDao = db.goalsDao()
    private val achievementDao = db.achievementDao()

    /** Emits null until the user finishes the welcome screen. */
    fun observeGoals(): Flow<PlayerGoals?> = goalsDao.observe().map { row ->
        row?.let { PlayerGoals(GoalRules.decode(it.goals), it.dailyMinutes) }
    }

    suspend fun saveGoals(goals: PlayerGoals) = goalsDao.upsert(
        PlayerGoalsEntity(goals = GoalRules.encode(goals.goals), dailyMinutes = goals.dailyMinutes)
    )

    /** Emits null while the avatar has not been created yet. */
    fun observeAvatar(): Flow<AvatarProfileEntity?> = avatarDao.observe()

    suspend fun saveAvatar(profile: AvatarProfileEntity) = avatarDao.upsert(profile)

    fun observeInventory(): Flow<List<InventoryItemEntity>> = inventoryDao.observeAll()

    fun observeEquipped(): Flow<List<EquippedEntity>> = equippedDao.observeAll()

    suspend fun equip(slot: ItemSlot, itemId: String) = equippedDao.upsert(EquippedEntity(slot, itemId))

    suspend fun unequip(slot: ItemSlot) = equippedDao.clear(slot)

    fun observeMissions(type: MissionType): Flow<List<MissionEntity>> =
        missionDao.observeActive(type)

    fun observeAttributes(): Flow<List<AttributeStateEntity>> = attributeDao.observeAll()

    /** Every completion ever made; used for the streak. */
    fun observeAllCompletions(): Flow<List<MissionCompletionEntity>> = completionDao.observeAll()

    /** Completions inside the current period (today for daily, this week for weekly). */
    fun observeCompletions(type: MissionType): Flow<List<MissionCompletionEntity>> {
        val range = periodRange(type, LocalDate.now().toEpochDay())
        return completionDao.observeBetween(range.first, range.last)
    }

    /** Creates a user mission. XP comes from the type, so it cannot be chosen. */
    suspend fun createMission(
        title: String,
        description: String?,
        attribute: Attribute,
        type: MissionType,
        requiresPhoto: Boolean
    ) {
        missionDao.insert(
            MissionEntity(
                title = MissionRules.cleanTitle(title),
                description = description?.let { MissionRules.cleanDescription(it) },
                attribute = attribute,
                type = type,
                xpReward = GameRules.missionXp(type),
                requiresPhoto = requiresPhoto
            )
        )
    }

    /** Text and photo requirement can change; attribute and type stay, so earlier completions keep their meaning. */
    suspend fun updateMissionDetails(id: Long, title: String, description: String?, requiresPhoto: Boolean) =
        missionDao.updateDetails(
            id,
            MissionRules.cleanTitle(title),
            description?.let { MissionRules.cleanDescription(it) },
            requiresPhoto
        )

    suspend fun deleteMission(id: Long) = missionDao.deactivate(id)

    suspend fun seedIfEmpty() {
        attributeDao.insertAllIfMissing(Attribute.entries.map { AttributeStateEntity(it) })
        if (missionDao.count() == 0) missionDao.insertAll(defaultMissions())
    }

    /** Completes the mission for the current period and returns the items earned by it (usually none). */
    suspend fun completeMission(mission: MissionEntity): CompletionResult = db.withTransaction {
        val today = LocalDate.now().toEpochDay()
        val range = periodRange(mission.type, today)
        if (completionDao.findForMission(mission.id, range.first, range.last) != null) {
            return@withTransaction CompletionResult(emptyList(), emptyList())
        }
        // Read before inserting: the last day with a mission before today, to detect a return after days away.
        val previousDay = completionDao.latestDayBefore(today)
        completionDao.insert(
            MissionCompletionEntity(
                missionId = mission.id,
                attribute = mission.attribute,
                xpEarned = mission.xpReward,
                completedAtMillis = System.currentTimeMillis(),
                dayEpoch = today
            )
        )
        val state = attributeDao.get(mission.attribute) ?: AttributeStateEntity(mission.attribute)
        attributeDao.upsert(
            state.copy(xp = state.xp + mission.xpReward, lastCompletedDayEpoch = today)
        )
        val items = grantRewards(mission)
        val achievements = evaluateAchievements(
            perfectDay = isPerfectDay(today),
            returnedAfterAbsence = previousDay != null &&
                    today - previousDay >= AchievementRules.ABSENCE_DAYS
        )
        CompletionResult(items, achievements)
    }

    /** Looks for achievements the player's history already earns (also unlocks past ones after an update). */
    suspend fun refreshAchievements(): List<AchievementUnlock> = db.withTransaction { evaluateAchievements() }

    fun observeAchievements(): Flow<List<AchievementEntity>> = achievementDao.observeAll()

    /** Debug tool: unlocks every achievement and gives their prizes, to see the screens without waiting weeks. */
    suspend fun debugUnlockAllAchievements(): List<AchievementUnlock> = db.withTransaction {
        val unlocked = achievementDao.unlockedIds().toSet()
        Achievement.entries.filter { it.name !in unlocked }.map { unlock(it) }
    }

    /** All the regular (default and user) daily missions are done today, and there are at least three of them. */
    private suspend fun isPerfectDay(today: Long): Boolean {
        val regular = missionDao.regularDailyIds()
        if (regular.size < AchievementRules.MIN_DAILY_FOR_PERFECT_DAY) return false
        val done = completionDao.missionIdsOn(today).toSet()
        return regular.all { it in done }
    }

    /**
     * Unlocks what [stats] earn, gives the prizes and repeats once more: a prize can itself complete another
     * achievement (the collector one counts items). Nothing is ever taken back.
     */
    private suspend fun evaluateAchievements(
        perfectDay: Boolean = false,
        returnedAfterAbsence: Boolean = false
    ): List<AchievementUnlock> {
        val result = mutableListOf<AchievementUnlock>()
        repeat(MAX_ACHIEVEMENT_PASSES) {
            val unlocked = achievementDao.unlockedIds()
                .mapNotNull { id -> Achievement.entries.firstOrNull { it.name == id } }
                .toSet()
            val earned = AchievementRules.newlyEarned(
                stats = achievementStats(perfectDay, returnedAfterAbsence),
                alreadyUnlocked = unlocked
            )
            if (earned.isEmpty()) return result
            earned.forEach { result += unlock(it) }
        }
        return result
    }

    private suspend fun achievementStats(perfectDay: Boolean, returned: Boolean): AchievementStats {
        val states = attributeDao.getAll()
        return AchievementStats(
            totalCompletions = completionDao.count(),
            weeklyCompletions = completionDao.weeklyCount(),
            bestStreak = StreakRules.longestStreak(completionDao.allDays().toSet()),
            totalLevel = GameRules.levelFor(states.sumOf { it.xp }).level,
            attributeLevels = Attribute.entries.associateWith { attribute ->
                GameRules.levelFor(states.firstOrNull { it.attribute == attribute }?.xp ?: 0).level
            },
            itemCount = inventoryDao.count(),
            perfectDay = perfectDay,
            returnedAfterAbsence = returned
        )
    }

    private suspend fun unlock(achievement: Achievement): AchievementUnlock {
        val now = System.currentTimeMillis()
        achievementDao.insertIfMissing(AchievementEntity(achievement.name, now))
        val item = achievement.rewardItemId?.let { ItemCatalog.byId(it) }?.takeIf { prize ->
            inventoryDao.insertIfMissing(
                InventoryItemEntity(itemId = prize.id, acquiredAtMillis = now, source = ItemSource.ACHIEVEMENT)
            ) != -1L
        }
        return AchievementUnlock(achievement, item)
    }

    /** Debug tool: adds XP to an attribute without completing a mission, and grants any items now due. */
    suspend fun debugAddXp(attribute: Attribute, amount: Int): List<CatalogItem> = db.withTransaction {
        val state = attributeDao.get(attribute) ?: AttributeStateEntity(attribute)
        attributeDao.upsert(state.copy(xp = state.xp + amount))
        grantRewards(mission = null)
    }

    /** Debug tool: puts the whole catalog in the inventory, to check every sprite on the avatar. */
    suspend fun debugGrantAllItems(): Int = db.withTransaction {
        val owned = inventoryDao.ownedIds().toSet()
        val now = System.currentTimeMillis()
        ItemCatalog.items.filter { it.id !in owned }.count { item ->
            inventoryDao.insertIfMissing(
                InventoryItemEntity(itemId = item.id, acquiredAtMillis = now, source = ItemSource.LEVEL)
            ) != -1L
        }
    }

    /**
     * Gives every item that is due and not owned yet. Items are never taken back, not even by undo.
     * Weekly items: at most one per attribute per week, so undo + redo cannot farm extra items.
     */
    private suspend fun grantRewards(mission: MissionEntity?): List<CatalogItem> {
        val states = attributeDao.getAll()
        val attributeLevels = Attribute.entries.associateWith { attribute ->
            GameRules.levelFor(states.firstOrNull { it.attribute == attribute }?.xp ?: 0).level
        }
        val totalLevel = GameRules.levelFor(states.sumOf { it.xp }).level
        val owned = inventoryDao.ownedIds().toMutableSet()
        val granted = mutableListOf<CatalogItem>()

        suspend fun give(item: CatalogItem?, source: ItemSource) {
            if (item == null || item.id in owned) return
            val entity = InventoryItemEntity(
                itemId = item.id,
                acquiredAtMillis = System.currentTimeMillis(),
                source = source
            )
            if (inventoryDao.insertIfMissing(entity) != -1L) {
                owned += item.id
                granted += item
            }
        }

        if (mission != null) {
            give(RewardRules.rewardMissionItem(mission.rewardItemId, owned), ItemSource.REWARD_MISSION)
            if (mission.type == MissionType.WEEKLY && !weeklyItemGivenThisWeek(mission.attribute)) {
                val level = attributeLevels.getValue(mission.attribute)
                give(RewardRules.nextWeeklyItem(mission.attribute, level, owned), ItemSource.WEEKLY)
            }
        }
        RewardRules.levelItemsDue(totalLevel, attributeLevels, owned)
            .forEach { give(it, ItemSource.LEVEL) }
        return granted
    }

    private suspend fun weeklyItemGivenThisWeek(attribute: Attribute): Boolean {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val zone = ZoneId.systemDefault()
        val from = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = monday.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        return inventoryDao.getBySourceBetween(ItemSource.WEEKLY, from, to)
            .any { ItemCatalog.byId(it.itemId)?.attribute == attribute }
    }

    /** Undo for accidental taps. */
    suspend fun undoMission(mission: MissionEntity) = db.withTransaction {
        val today = LocalDate.now().toEpochDay()
        val range = periodRange(mission.type, today)
        val completion = completionDao.findForMission(mission.id, range.first, range.last)
            ?: return@withTransaction
        completionDao.deleteById(completion.id)
        val state = attributeDao.get(mission.attribute) ?: return@withTransaction
        attributeDao.upsert(
            state.copy(
                xp = (state.xp - completion.xpEarned).coerceAtLeast(0),
                lastCompletedDayEpoch = completionDao.latestDayFor(mission.attribute)
            )
        )
    }

    /** Weeks start on Monday. */
    private fun periodRange(type: MissionType, todayEpoch: Long): LongRange =
        when (type) {
            MissionType.DAILY -> todayEpoch..todayEpoch
            MissionType.WEEKLY -> {
                val today = LocalDate.ofEpochDay(todayEpoch)
                val monday = todayEpoch - (today.dayOfWeek.value - 1)
                monday..(monday + 6)
            }
        }

    private fun defaultMissions() = listOf(
        MissionEntity(title = "Tender la cama", attribute = Attribute.DISCIPLINE,
            type = MissionType.DAILY, xpReward = GameRules.missionXp(MissionType.DAILY), requiresPhoto = true),
        MissionEntity(title = "15 minutos de ejercicio", attribute = Attribute.BODY,
            type = MissionType.DAILY, xpReward = GameRules.missionXp(MissionType.DAILY)),
        MissionEntity(title = "Leer 30 minutos", attribute = Attribute.MIND,
            type = MissionType.DAILY, xpReward = GameRules.missionXp(MissionType.DAILY)),
        MissionEntity(title = "Una hora sin pantallas", attribute = Attribute.MIND,
            type = MissionType.DAILY, xpReward = GameRules.missionXp(MissionType.DAILY)),
        MissionEntity(title = "Escribirle a alguien que quieres", attribute = Attribute.SOCIAL,
            type = MissionType.DAILY, xpReward = GameRules.missionXp(MissionType.DAILY)),
        MissionEntity(title = "Salir a caminar 3 veces", attribute = Attribute.BODY,
            type = MissionType.WEEKLY, xpReward = GameRules.missionXp(MissionType.WEEKLY)),
        MissionEntity(title = "Leer 2 horas en total", attribute = Attribute.MIND,
            type = MissionType.WEEKLY, xpReward = GameRules.missionXp(MissionType.WEEKLY)),
        MissionEntity(title = "Quedar con un amigo o familiar", attribute = Attribute.SOCIAL,
            type = MissionType.WEEKLY, xpReward = GameRules.missionXp(MissionType.WEEKLY)),
        MissionEntity(title = "Ordenar tu cuarto a fondo", attribute = Attribute.DISCIPLINE,
            type = MissionType.WEEKLY, xpReward = GameRules.missionXp(MissionType.WEEKLY))
    )
}