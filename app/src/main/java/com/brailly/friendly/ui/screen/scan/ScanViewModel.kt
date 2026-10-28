package com.brailly.friendly.ui.screen.scan

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brailly.friendly.data.scan.BrailleDetector
import com.brailly.friendly.core.scan.ReadingQuality
import com.brailly.friendly.data.scan.BrailleDetectorException
import com.brailly.friendly.data.scan.ScanHistoryRepository
import com.brailly.friendly.data.scan.ScannedLine
import com.brailly.friendly.data.scan.ScannedParagraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data object Processing : ScanState

    /**
     * Hasil pemindaian. [text] adalah bacaan apa adanya dari halaman, tanpa
     * perapian oleh model bahasa, agar isinya tetap setia pada aslinya.
     */
    data class Result(
        val text: String,
        val paragraphs: List<ScannedParagraph>,
        val lines: List<ScannedLine>,
        val confidence: Float,
        val cellCount: Int,
        val quality: Float,
        /** Bagian sel yang polanya ada dalam tabel Braille Indonesia. */
        val knownFraction: Float,
    ) : ScanState {
        val isEmpty: Boolean get() = cellCount == 0

        /** Bacaan patut diragukan: biasanya cahaya kurang atau foto kabur. */
        val isDoubtful: Boolean get() = ReadingQuality.isDoubtful(cellCount, confidence)

        /**
         * Titik terbaca yakin, tetapi banyak polanya di luar tabel Braille
         * Indonesia. Keterangan, bukan peringatan: bacaannya tetap sahih.
         */
        val isLikelyOtherLanguage: Boolean
            get() = ReadingQuality.isLikelyOtherLanguage(cellCount, confidence, knownFraction)
    }

    data class Error(val message: String) : ScanState
}

/**
 * Mengatur alur pemindaian: ambil gambar, jalankan model Braille, tampilkan
 * teksnya, lalu simpan ke riwayat.
 *
 * Hasil deteksi disajikan apa adanya. Perapian dengan model bahasa sengaja
 * tidak dipakai di sini: untuk aplikasi yang membacakan karya tulis orang,
 * teks keliru yang terdengar meyakinkan lebih berisiko daripada teks yang
 * jelas-jelas rusak.
 */
class ScanViewModel(
    private val detector: BrailleDetector,
    private val history: ScanHistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun onImageCaptured(bitmap: Bitmap) {
        _state.value = ScanState.Processing
        viewModelScope.launch {
            val hasil = try {
                val deteksi = withContext(Dispatchers.Default) { detector.detect(bitmap) }
                ScanState.Result(
                    text = deteksi.rawText,
                    paragraphs = deteksi.paragraphs,
                    lines = deteksi.lines,
                    confidence = deteksi.confidence,
                    cellCount = deteksi.cellCount,
                    quality = deteksi.quality,
                    knownFraction = deteksi.knownFraction,
                )
            } catch (e: BrailleDetectorException) {
                // Model bermasalah: perlu diberitahukan, bukan disamarkan
                // sebagai "tidak ada braille terdeteksi".
                ScanState.Error(e.message ?: "Model pemindai tidak dapat dijalankan")
            } catch (e: Exception) {
                ScanState.Error(e.message ?: "Gagal memproses gambar")
            }

            // Hasilnya ditampilkan lebih dulu, penyimpanan riwayat menyusul.
            // Urutan ini disengaja: menulis ke basis data sebelum menampilkan
            // hasil membuat kegagalan penyimpanan tampil sebagai pemindaian
            // yang gagal, padahal bacaannya sudah benar.
            _state.value = hasil
            if (hasil is ScanState.Result) recordToHistory(hasil)
        }
    }

    private suspend fun recordToHistory(result: ScanState.Result) {
        try {
            history.record(
                text = result.text,
                confidence = result.confidence,
                cellCount = result.cellCount,
                paragraphCount = result.paragraphs.size,
                doubtful = result.isDoubtful,
            )
        } catch (e: Exception) {
            // Riwayat adalah kemudahan, bukan hasil utama pemindaian. Bila
            // penyimpanannya gagal, teks yang sudah terbaca tetap ditampilkan
            // dan masih bisa disalin atau dibagikan.
            Log.w(TAG, "Hasil pemindaian gagal disimpan ke riwayat", e)
        }
    }

    fun setScanning() {
        _state.value = ScanState.Scanning
    }

    fun reset() {
        _state.value = ScanState.Idle
    }

    override fun onCleared() {
        detector.close()
        super.onCleared()
    }

    private companion object {
        const val TAG = "ScanViewModel"
    }
}
