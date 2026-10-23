package com.brailly.friendly.data.learn

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Hasil terbaik satu level kuis yang dipersistenkan ke Room.
 *
 * Satu baris mewakili satu level (dikunci oleh [levelIndex] 0-based). Nilai yang
 * disimpan adalah pencapaian terbaik: skor tertinggi dan status lulus. Baris
 * hanya dibuat/diperbarui setelah pengguna menyelesaikan sebuah level.
 *
 * @param levelIndex indeks level 0-based, sekaligus primary key.
 * @param bestScore skor terbaik (0..[totalQuestions]).
 * @param totalQuestions jumlah soal pada level tersebut.
 * @param passed true bila level pernah diselesaikan dengan skor >= ambang lulus.
 * @param lastPlayedAt epoch millis terakhir kali level dimainkan.
 */
@Entity(tableName = "level_result")
data class LevelResultEntity(
    @PrimaryKey val levelIndex: Int,
    val bestScore: Int,
    val totalQuestions: Int,
    val passed: Boolean,
    val lastPlayedAt: Long,
)
