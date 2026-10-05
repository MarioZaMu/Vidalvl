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
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY acquiredAtMillis")
    fun observeAll(): Flow<List<InventoryItemEntity>>

    @Query("SELECT itemId FROM inventory")
    suspend fun ownedIds(): List<String>

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