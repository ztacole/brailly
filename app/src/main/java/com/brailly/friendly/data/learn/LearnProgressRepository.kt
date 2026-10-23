package com.brailly.friendly.data.learn

import kotlinx.coroutines.flow.Flow

/**
 * Repository progres belajar: menjembatani [LevelResultDao] dengan lapisan UI
 * (ViewModel). Menyimpan dan membaca hasil kuis per level.
 */
class LearnProgressRepository(
    private val dao: LevelResultDao,
) {

    /** Aliran seluruh hasil level yang tersimpan. */
    fun observeResults(): Flow<List<LevelResultEntity>> = dao.observeAll()

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
}
