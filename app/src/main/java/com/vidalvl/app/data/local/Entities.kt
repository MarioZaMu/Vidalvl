package com.vidalvl.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.MissionType

/** A mission the user can complete (daily or weekly). */
@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val attribute: Attribute,
    val type: MissionType,
    val xpReward: Int,
    val requiresPhoto: Boolean = false,
    val isActive: Boolean = true
)

/** History of completed missions. Feeds streaks, the reflection timeline and AI suggestions. */
@Entity(
    tableName = "mission_completions",
    foreignKeys = [
        ForeignKey(
            entity = MissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("missionId"), Index("dayEpoch")]
)
data class MissionCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: Long,
    val attribute: Attribute,
    val xpEarned: Int,
    val completedAtMillis: Long,
    /** Local date as epoch day (days since 1970-01-01). */
    val dayEpoch: Long
)

/** Accumulated progress of one attribute. Level and glow are derived from this. */
@Entity(tableName = "attribute_states")
data class AttributeStateEntity(
    @PrimaryKey val attribute: Attribute,
    val xp: Int = 0,
    val lastCompletedDayEpoch: Long? = null
)