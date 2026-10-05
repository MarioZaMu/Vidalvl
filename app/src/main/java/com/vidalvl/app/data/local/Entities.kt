package com.vidalvl.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.Gender
import com.vidalvl.app.domain.model.ItemSlot
import com.vidalvl.app.domain.model.ItemSource
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
    val isActive: Boolean = true,
    /** Catalog item given by this mission (special "reward" missions only). */
    val rewardItemId: String? = null
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

/** The user's avatar look. A single row (id = 1); no row means the avatar is not created yet. */
@Entity(tableName = "avatar_profile")
data class AvatarProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ID,
    val gender: Gender,
    val skinTone: Int,
    val hairStyle: Int,
    /** ARGB int. */
    val hairColor: Int,
    @ColumnInfo(defaultValue = "1") val eyeStyle: Int = 1,
    @ColumnInfo(defaultValue = "''") val name: String = ""
) {
    companion object {
        const val SINGLE_ID = 1
    }
}

/** Items the user owns. Item ids come from the Kotlin catalog; each one can be owned once. */
@Entity(
    tableName = "inventory",
    indices = [Index(value = ["itemId"], unique = true)]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val acquiredAtMillis: Long,
    val source: ItemSource
)

/** What is currently worn: one item per slot. */
@Entity(tableName = "equipped")
data class EquippedEntity(
    @PrimaryKey val slot: ItemSlot,
    val itemId: String
)