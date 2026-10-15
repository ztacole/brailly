package com.brailly.friendly.ui.screen.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brailly.friendly.data.gemma.DownloadStatus
import com.brailly.friendly.data.gemma.GemmaClient
import com.brailly.friendly.data.gemma.GemmaConfig
import com.brailly.friendly.data.gemma.GemmaModelDownloader
import com.brailly.friendly.data.scan.BrailleDetector
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
    data class Result(
        val detectedText: String,
        val refinedText: String?,
        val confidence: Float,
        val usedGemma: Boolean,
    ) : ScanState
    data class Error(val message: String) : ScanState
}

/**
 * Status model Gemma on-device untuk ditampilkan di UI (banner unduh).
 */
sealed interface ModelState {
    /** Model sudah tersedia di perangkat. */
    data object Ready : ModelState

    /** Model belum ada; bisa diunduh (bila unduhan dikonfigurasi). */
    data class NotDownloaded(val canDownload: Boolean) : ModelState

    /** Sedang mengunduh. [fraction] null bila ukuran total tak diketahui. */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val fraction: Float?,
    ) : ModelState

    /**
     * Unduhan terhenti tetapi progres tersimpan; dapat dilanjutkan (resume).
     */
    data class Paused(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val fraction: Float?,
        val message: String,
    ) : ModelState

    /** Unduhan gagal (tanpa progres yang bisa dilanjutkan). */
    data class Error(val message: String) : ModelState
}

class ScanViewModel(
    private val detector: BrailleDetector,
    private val gemmaClient: GemmaClient,
    private val modelDownloader: GemmaModelDownloader,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _modelState = MutableStateFlow<ModelState>(initialModelState())
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private fun initialModelState(): ModelState =
        if (modelDownloader.isModelAvailable()) {
            ModelState.Ready
        } else {
            ModelState.NotDownloaded(canDownload = GemmaConfig.isDownloadConfigured)
        }

    /** Mulai (atau lanjutkan) unduhan model on-device dan pancarkan progres ke UI. */
    fun downloadModel() {
        // Hindari memulai ulang bila sudah berjalan.
        if (_modelState.value is ModelState.Downloading) return
        viewModelScope.launch {
            modelDownloader.download().collect { status ->
                _modelState.value = when (status) {
                    is DownloadStatus.AlreadyAvailable, DownloadStatus.Completed -> ModelState.Ready
                    is DownloadStatus.InProgress -> ModelState.Downloading(
                        bytesDownloaded = status.bytesDownloaded,
                        totalBytes = status.totalBytes,
                        fraction = status.fraction,
                    )
                    is DownloadStatus.Paused -> ModelState.Paused(
                        bytesDownloaded = status.bytesDownloaded,
                        totalBytes = status.totalBytes,
                        fraction = if (status.totalBytes > 0) {
                            (status.bytesDownloaded.toFloat() / status.totalBytes).coerceIn(0f, 1f)
                        } else null,
                        message = status.message,
                    )
                    is DownloadStatus.Failed -> ModelState.Error(status.message)
                }
            }
        }
    }

    fun onImageCaptured(bitmap: Bitmap) {
        _state.value = ScanState.Processing
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) { detector.detect(bitmap) }
                val refined = withContext(Dispatchers.IO) {
                    gemmaClient.refineBrailleText(result.rawText)
                }
                _state.value = ScanState.Result(
                    detectedText = result.rawText,
                    refinedText = refined,
                    confidence = result.confidence,
                    usedGemma = refined != null,
                )
            } catch (e: Exception) {
                _state.value = ScanState.Error(e.message ?: "Gagal memproses gambar")
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
