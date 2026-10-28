package com.brailly.friendly.data.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/** Kecepatan baca yang bisa dipilih pengguna di layar Profil. */
enum class SpeechRate(val label: String, val multiplier: Float) {
    SLOW("Lambat", 0.7f),
    NORMAL("Normal", 1.0f),
    FAST("Cepat", 1.4f),
}

/** Kesiapan mesin suara perangkat. */
sealed interface SpeechEngineStatus {
    data object Initializing : SpeechEngineStatus
    data object Ready : SpeechEngineStatus
    data class Unavailable(val reason: String) : SpeechEngineStatus
}

/**
 * Keadaan pembacaan yang diamati UI.
 *
 * [speakingId] memuat penanda teks yang sedang dibacakan, bukan sekadar
 * bendera boolean, supaya daftar riwayat bisa menandai baris mana yang
 * sedang berbunyi ketika beberapa tombol bacakan tampil bersamaan.
 */
data class SpeechState(
    val status: SpeechEngineStatus = SpeechEngineStatus.Initializing,
    val speakingId: String? = null,
    val rate: SpeechRate = SpeechRate.NORMAL,
    /** True bila mesin suara tidak punya data bahasa Indonesia. */
    val usingFallbackLanguage: Boolean = false,
    /** Kegagalan sesaat, misalnya mesin menolak satu permintaan baca. */
    val lastError: String? = null,
) {
    val isReady: Boolean get() = status is SpeechEngineStatus.Ready
    val isSpeaking: Boolean get() = speakingId != null

    fun isSpeaking(utteranceId: String): Boolean = speakingId == utteranceId

    /**
     * Alasan pembacaan tidak dapat dilakukan, siap ditampilkan ke pengguna,
     * atau null bila tidak ada masalah yang perlu disampaikan.
     */
    val problem: String?
        get() = lastError ?: (status as? SpeechEngineStatus.Unavailable)?.reason
}

/**
 * Pembaca teks memakai mesin suara bawaan perangkat.
 *
 * Dipakai untuk membacakan hasil pemindaian, isi riwayat, dan hasil terjemahan
 * Braille ke teks. Mesin suaranya milik sistem, jadi tidak ada teks yang
 * dikirim ke jaringan oleh kelas ini.
 *
 * Instansnya tunggal untuk seluruh aplikasi: `TextToSpeech` mengikat sebuah
 * layanan sistem, dan membuat satu instans per layar berarti pembacaan di satu
 * layar tidak bisa dihentikan dari layar lain.
 *
 * Kegagalan di kelas ini selalu berakhir sebagai pesan di [SpeechState.problem],
 * tidak pernah sebagai diam. Mesin suara bisa tidak terpasang, kehilangan data
 * bahasa, atau menolak permintaan, dan tombol kelabu tanpa keterangan membuat
 * ketiganya tampak sama seperti aplikasi yang rusak.
 *
 * Harus dibuat dan dipanggil dari main thread, karena `TextToSpeech` mengirim
 * callback-nya lewat looper thread pembuatnya.
 */
class TextToSpeechManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(SpeechState(rate = readSavedRate()))
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    /**
     * Dideklarasikan sebelum blok `init` dengan sengaja.
     *
     * Properti dan blok `init` dijalankan berurutan sesuai letaknya di kelas.
     * Saat tidak ada mesin suara terpasang, `TextToSpeech` memanggil
     * `OnInitListener` secara serentak dari dalam konstruktornya, jadi
     * penyiapan bisa berjalan sebelum baris-baris di bawah `init` sempat
     * terisi. Menaruh pendengar ini di atas memastikan nilainya sudah ada.
     */
    private val progressListener = object : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            val base = utteranceId?.baseUtteranceId() ?: return
            _state.update { it.copy(speakingId = base) }
        }

        override fun onDone(utteranceId: String?) {
            // Teks panjang dipecah menjadi beberapa ucapan. Status "sedang
            // membaca" baru dilepas setelah potongan terakhir selesai, kalau
            // tidak tombolnya berkedip kembali ke "bacakan" di tengah bacaan.
            if (utteranceId != null && utteranceId.isFinalChunk()) {
                clearSpeaking(utteranceId.baseUtteranceId())
            }
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            clearSpeaking(utteranceId?.baseUtteranceId())
        }

        @Deprecated("Diganti onError(String, Int), tetap wajib ditimpa")
        override fun onError(utteranceId: String?) {
            reportSpeakFailure(utteranceId, null)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            reportSpeakFailure(utteranceId, errorCode)
        }
    }

    private var engine: TextToSpeech? = null

    /** Status dari `OnInitListener` yang belum sempat diproses. */
    private var initStatus: Int? = null

    /**
     * Permintaan baca yang datang sebelum mesin selesai disiapkan.
     *
     * Penyiapan mesin suara butuh waktu mengikat layanannya, dan tekanan
     * tombol pertama pengguna sering jatuh tepat di jendela itu. Tanpa
     * penampungan ini, pembacaan pertama hilang tanpa pesan apa pun.
     */
    private var pendingRequest: Request? = null

    private data class Request(val text: String, val utteranceId: String)

    init {
        createEngine()
    }

    /**
     * Paksa penyiapan mesin suara sekarang.
     *
     * Dipanggil saat layar utama dibuat. Instans ini disediakan Koin secara
     * malas, dan tanpa pemanggilan ini mesinnya baru mulai disiapkan pada
     * tekanan tombol bacakan yang pertama, sehingga tekanan itu hanya
     * mengantre tanpa suara apa pun keluar.
     */
    fun warmUp() {
        if (engine == null) createEngine()
    }

    // --- Penyiapan mesin ---

    private fun createEngine() {
        initStatus = null
        _state.update {
            it.copy(status = SpeechEngineStatus.Initializing, lastError = null)
        }
        engine = TextToSpeech(appContext, ::onEngineInit)
        // Ketika mesin suara tidak terpasang, `OnInitListener` dipanggil dari
        // dalam konstruktor di atas, yaitu saat `engine` masih kosong. Status
        // seperti itu tertahan di `initStatus` dan baru diproses di sini.
        applyInitStatus()
    }

    /**
     * Siapkan ulang mesin suara dari awal.
     *
     * Kegagalan penyiapan bersifat sekali-dan-selamanya pada `TextToSpeech`:
     * instans yang gagal tidak akan pulih sendiri. Tanpa jalan ulang ini,
     * pengguna yang memasang mesin suara setelah aplikasi berjalan harus
     * menutup aplikasinya dulu agar fitur bacakan hidup.
     */
    fun retryInit() {
        engine?.shutdown()
        engine = null
        createEngine()
    }

    private fun onEngineInit(status: Int) {
        initStatus = status
        applyInitStatus()
    }

    private fun applyInitStatus() {
        val status = initStatus ?: return
        val tts = engine ?: return
        initStatus = null

        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Mesin suara gagal disiapkan, kode $status")
            pendingRequest = null
            _state.update {
                it.copy(
                    status = SpeechEngineStatus.Unavailable(
                        "Mesin suara tidak dapat disiapkan. Pasang atau aktifkan " +
                            "mesin text-to-speech di Pengaturan sistem, lalu coba lagi.",
                    ),
                    speakingId = null,
                )
            }
            return
        }

        val indonesianAvailable = applyLanguage(tts)
        tts.setSpeechRate(_state.value.rate.multiplier)
        tts.setOnUtteranceProgressListener(progressListener)
        _state.update {
            it.copy(
                status = SpeechEngineStatus.Ready,
                usingFallbackLanguage = !indonesianAvailable,
                lastError = null,
            )
        }
        Log.i(TAG, "Mesin suara siap, bahasa Indonesia tersedia: $indonesianAvailable")

        pendingRequest?.let { request ->
            pendingRequest = null
            speak(request.text, request.utteranceId)
        }
    }

    /** @return true bila data bahasa Indonesia tersedia di mesin suara. */
    private fun applyLanguage(tts: TextToSpeech): Boolean {
        val result = tts.setLanguage(Locale.forLanguageTag("id-ID"))
        if (result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return true
        }
        // Tanpa data bahasa Indonesia teksnya tetap dibacakan memakai bahasa
        // bawaan mesin. Pelafalannya meleset, tetapi diam sama sekali lebih
        // merugikan pengguna yang mengandalkan suara.
        Log.w(TAG, "Bahasa Indonesia tidak tersedia (kode $result), memakai bahasa bawaan")
        tts.setLanguage(Locale.getDefault())
        return false
    }

    private fun reportSpeakFailure(utteranceId: String?, errorCode: Int?) {
        Log.w(TAG, "Pembacaan gagal (kode $errorCode) untuk $utteranceId")
        val base = utteranceId?.baseUtteranceId()
        _state.update { current ->
            current.copy(
                speakingId = if (base == null || current.speakingId == base) {
                    null
                } else {
                    current.speakingId
                },
                lastError = "Mesin suara gagal membacakan teks" +
                    if (errorCode != null) " (kode $errorCode)." else ".",
            )
        }
    }

    private fun clearSpeaking(baseId: String?) {
        _state.update { current ->
            // Hanya bersihkan bila penandanya memang milik bacaan yang
            // dilaporkan selesai; permintaan baru yang menyusul tidak boleh
            // ikut terhapus oleh laporan bacaan sebelumnya.
            if (baseId == null || current.speakingId == baseId) {
                current.copy(speakingId = null)
            } else {
                current
            }
        }
    }

    // --- Perintah dari UI ---

    /**
     * Bacakan [text]. [utteranceId] menandai sumber bacaan agar tombol yang
     * bersangkutan bisa berubah menjadi "hentikan" sementara teks berbunyi.
     *
     * Permintaan baru menghentikan bacaan yang sedang berjalan: menumpuk
     * antrean membuat pengguna menunggu teks lama habis dulu.
     *
     * Bila mesinnya belum siap, permintaannya disimpan dan penyiapan dicoba
     * ulang, sehingga tekanan tombol tetap berbuah suara begitu mesinnya hidup.
     */
    fun speak(text: String, utteranceId: String = DEFAULT_UTTERANCE_ID) {
        val spoken = text.normalizedForSpeech()
        if (spoken.isBlank()) return

        val tts = engine
        val current = _state.value
        if (tts == null || !current.isReady) {
            pendingRequest = Request(spoken, utteranceId)
            // Penyiapan yang sudah berjalan dibiarkan selesai; yang sudah
            // gagal dicoba lagi, karena instans TextToSpeech yang gagal tidak
            // akan pulih dengan sendirinya.
            if (current.status !is SpeechEngineStatus.Initializing) retryInit()
            return
        }

        val chunks = spoken.chunkForSpeech()
        val rejected = chunks.withIndex().firstOrNull { (index, chunk) ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val id = chunkUtteranceId(utteranceId, index, chunks.size)
            tts.speak(chunk, mode, null, id) != TextToSpeech.SUCCESS
        }

        if (rejected != null) {
            // Nilai kembalian `speak` adalah satu-satunya tanda penolakan di
            // muka: pada kasus ini UtteranceProgressListener tidak pernah
            // dipanggil, jadi tanpa pemeriksaan ini tidak ada yang melapor.
            reportSpeakFailure(utteranceId, null)
            return
        }

        // `onStart` kadang tiba beberapa ratus milidetik kemudian. Menandainya
        // sekarang membuat tombol langsung berganti menjadi "hentikan"
        // sehingga tekanan kedua tidak memulai bacaan rangkap.
        _state.update { it.copy(speakingId = utteranceId, lastError = null) }
    }

    /** Hentikan pembacaan yang sedang berjalan maupun yang masih tertunda. */
    fun stop() {
        pendingRequest = null
        engine?.stop()
        _state.update { it.copy(speakingId = null) }
    }

    /** Bacakan [text] bila sedang diam, hentikan bila teks itu yang berbunyi. */
    fun toggle(text: String, utteranceId: String = DEFAULT_UTTERANCE_ID) {
        if (_state.value.isSpeaking(utteranceId)) stop() else speak(text, utteranceId)
    }

    /** Ubah kecepatan baca dan simpan pilihannya antar sesi. */
    fun setRate(rate: SpeechRate) {
        prefs.edit { putString(KEY_RATE, rate.name) }
        engine?.setSpeechRate(rate.multiplier)
        _state.update { it.copy(rate = rate) }
    }

    /**
     * Lepaskan mesin suara. Dipanggil saat pemilik instans benar-benar selesai
     * memakainya; dalam aplikasi ini mesinnya hidup selama proses berjalan.
     */
    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        _state.update {
            it.copy(
                status = SpeechEngineStatus.Unavailable("Mesin suara sudah dilepas"),
                speakingId = null,
            )
        }
    }

    private fun readSavedRate(): SpeechRate {
        val saved = prefs.getString(KEY_RATE, null) ?: return SpeechRate.NORMAL
        return SpeechRate.entries.firstOrNull { it.name == saved } ?: SpeechRate.NORMAL
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
        private const val PREFS_NAME = "brailly_speech"
        private const val KEY_RATE = "speech_rate"

        /** Penanda bawaan untuk layar yang hanya punya satu tombol bacakan. */
        const val DEFAULT_UTTERANCE_ID = "brailly-speech"
    }
}

