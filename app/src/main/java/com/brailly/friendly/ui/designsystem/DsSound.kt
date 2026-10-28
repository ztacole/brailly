package com.brailly.friendly.ui.designsystem

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/** Momen yang diberi tanda suara. */
enum class DsSound {
    /** Tombol atau node ditekan. */
    Press,

    /** Jawaban benar. */
    Correct,

    /** Jawaban keliru. */
    Wrong,

    /** Level tuntas. */
    Complete,
}

/**
 * Pemutar tanda suara.
 *
 * Dibuat sebagai antarmuka supaya ada implementasi bisu sebagai nilai bawaan
 * [LocalDsSounds]. Dengan begitu komponen design system tetap dapat disusun di
 * luar [DsSoundHost], misalnya pada pratinjau, tanpa membuat aplikasi jatuh.
 */
interface DsSoundPlayer {
    /** Dibaca di dalam komposisi, sehingga sakelarnya langsung tampak berubah. */
    val enabled: Boolean

    fun setEnabled(value: Boolean)

    fun play(sound: DsSound)
}

/** Implementasi bisu: dipakai sebelum [DsSoundHost] terpasang. */
object DsSilentSounds : DsSoundPlayer {
    override val enabled: Boolean get() = false
    override fun setEnabled(value: Boolean) = Unit
    override fun play(sound: DsSound) = Unit
}

val LocalDsSounds = staticCompositionLocalOf<DsSoundPlayer> { DsSilentSounds }

/**
 * Tanda suara yang dibangkitkan oleh platform.
 *
 * Nadanya dibangkitkan [ToneGenerator], dan bunyi ketuk diambil dari efek
 * antarmuka milik sistem. Tidak ada berkas audio yang disertakan dan tidak ada
 * pustaka audio yang ditambahkan: keduanya memang tidak diizinkan, dan
 * kebetulan pendekatan ini juga membuat ukuran aplikasi tidak bertambah.
 *
 * Nada sintetis jelas lebih sederhana daripada efek suara yang direkam. Begitu
 * berkas audio berlisensi tersedia, `play` dapat dialihkan ke `SoundPool`
 * tanpa mengubah satu pun pemanggilnya.
 *
 * Tiga hal yang dihormati di sini:
 *
 * 1. **Mode senyap.** Saat ponsel dalam mode senyap atau getar, tidak ada nada
 *    yang dibunyikan. Aplikasi yang tetap berbunyi dalam mode senyap akan
 *    membuat pengguna malu di tempat umum.
 * 2. **Setelan bunyi sentuh sistem.** Bunyi ketuk memakai
 *    `AudioManager.playSoundEffect`, yang sudah mengikuti setelan itu.
 * 3. **Pilihan pengguna.** Sakelarnya tersimpan antar sesi, dan khusus
 *    dimatikan secara bawaan tidak dilakukan karena suara adalah bagian dari
 *    umpan balik yang diandalkan pengguna tunanetra.
 */
class DsPlatformSounds(context: Context) : DsSoundPlayer {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var enabledState by mutableStateOf(prefs.getBoolean(KEY_ENABLED, true))
    override val enabled: Boolean get() = enabledState

    /**
     * Dibuat saat pertama dipakai.
     *
     * `ToneGenerator` menahan jalur audio selama hidupnya, jadi tidak ada
     * gunanya membuatnya untuk pengguna yang mematikan efek suara.
     */
    private var toneGenerator: ToneGenerator? = null
    private var toneUnavailable = false

    override fun setEnabled(value: Boolean) {
        enabledState = value
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        if (!value) release()
    }

    override fun play(sound: DsSound) {
        if (!enabledState) return
        // Mode senyap dan getar sama-sama berarti "jangan berbunyi".
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        if (sound == DsSound.Press) {
            // Efek milik sistem, bukan nada buatan sendiri: bunyinya sama
            // dengan ketukan di aplikasi lain dan ikut mati bila pengguna
            // mematikan bunyi sentuh di Pengaturan.
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
            return
        }

        val generator = ensureToneGenerator() ?: return
        val (tone, durationMs) = when (sound) {
            DsSound.Correct -> ToneGenerator.TONE_PROP_ACK to 150
            DsSound.Wrong -> ToneGenerator.TONE_PROP_NACK to 250
            DsSound.Complete -> ToneGenerator.TONE_PROP_BEEP2 to 300
            DsSound.Press -> return
        }
        try {
            generator.startTone(tone, durationMs)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Nada $sound gagal dibunyikan", e)
        }
    }

    private fun ensureToneGenerator(): ToneGenerator? {
        toneGenerator?.let { return it }
        if (toneUnavailable) return null
        return try {
            ToneGenerator(AudioManager.STREAM_SYSTEM, TONE_VOLUME).also { toneGenerator = it }
        } catch (e: RuntimeException) {
            // Jalur audio bisa sedang dipakai penuh. Kegagalannya tidak
            // diulang terus-menerus: tanpa penanda ini, setiap jawaban akan
            // memicu percobaan baru yang pasti gagal.
            Log.w(TAG, "ToneGenerator tidak tersedia", e)
            toneUnavailable = true
            null
        }
    }

    /** Lepaskan jalur audio. */
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
        toneUnavailable = false
    }

    private companion object {
        const val TAG = "DsPlatformSounds"
        const val PREFS_NAME = "brailly_sound"
        const val KEY_ENABLED = "sound_effects_enabled"

        /** 0..100. Nada umpan balik tidak perlu menandingi suara pembacaan. */
        const val TONE_VOLUME = 70
    }
}

/**
 * Sediakan pemutar tanda suara bagi seluruh pohon komposisi di bawahnya.
 *
 * Dipasang satu kali di tingkat aplikasi. Jalur audionya dilepas saat pohon
 * ini dibuang.
 */
@Composable
fun DsSoundHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val player = remember(context) { DsPlatformSounds(context) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    CompositionLocalProvider(
        LocalDsSounds provides player,
        content = content,
    )}
