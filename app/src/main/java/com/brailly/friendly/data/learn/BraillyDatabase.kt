package com.brailly.friendly.data.learn

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.brailly.friendly.data.scan.ScanHistoryDao
import com.brailly.friendly.data.scan.ScanHistoryEntity

/**
 * Basis data Room aplikasi: hasil kuis per level, progres menonton materi
 * video pada Jalur Kuis, dan riwayat pemindaian braille.
 *
 * Versi 2 menambahkan tabel `video_progress`, versi 3 menambahkan tabel
 * `scan_history`. Keduanya hanya menambah tabel tanpa mengubah yang lama,
 * sehingga migrasinya bisa dibangkitkan otomatis oleh Room dari skema yang
 * diekspor ke `app/schemas`, dan progres pengguna lama tetap utuh.
 *
 * Kelasnya tetap berada di paket `data.learn` meski kini juga menampung tabel
 * milik `data.scan`. Room menamai folder skema yang diekspor memakai nama
 * kelas yang lengkap dengan paketnya, jadi memindahkannya akan memutus
 * rangkaian skema `1.json`..`3.json` yang menjadi dasar migrasi otomatis.
 */
@Database(
    entities = [
        LevelResultEntity::class,
        VideoProgressEntity::class,
        ScanHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class BraillyDatabase : RoomDatabase() {
    abstract fun levelResultDao(): LevelResultDao
    abstract fun videoProgressDao(): VideoProgressDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        const val NAME = "brailly.db"
    }
}