// --- Penyiapan teks ---

private const val CHUNK_SEPARATOR = '#'

/** Batas aman bila `getMaxSpeechInputLength()` mengembalikan nilai tak wajar. */
private const val SPEECH_INPUT_FALLBACK = 3900

/** Sisa jarak dari batas resmi; sebagian mesin menolak teks tepat di batasnya. */
private const val SPEECH_INPUT_MARGIN = 64

private val SENTENCE_ENDINGS = charArrayOf('.', '!', '?', ',', ':', ';')

private fun chunkUtteranceId(baseId: String, index: Int, total: Int): String =
    "$baseId$CHUNK_SEPARATOR$index/$total"

private fun String.baseUtteranceId(): String = substringBefore(CHUNK_SEPARATOR)

private fun String.isFinalChunk(): Boolean {
    val suffix = substringAfter(CHUNK_SEPARATOR, "")
    if (suffix.isEmpty()) return true
    val index = suffix.substringBefore('/').toIntOrNull() ?: return true
    val total = suffix.substringAfter('/').toIntOrNull() ?: return true
    return index >= total - 1
}

/**
 * Rapikan teks sebelum dibacakan.
 *
 * Hasil pemindaian datang sebagai kumpulan baris tanpa tanda baca. Mesin suara
 * mengabaikan ganti baris, sehingga seluruh halaman terbaca sebagai satu
 * kalimat tanpa jeda. Titik yang ditambahkan di sini hanya memengaruhi jeda,
 * tidak menambah kata yang diucapkan, jadi isi bacaannya tetap setia pada
 * aslinya.
 */
