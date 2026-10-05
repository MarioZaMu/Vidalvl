package com.vidalvl.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MissionEntity::class,
        MissionCompletionEntity::class,
        AttributeStateEntity::class,
        AvatarProfileEntity::class,
        InventoryItemEntity::class,
        EquippedEntity::class,
        PlayerGoalsEntity::class,
        AchievementEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class VidalvlDatabase : RoomDatabase() {
    abstract fun missionDao(): MissionDao
    abstract fun completionDao(): CompletionDao
    abstract fun attributeDao(): AttributeDao
    abstract fun avatarDao(): AvatarDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun equippedDao(): EquippedDao
    abstract fun goalsDao(): GoalsDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var instance: VidalvlDatabase? = null

        /** v1 -> v2: avatar, inventory, equipped items and the mission reward column. Keeps all data. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `avatar_profile` (" +
                            "`id` INTEGER NOT NULL, `gender` TEXT NOT NULL, `skinTone` INTEGER NOT NULL, " +
                            "`hairStyle` INTEGER NOT NULL, `hairColor` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inventory` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemId` TEXT NOT NULL, " +
                            "`acquiredAtMillis` INTEGER NOT NULL, `source` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_itemId` ON `inventory` (`itemId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `equipped` (" +
                            "`slot` TEXT NOT NULL, `itemId` TEXT NOT NULL, PRIMARY KEY(`slot`))"
                )
                db.execSQL("ALTER TABLE `missions` ADD COLUMN `rewardItemId` TEXT")
            }
        }

        /** v2 -> v3: data only. Smaller XP rewards for the existing missions; earned XP is untouched. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE `missions` SET `xpReward` = 8 WHERE `type` = 'DAILY' AND `xpReward` = 25")
                db.execSQL("UPDATE `missions` SET `xpReward` = 15 WHERE `type` = 'WEEKLY' AND `xpReward` = 100")
            }
        }

        /** v3 -> v4: eye style for the avatar. Existing avatars get style 1. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `avatar_profile` ADD COLUMN `eyeStyle` INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** v4 -> v5: player name. Existing avatars start with a blank name. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `avatar_profile` ADD COLUMN `name` TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v5 -> v6: goals chosen on the welcome screen. Existing users see the welcome once. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `player_goals` (" +
                            "`id` INTEGER NOT NULL, `goals` TEXT NOT NULL, `dailyMinutes` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
            }
        }

        /** v6 -> v7: marks suggested missions with the period they were made for. Existing missions stay as they are. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `missions` ADD COLUMN `generatedFor` INTEGER")
            }
        }

        /** v7 -> v8: unlocked achievements. Existing users unlock theirs from their history the next time the app opens. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `achievements` (" +
                            "`id` TEXT NOT NULL, `unlockedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        fun getInstance(context: Context): VidalvlDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VidalvlDatabase::class.java,
                    "vidalvl.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build().also { instance = it }
            }
    }
}