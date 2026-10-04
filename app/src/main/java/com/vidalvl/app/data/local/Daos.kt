package com.vidalvl.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vidalvl.app.domain.model.Attribute
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfMissing(states: List<AttributeStateEntity>)
}