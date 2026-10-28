package com.brailly.friendly.data.learn

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Basis data Room aplikasi: hasil kuis per level dan progres menonton materi
 * video pada Jalur Kuis.
 *
 * Versi 2 hanya menambahkan tabel `video_progress`, sehingga migrasinya bisa
 * dibangkitkan otomatis oleh Room dari skema yang diekspor ke `app/schemas`.
 * Tabel `level_result` tidak berubah, jadi progres kuis pengguna lama tetap
 * utuh.
 */
@Database(
    entities = [LevelResultEntity::class, VideoProgressEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class BraillyDatabase : RoomDatabase() {
    abstract fun levelResultDao(): LevelResultDao
    abstract fun videoProgressDao(): VideoProgressDao

    companion object {
        const val NAME = "brailly.db"
    }
}
