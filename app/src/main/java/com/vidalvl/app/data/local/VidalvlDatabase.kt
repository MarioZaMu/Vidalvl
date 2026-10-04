package com.vidalvl.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MissionEntity::class,
        MissionCompletionEntity::class,
        AttributeStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VidalvlDatabase : RoomDatabase() {
    abstract fun missionDao(): MissionDao
    abstract fun completionDao(): CompletionDao
    abstract fun attributeDao(): AttributeDao

    companion object {
        @Volatile
        private var instance: VidalvlDatabase? = null

        fun getInstance(context: Context): VidalvlDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VidalvlDatabase::class.java,
                    "vidalvl.db"
                ).build().also { instance = it }
            }
    }
}