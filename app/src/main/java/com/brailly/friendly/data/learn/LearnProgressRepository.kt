package com.brailly.friendly.data.learn

import kotlinx.coroutines.flow.Flow

/**
 * Repository progres belajar: menjembatani [LevelResultDao] dan
 * [VideoProgressDao] dengan lapisan UI (ViewModel). Menyimpan dan membaca
 * hasil kuis per level serta progres menonton materi video.
 */
class LearnProgressRepository(
    private val dao: LevelResultDao,
    private val videoDao: VideoProgressDao,
) {

    /** Aliran seluruh hasil level yang tersimpan. */
    fun observeResults(): Flow<List<LevelResultEntity>> = dao.observeAll()

    /** Aliran progres menonton seluruh materi video. */
    fun observeVideoProgress(): Flow<List<VideoProgressEntity>> = videoDao.observeAll()

    /**
     * Catat hasil penyelesaian sebuah level. Hanya memperbarui bila skor baru
     * lebih baik dari rekor sebelumnya; status lulus bersifat "pernah lulus"
     * (sekali lulus tetap lulus).
     */
    suspend fun recordResult(
        levelIndex: Int,
        score: Int,
        totalQuestions: Int,
        passed: Boolean,
    ) {
        val existing = dao.getByIndex(levelIndex)
        val bestScore = maxOf(existing?.bestScore ?: 0, score)
        val everPassed = (existing?.passed ?: false) || passed
        dao.upsert(
            LevelResultEntity(
                levelIndex = levelIndex,
                bestScore = bestScore,
                totalQuestions = totalQuestions,
                passed = everPassed,
                lastPlayedAt = System.currentTimeMillis(),
            )
        )
    }

    /**
     * Catat progres menonton satu materi video.
     *
     * Durasi tertonton hanya boleh bertambah, dan status tuntas bersifat
     * "pernah tuntas". Keduanya disengaja: menonton ulang sebagian video tidak
     * boleh menurunkan progres atau mengunci kembali level yang sudah terbuka.
     */
    suspend fun recordVideoProgress(
        videoId: String,
        watchedSeconds: Int,
        durationSeconds: Int,
        completed: Boolean,
    ) {
        val existing = videoDao.getById(videoId)
        videoDao.upsert(
            VideoProgressEntity(
                videoId = videoId,
                watchedSeconds = maxOf(existing?.watchedSeconds ?: 0, watchedSeconds),
                // Durasi dari pemutar baru tersedia setelah video siap; jangan
                // timpa nilai yang sudah valid dengan 0.
                durationSeconds = if (durationSeconds > 0) durationSeconds
                else existing?.durationSeconds ?: 0,
                completed = (existing?.completed ?: false) || completed,
                lastWatchedAt = System.currentTimeMillis(),
            )
        )
    }
}
