package com.brailly.friendly.data.learn

import androidx.room.Database
import androidx.room.RoomDatabase

/** Basis data Room aplikasi. Saat ini hanya menyimpan hasil kuis Belajar. */
@Database(
    entities = [LevelResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class BraillyDatabase : RoomDatabase() {
    abstract fun levelResultDao(): LevelResultDao

    companion object {
        const val NAME = "brailly.db"
    }
}
