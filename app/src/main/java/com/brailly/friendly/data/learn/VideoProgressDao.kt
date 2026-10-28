package com.brailly.friendly.data.learn

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Akses data progres menonton materi video. */
@Dao
interface VideoProgressDao {

    /** Alirkan progres seluruh materi video yang pernah diputar. */
    @Query("SELECT * FROM video_progress")
    fun observeAll(): Flow<List<VideoProgressEntity>>

    /** Ambil progres satu materi (null bila belum pernah diputar). */
    @Query("SELECT * FROM video_progress WHERE videoId = :videoId LIMIT 1")
    suspend fun getById(videoId: String): VideoProgressEntity?

    /** Sisipkan atau perbarui progres satu materi. */
    @Upsert
    suspend fun upsert(progress: VideoProgressEntity)
}
