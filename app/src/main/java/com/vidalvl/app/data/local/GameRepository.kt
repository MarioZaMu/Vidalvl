package com.vidalvl.app.data.local

import androidx.room.withTransaction
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.MissionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class GameRepository(private val db: VidalvlDatabase) {
    private val missionDao = db.missionDao()
    private val completionDao = db.completionDao()
    private val attributeDao = db.attributeDao()

    fun observeMissions(type: MissionType): Flow<List<MissionEntity>> =
        missionDao.observeActive(type)

    fun observeAttributes(): Flow<List<AttributeStateEntity>> = attributeDao.observeAll()

    /** Completions inside the current period (today for daily, this week for weekly). */
    fun observeCompletions(type: MissionType): Flow<List<MissionCompletionEntity>> {
        val range = periodRange(type, LocalDate.now().toEpochDay())
        return completionDao.observeBetween(range.first, range.last)
    }

    suspend fun seedIfEmpty() {
        attributeDao.insertAllIfMissing(Attribute.entries.map { AttributeStateEntity(it) })
        if (missionDao.count() == 0) missionDao.insertAll(defaultMissions())
    }

    suspend fun completeMission(mission: MissionEntity) = db.withTransaction {
        val today = LocalDate.now().toEpochDay()
        val range = periodRange(mission.type, today)
        if (completionDao.findForMission(mission.id, range.first, range.last) != null) {
            return@withTransaction
        }
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
            type = MissionType.DAILY, xpReward = 25, requiresPhoto = true),
        MissionEntity(title = "15 minutos de ejercicio", attribute = Attribute.BODY,
            type = MissionType.DAILY, xpReward = 25),
        MissionEntity(title = "Leer 30 minutos", attribute = Attribute.MIND,
            type = MissionType.DAILY, xpReward = 25),
        MissionEntity(title = "Una hora sin pantallas", attribute = Attribute.MIND,
            type = MissionType.DAILY, xpReward = 25),
        MissionEntity(title = "Escribirle a alguien que quieres", attribute = Attribute.SOCIAL,
            type = MissionType.DAILY, xpReward = 25),
        MissionEntity(title = "Salir a caminar 3 veces", attribute = Attribute.BODY,
            type = MissionType.WEEKLY, xpReward = 100),
        MissionEntity(title = "Leer 2 horas en total", attribute = Attribute.MIND,
            type = MissionType.WEEKLY, xpReward = 100),
        MissionEntity(title = "Quedar con un amigo o familiar", attribute = Attribute.SOCIAL,
            type = MissionType.WEEKLY, xpReward = 100),
        MissionEntity(title = "Ordenar tu cuarto a fondo", attribute = Attribute.DISCIPLINE,
            type = MissionType.WEEKLY, xpReward = 100)
    )
}