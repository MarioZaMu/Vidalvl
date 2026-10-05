package com.vidalvl.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.ItemSlot
import com.vidalvl.app.domain.model.ItemSource
import com.vidalvl.app.domain.model.MissionType
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions WHERE isActive = 1 AND type = :type ORDER BY id")
    fun observeActive(type: MissionType): Flow<List<MissionEntity>>

    @Query("SELECT COUNT(*) FROM missions")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(missions: List<MissionEntity>)

    @Insert
    suspend fun insert(mission: MissionEntity): Long

    @Query("UPDATE missions SET title = :title, description = :description, requiresPhoto = :requiresPhoto WHERE id = :id")
    suspend fun updateDetails(id: Long, title: String, description: String?, requiresPhoto: Boolean)

    /** Soft delete: the mission disappears from the lists but its history and XP stay. */
    @Query("UPDATE missions SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("SELECT title FROM missions WHERE isActive = 1")
    suspend fun activeTitles(): List<String>

    /** Daily missions that count for a "perfect day": the default and the user's own, not the optional suggested ones. */
    @Query("SELECT id FROM missions WHERE isActive = 1 AND type = 'DAILY' AND generatedFor IS NULL")
    suspend fun regularDailyIds(): List<Long>

    /** Hides suggested missions whose day (daily) or week (weekly) is over. */
    @Query(
        "UPDATE missions SET isActive = 0 WHERE isActive = 1 AND generatedFor IS NOT NULL AND " +
                "((type = 'DAILY' AND generatedFor < :today) OR (type = 'WEEKLY' AND generatedFor < :monday))"
    )
    suspend fun expireSuggestions(today: Long, monday: Long)
}

@Dao
interface CompletionDao {
    @Insert
    suspend fun insert(completion: MissionCompletionEntity): Long

    @Query("DELETE FROM mission_completions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM mission_completions WHERE dayEpoch BETWEEN :fromDay AND :toDay")
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<MissionCompletionEntity>>

    @Query(
        "SELECT * FROM mission_completions " +
                "WHERE missionId = :missionId AND dayEpoch BETWEEN :fromDay AND :toDay LIMIT 1"
    )
    suspend fun findForMission(missionId: Long, fromDay: Long, toDay: Long): MissionCompletionEntity?

    @Query("SELECT MAX(dayEpoch) FROM mission_completions WHERE attribute = :attribute")
    suspend fun latestDayFor(attribute: Attribute): Long?

    @Query("SELECT * FROM mission_completions ORDER BY completedAtMillis DESC")
    fun observeAll(): Flow<List<MissionCompletionEntity>>

    @Query("SELECT DISTINCT dayEpoch FROM mission_completions")
    suspend fun allDays(): List<Long>

    @Query("SELECT COUNT(*) FROM mission_completions")
    suspend fun count(): Int

    @Query(
        "SELECT COUNT(*) FROM mission_completions c JOIN missions m ON m.id = c.missionId " +
                "WHERE m.type = 'WEEKLY'"
    )
    suspend fun weeklyCount(): Int

    @Query("SELECT MAX(dayEpoch) FROM mission_completions WHERE dayEpoch < :day")
    suspend fun latestDayBefore(day: Long): Long?

    @Query("SELECT DISTINCT missionId FROM mission_completions WHERE dayEpoch = :day")
    suspend fun missionIdsOn(day: Long): List<Long>

    @Query(
        "SELECT DISTINCT m.title FROM mission_completions c JOIN missions m ON m.id = c.missionId " +
                "WHERE c.dayEpoch >= :fromDay"
    )
    suspend fun titlesSince(fromDay: Long): List<String>
}

@Dao
interface AttributeDao {
    @Query("SELECT * FROM attribute_states")
    fun observeAll(): Flow<List<AttributeStateEntity>>

    @Query("SELECT * FROM attribute_states WHERE attribute = :attribute")
    suspend fun get(attribute: Attribute): AttributeStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: AttributeStateEntity)

    @Query("SELECT * FROM attribute_states")
    suspend fun getAll(): List<AttributeStateEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfMissing(states: List<AttributeStateEntity>)
}

@Dao
interface AvatarDao {
    @Query("SELECT * FROM avatar_profile WHERE id = 1")
    fun observe(): Flow<AvatarProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: AvatarProfileEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT id FROM achievements")
    suspend fun unlockedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(achievement: AchievementEntity): Long
}

@Dao
interface GoalsDao {
    @Query("SELECT * FROM player_goals WHERE id = 1")
    fun observe(): Flow<PlayerGoalsEntity?>

    @Query("SELECT * FROM player_goals WHERE id = 1")
    suspend fun get(): PlayerGoalsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goals: PlayerGoalsEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY acquiredAtMillis")
    fun observeAll(): Flow<List<InventoryItemEntity>>

    @Query("SELECT itemId FROM inventory")
    suspend fun ownedIds(): List<String>

    @Query("SELECT COUNT(*) FROM inventory")
    suspend fun count(): Int

    @Query("SELECT * FROM inventory WHERE source = :source AND acquiredAtMillis >= :fromMillis AND acquiredAtMillis < :toMillis")
    suspend fun getBySourceBetween(source: ItemSource, fromMillis: Long, toMillis: Long): List<InventoryItemEntity>

    /** Returns -1 when the item was already owned. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(item: InventoryItemEntity): Long
}

@Dao
interface EquippedDao {
    @Query("SELECT * FROM equipped")
    fun observeAll(): Flow<List<EquippedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(equipped: EquippedEntity)

    @Query("DELETE FROM equipped WHERE slot = :slot")
    suspend fun clear(slot: ItemSlot)
}