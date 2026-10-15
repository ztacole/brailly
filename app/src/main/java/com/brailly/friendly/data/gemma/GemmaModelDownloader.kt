package com.brailly.friendly.data.gemma

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.readByteArray
import java.io.File
import java.io.FileOutputStream

/**
 * Status unduhan model Gemma on-device.
 */
sealed interface DownloadStatus {
    /** Model sudah tersedia; tidak perlu unduh. */
    data object AlreadyAvailable : DownloadStatus

    /** Sedang mengunduh. [bytesDownloaded] dari [totalBytes] (total -1 bila tak diketahui). */
    data class InProgress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadStatus {
        /** Fraksi 0f..1f, atau null bila total tak diketahui. */
        val fraction: Float?
            get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else null
    }

    /** Unduhan selesai; model siap dipakai. */
    data object Completed : DownloadStatus

    /**
     * Unduhan terhenti sebelum selesai (mis. koneksi terputus), tetapi progres
     * sudah tersimpan sehingga dapat dilanjutkan (resume).
     * [bytesDownloaded] = byte tersimpan, [totalBytes] = -1 bila tak diketahui.
     */
    data class Paused(val bytesDownloaded: Long, val totalBytes: Long, val message: String) : DownloadStatus

    /** Unduhan gagal (tanpa progres yang bisa dilanjutkan). */
    data class Failed(val message: String) : DownloadStatus
}

/**
 * Mengunduh model Gemma on-device (.task) ke penyimpanan internal aplikasi.
 *
 * Menulis ke file sementara (`.part`) lalu me-rename secara atomik agar file model
 * final tidak pernah dalam keadaan setengah terunduh (yang bisa membuat inisialisasi
 * MediaPipe gagal).
 *
 * **Resume:** bila terdapat file `.part` dari unduhan yang terputus, pengunduh
 * melanjutkan dengan HTTP Range request (`Range: bytes=<offset>-`). Bila server
 * merespons `206 Partial Content`, byte baru di-append. Bila server mengabaikan
 * Range dan merespons `200 OK`, unduhan dimulai ulang dari nol secara aman.
 *
 * Progress dipancarkan sebagai [Flow] agar dapat ditampilkan di UI.
 */
class GemmaModelDownloader(
    private val context: Context,
    private val http: HttpClient,
) {

    /** True bila model final sudah ada & tidak kosong. */
    fun isModelAvailable(): Boolean = GemmaConfig.isOnDeviceModelPresent(context)

    /** Hapus unduhan parsial yang tersimpan (mis. untuk memaksa mulai dari awal). */
    fun clearPartialDownload() {
        val tmpFile = File(context.filesDir, GemmaConfig.ON_DEVICE_MODEL_FILE + PART_SUFFIX)
        if (tmpFile.exists()) tmpFile.delete()
    }

    /**
     * Mulai atau lanjutkan unduhan. Aman dipanggil ulang: bila model sudah ada,
     * langsung memancarkan [DownloadStatus.AlreadyAvailable]. Bila ada file `.part`,
     * unduhan dilanjutkan dari byte terakhir.
     */
    fun download(): Flow<DownloadStatus> = flow {
        if (isModelAvailable()) {
            emit(DownloadStatus.AlreadyAvailable)
            return@flow
        }
        if (!GemmaConfig.isDownloadConfigured) {
            emit(DownloadStatus.Failed("URL unduhan model belum dikonfigurasi."))
            return@flow
        }

        val finalFile = File(context.filesDir, GemmaConfig.ON_DEVICE_MODEL_FILE)
        val tmpFile = File(context.filesDir, GemmaConfig.ON_DEVICE_MODEL_FILE + PART_SUFFIX)

        // Byte yang sudah terunduh sebelumnya (untuk resume).
        val existingBytes = if (tmpFile.exists()) tmpFile.length() else 0L

        try {
            http.prepareGet(GemmaConfig.MODEL_DOWNLOAD_URL) {
                if (GemmaConfig.MODEL_DOWNLOAD_AUTH_HEADER.isNotBlank()) {
                    header(HttpHeaders.Authorization, GemmaConfig.MODEL_DOWNLOAD_AUTH_HEADER)
                }
                // Minta resume bila sudah ada sebagian data.
                if (existingBytes > 0) {
                    header(HttpHeaders.Range, "bytes=$existingBytes-")
                }
            }.execute { response ->
                // Tentukan apakah server menghormati Range.
                val resumed = existingBytes > 0 &&
                    response.status == HttpStatusCode.PartialContent

                if (existingBytes > 0 && !resumed) {
                    // Server mengabaikan Range (200 OK) → mulai ulang dari nol.
                    Log.i(TAG, "Server tidak mendukung resume; mengulang dari awal.")
                }

                // Offset awal & total ukuran final.
                val startOffset = if (resumed) existingBytes else 0L
                val bodyLength = response.contentLength() // 206: sisa byte; 200: total
                val total = when {
                    bodyLength != null && resumed -> startOffset + bodyLength
                    bodyLength != null -> bodyLength
                    GemmaConfig.EXPECTED_MODEL_SIZE_BYTES > 0 -> GemmaConfig.EXPECTED_MODEL_SIZE_BYTES
                    else -> -1L
                }

                val channel = response.bodyAsChannel()
                var downloaded = startOffset
                emit(DownloadStatus.InProgress(downloaded, total))

                // append=true saat resume; false (truncate) saat mulai ulang.
                FileOutputStream(tmpFile, resumed).use { out ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_CHUNK.toLong())
                        while (!packet.exhausted()) {
                            val bytes = packet.readByteArray()
                            out.write(bytes)
                            downloaded += bytes.size
                            emit(DownloadStatus.InProgress(downloaded, total))
                        }
                    }
                    out.flush()
                }

                // Validasi ringan: file tidak boleh kosong.
                if (tmpFile.length() <= 0L) {
                    tmpFile.delete()
                    emit(DownloadStatus.Failed("File model kosong setelah unduhan."))
                    return@execute
                }

                // Bila total diketahui namun ukuran belum lengkap, pertahankan .part
                // agar bisa dilanjutkan di percobaan berikutnya.
                if (total > 0 && tmpFile.length() < total) {
                    emit(
                        DownloadStatus.Paused(
                            bytesDownloaded = tmpFile.length(),
                            totalBytes = total,
                            message = "Unduhan terhenti; dapat dilanjutkan.",
                        )
                    )
                    return@execute
                }

                // Rename atomik ke nama final.
                if (finalFile.exists()) finalFile.delete()
                if (tmpFile.renameTo(finalFile)) {
                    emit(DownloadStatus.Completed)
                } else {
                    tmpFile.delete()
                    emit(DownloadStatus.Failed("Gagal menyimpan model."))
                }
            }
        } catch (t: Throwable) {
            // Pertahankan file .part agar unduhan bisa dilanjutkan nanti.
            Log.w(TAG, "Unduhan model terputus/gagal: ${t.message}", t)
            val saved = if (tmpFile.exists()) tmpFile.length() else 0L
            if (saved > 0) {
                emit(
                    DownloadStatus.Paused(
                        bytesDownloaded = saved,
                        totalBytes = -1L,
                        message = t.message ?: "Unduhan terhenti; dapat dilanjutkan.",
                    )
                )
            } else {
                emit(DownloadStatus.Failed(t.message ?: "Unduhan gagal."))
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val TAG = "GemmaModelDownloader"
        const val DEFAULT_CHUNK = 64 * 1024
        const val PART_SUFFIX = ".part"
    }
}
