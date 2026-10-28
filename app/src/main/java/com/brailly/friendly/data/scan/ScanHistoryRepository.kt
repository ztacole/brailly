package com.brailly.friendly.data.scan

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * Repository riwayat pemindaian: menjembatani [ScanHistoryDao] dengan
 * ViewModel.
 *
 * Riwayat tidak dipangkas otomatis. Memotong entri lama memang menjaga ukuran
 * basis data, tetapi yang terhapus adalah hasil kerja pengguna, dan tidak ada
 * cara mengembalikannya. Penghapusan diserahkan kepada pengguna lewat tombol
 * hapus per entri dan "Hapus Semua" di layar riwayat.
 */
class ScanHistoryRepository(
    private val dao: ScanHistoryDao,
) {

    /**
     * Aliran riwayat yang dimuat sepotong demi sepotong.
     *
     * Dipakai Paging 3 alih-alih satu `Flow<List<..>>` karena setiap entri
     * memuat teks utuh satu halaman braille. Setelah beberapa puluh
     * pemindaian, memuat semuanya sekaligus berarti menahan seluruh teks di
     * memori hanya untuk menampilkan beberapa baris pertama.
     */
    fun pagedEntries(): Flow<PagingData<ScanHistoryEntity>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            // Entri riwayat tingginya tidak seragam, jadi penyangga ditetapkan
            // dalam jumlah baris, bukan dibiarkan mengikuti ukuran viewport.
            initialLoadSize = PAGE_SIZE * 2,
            prefetchDistance = PAGE_SIZE / 2,
            // Baris kosong sebagai penampung akan dibacakan pembaca layar
            // sebagai entri yang ada tetapi tak berisi apa-apa.
            enablePlaceholders = false,
        ),
        pagingSourceFactory = dao::pagingSource,
    ).flow

    /** Jumlah entri riwayat saat ini. */
    fun observeCount(): Flow<Int> = dao.observeCount()

    /**
     * Catat satu hasil pemindaian.
     *
     * Hasil tanpa sel terbaca tidak disimpan: entri kosong tidak bisa dibaca,
     * disalin, maupun dibagikan, dan hanya memenuhi daftar.
     *
     * @return id entri baru, atau null bila hasilnya tidak layak disimpan.
     */
    suspend fun record(
        text: String,
        confidence: Float,
        cellCount: Int,
        paragraphCount: Int,
        doubtful: Boolean,
    ): Long? {
        if (cellCount == 0 || text.isBlank()) return null
        return dao.insert(
            ScanHistoryEntity(
                text = text,
                confidence = confidence,
                cellCount = cellCount,
                paragraphCount = paragraphCount,
                doubtful = doubtful,
                scannedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun clear() = dao.clear()

    private companion object {
        /**
         * Satu halaman memuat lebih banyak entri daripada yang tampak di
         * layar, supaya gulir pendek tidak langsung memicu permintaan baru.
         */
        const val PAGE_SIZE = 20
    }
}
