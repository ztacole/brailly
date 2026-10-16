package com.brailly.friendly.ui.screen.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brailly.friendly.data.scan.BrailleDetector
import com.brailly.friendly.core.scan.ReadingQuality
import com.brailly.friendly.data.scan.BrailleDetectorException
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
    ) : ScanState {
        val isEmpty: Boolean get() = cellCount == 0

        /** Bacaan patut diragukan: biasanya cahaya kurang atau foto kabur. */
        val isDoubtful: Boolean get() = ReadingQuality.isDoubtful(cellCount, quality)
    }

    data class Error(val message: String) : ScanState
}

/**
 * Mengatur alur pemindaian: ambil gambar, jalankan model Braille, tampilkan
 * teksnya.
 *
 * Hasil deteksi disajikan apa adanya. Perapian dengan model bahasa sengaja
 * tidak dipakai di sini: untuk aplikasi yang membacakan karya tulis orang,
 * teks keliru yang terdengar meyakinkan lebih berisiko daripada teks yang
 * jelas-jelas rusak.
 */
class ScanViewModel(
    private val detector: BrailleDetector,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun onImageCaptured(bitmap: Bitmap) {
        _state.value = ScanState.Processing
        viewModelScope.launch {
            _state.value = try {
                val hasil = withContext(Dispatchers.Default) { detector.detect(bitmap) }
                ScanState.Result(
                    text = hasil.rawText,
                    paragraphs = hasil.paragraphs,
                    lines = hasil.lines,
                    confidence = hasil.confidence,
                    cellCount = hasil.cellCount,
                    quality = hasil.quality,
                )
            } catch (e: BrailleDetectorException) {
                // Model bermasalah: perlu diberitahukan, bukan disamarkan
                // sebagai "tidak ada braille terdeteksi".
                ScanState.Error(e.message ?: "Model pemindai tidak dapat dijalankan")
            } catch (e: Exception) {
                ScanState.Error(e.message ?: "Gagal memproses gambar")
            }
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
}