private fun String.normalizedForSpeech(): String =
    lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(separator = "\n") { line ->
            if (line.last() in SENTENCE_ENDINGS) line else "$line."
        }

private fun speechInputLimit(): Int {
    val reported = TextToSpeech.getMaxSpeechInputLength() - SPEECH_INPUT_MARGIN
    return if (reported > 0) reported else SPEECH_INPUT_FALLBACK
}

/**
 * Pecah teks yang melebihi batas masukan mesin suara.
 *
 * `TextToSpeech.speak` menolak teks yang lebih panjang dari
 * `getMaxSpeechInputLength()` dan mengembalikan error tanpa membacakan apa pun.
 * Satu halaman braille yang padat bisa melewati batas itu, jadi teksnya dipecah
 * di batas kalimat terdekat supaya jedanya terdengar wajar.
 */
private fun String.chunkForSpeech(): List<String> {
    val limit = speechInputLimit()
    if (length <= limit) return listOf(this)

    val chunks = mutableListOf<String>()
    var start = 0
    while (start < length) {
        if (length - start <= limit) {
            chunks += substring(start)
            break
        }
        val cut = lastBreakIndex(start, start + limit)
        chunks += substring(start, cut)
        start = cut
    }
    return chunks.map { it.trim() }.filter { it.isNotEmpty() }
}

/**
 * Titik potong alami terakhir dalam rentang `[start, end)`: diutamakan akhir
 * kalimat, lalu sela kata, dan sebagai jalan terakhir dipotong paksa agar
 * perulangan pemecah teks tidak berhenti tanpa kemajuan.
 */
private fun String.lastBreakIndex(start: Int, end: Int): Int {
    for (i in end - 1 downTo start) {
        if (this[i] in SENTENCE_ENDINGS || this[i] == '\n') return i + 1
    }
    for (i in end - 1 downTo start) {
        if (this[i].isWhitespace()) return i + 1
    }
    return end
}
