package com.brailly.friendly.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Tahap pengenalan ucapan yang sedang berjalan. */
sealed interface DictationStatus {
    data object Idle : DictationStatus

    /** Mikrofon terbuka, menunggu atau sedang menangkap ucapan. */
    data object Listening : DictationStatus

    /** Ucapan sudah selesai, hasilnya sedang diproses. */
    data object Working : DictationStatus

    /** Gagal, dengan pesan yang layak ditampilkan ke pengguna. */
    data class Failed(val message: String) : DictationStatus
}

data class DictationState(
    val status: DictationStatus = DictationStatus.Idle,
    /** Teks sementara, ditampilkan sambil pengguna masih berbicara. */
    val partialText: String = "",
) {
    val isListening: Boolean get() = status is DictationStatus.Listening
    val isBusy: Boolean
        get() = status is DictationStatus.Listening || status is DictationStatus.Working

    val errorMessage: String? get() = (status as? DictationStatus.Failed)?.message
}

/**
 * Pengubah ucapan menjadi teks (voice-to-text) memakai [SpeechRecognizer]
 * milik sistem.
 *
 * Dipakai layar Terjemah agar pengguna bisa mendiktekan teks yang akan diubah
 * menjadi braille, tanpa mengetik.
 *
 * Perekaman hanya berjalan setelah pengguna menekan tombol mikrofon dan
 * berhenti sendiri saat ucapannya selesai; tidak ada mode mendengarkan
 * terus-menerus. Audionya diproses oleh layanan pengenalan suara perangkat,
 * dan pada sebagian perangkat layanan itu mengirim potongan audio ke server
 * penyedianya. Jadi berbeda dengan pemindaian braille yang sepenuhnya di
 * perangkat, dikte dapat melibatkan jaringan di luar kendali aplikasi ini.
 *
 * Seluruh metode publik harus dipanggil dari main thread: itu syarat
 * [SpeechRecognizer], dan semua pemanggilnya berasal dari callback UI.
 */
class DictationRecognizer(context: Context) {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(DictationState())
    val state: StateFlow<DictationState> = _state.asStateFlow()

    /**
     * Teks final hasil dikte. Dibuat sebagai aliran peristiwa, bukan state,
     * karena hasil yang sama tidak boleh disisipkan dua kali ke kolom teks
     * setiap kali layarnya disusun ulang.
     */
    private val _results = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val results: SharedFlow<String> = _results.asSharedFlow()

    private var recognizer: SpeechRecognizer? = null

    /**
     * Ketersediaan layanan pengenalan suara, diperiksa sekali.
     *
     * Hasilnya bergantung pada paket yang terpasang. Memasang mesin pengenal
     * baru di tengah sesi tidak akan terbaca sampai aplikasi dijalankan ulang;
     * menukar itu dengan kueri ke package manager pada setiap penyusunan ulang
     * UI tidak sepadan.
     */
    val isAvailable: Boolean by lazy {
        SpeechRecognizer.isRecognitionAvailable(appContext)
    }

    /**
     * Mulai mendengarkan. Pemanggil wajib memastikan izin `RECORD_AUDIO` sudah
     * diberikan; tanpa itu layanan menjawab dengan galat izin.
     */
    fun start() {
        if (!isAvailable) {
            _state.value = DictationState(
                status = DictationStatus.Failed(
                    "Perangkat ini belum punya layanan pengenalan suara.",
                ),
            )
            return
        }

        val engine = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }

        _state.value = DictationState(status = DictationStatus.Listening)
        engine.startListening(buildIntent())
    }

    /** Batalkan sesi yang sedang berjalan dan kembalikan keadaan ke diam. */
    fun cancel() {
        recognizer?.cancel()
        _state.value = DictationState()
    }

    /** Buang pesan galat terakhir tanpa memulai sesi baru. */
    fun dismissMessage() {
        _state.update { it.copy(status = DictationStatus.Idle, partialText = "") }
    }

    /** Lepaskan sumber daya mikrofon. */
    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _state.value = DictationState()
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_TAG)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Untuk dikte hanya dugaan terbaik yang berguna. Memilih sendiri
            // di antara beberapa alternatif adalah pekerjaan yang tidak bisa
            // dilakukan aplikasi, dan menyodorkannya ke pengguna lebih lambat
            // daripada membiarkan mereka memperbaiki teksnya langsung.
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    private val listener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            _state.update { it.copy(status = DictationStatus.Listening) }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _state.update { it.copy(status = DictationStatus.Working) }
        }

        override fun onError(error: Int) {
            _state.value = DictationState(status = DictationStatus.Failed(messageFor(error)))
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

            if (text.isEmpty()) {
                _state.value = DictationState(
                    status = DictationStatus.Failed("Tidak ada ucapan yang tertangkap."),
                )
                return
            }

            _state.value = DictationState()
            _results.tryEmit(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?: return
            _state.update { it.copy(partialText = text) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun messageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO ->
            "Mikrofon tidak dapat dibaca. Pastikan tidak dipakai aplikasi lain."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Izin mikrofon belum diberikan."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Pengenalan suara perangkat ini butuh koneksi internet."
        SpeechRecognizer.ERROR_NO_MATCH ->
            "Ucapan tidak dikenali. Coba bicara lebih jelas."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "Layanan pengenalan suara sedang sibuk. Coba sesaat lagi."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Tidak ada suara yang terdengar."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "Bahasa Indonesia belum tersedia di layanan pengenalan suara."
        SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
            "Layanan pengenalan suara tidak merespons."
        else -> {
            Log.w(TAG, "Pengenalan suara gagal, kode $error")
            "Pengenalan suara gagal (kode $error)."
        }
    }

    private companion object {
        const val TAG = "DictationRecognizer"
        const val LANGUAGE_TAG = "id-ID"
    }
}
