package com.brailly.friendly.data.scan

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Akses data riwayat pemindaian. */
@Dao
interface ScanHistoryDao {

    /**
     * Sumber halaman untuk Paging 3, terurut dari pemindaian terbaru.
     *
     * Urutan kedua memakai `id` sebagai pemecah seri. Dua pemindaian pada
     * milidetik yang sama akan punya `scannedAt` identik, dan tanpa pemecah
     * seri SQLite bebas menentukan urutannya per permintaan halaman, sehingga
     * baris yang sama bisa muncul dua kali atau terlewat saat menggulir.
     */
    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC, id DESC")
    fun pagingSource(): PagingSource<Int, ScanHistoryEntity>

    /** Jumlah entri, dipakai menampilkan ringkasan dan keadaan kosong. */
    @Query("SELECT COUNT(*) FROM scan_history")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(entry: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clear()
}
