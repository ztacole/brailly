package com.brailly.friendly.data.learn

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Progres menonton satu materi video pembelajaran.
 *
 * Kuncinya adalah [videoId], bukan urutan materi, supaya mengganti atau
 * menyusun ulang daftar materi di `VideoLessonCatalog` tidak menggeser progres
 * materi lain. Konsekuensinya memang disengaja: menukar `videoId` sebuah materi
 * membuat progres lamanya tidak terpakai, sehingga pengguna menonton ulang
 * materi yang isinya memang baru.
 *
 * [watchedSeconds] adalah akumulasi detik yang benar-benar diputar, bukan
 * posisi terakhir pemutar. Lompatan seek tidak ditambahkan ke nilai ini (lihat
 * `LearnViewModel.onVideoProgress`), sehingga menggeser bilah waktu ke akhir
 * video tidak menyelesaikan gerbang.
 *
 * @param videoId ID video YouTube, sekaligus primary key.
 * @param watchedSeconds total detik tertonton, maksimal sebesar durasi video.
 * @param durationSeconds durasi video menurut pemutar; 0 bila belum diketahui.
 * @param completed true bila materi pernah tuntas ditonton.
 * @param lastWatchedAt epoch millis terakhir kali materi diputar.
 */
@Entity(tableName = "video_progress")
data class VideoProgressEntity(
    @PrimaryKey val videoId: String,
    val watchedSeconds: Int,
    val durationSeconds: Int,
    val completed: Boolean,
    val lastWatchedAt: Long,
)
