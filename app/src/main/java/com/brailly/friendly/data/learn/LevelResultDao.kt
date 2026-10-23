package com.brailly.friendly.data.learn

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Akses data hasil kuis per level. */
@Dao
interface LevelResultDao {

    /** Alirkan semua hasil level, diurutkan berdasarkan indeks level. */
    @Query("SELECT * FROM level_result ORDER BY levelIndex ASC")
    fun observeAll(): Flow<List<LevelResultEntity>>

    /** Ambil hasil satu level (null bila belum pernah dimainkan). */
    @Query("SELECT * FROM level_result WHERE levelIndex = :levelIndex LIMIT 1")
    suspend fun getByIndex(levelIndex: Int): LevelResultEntity?

    /** Sisipkan atau perbarui hasil satu level. */
    @Upsert
    suspend fun upsert(result: LevelResultEntity)
}
