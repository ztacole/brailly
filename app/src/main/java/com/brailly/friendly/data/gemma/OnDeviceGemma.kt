package com.brailly.friendly.data.gemma

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Wrapper Gemma on-device menggunakan MediaPipe LLM Inference.
 *
 * Menjalankan model Gemma (.task) secara lokal/offline. Inisialisasi bersifat lazy
 * dan aman-gagal: bila model belum ada atau gagal dimuat, [isReady] tetap false
 * sehingga pemanggil dapat melakukan fallback ke jalur online.
 */
class OnDeviceGemma(private val context: Context) {

    private val mutex = Mutex()

    @Volatile
    private var engine: LlmInference? = null

    @Volatile
    private var initFailed: Boolean = false

    /**
     * Memastikan engine terinisialisasi. Mengembalikan true bila engine siap dipakai.
     * Idempotent & thread-safe. Sekali gagal (model tidak ada / korup), tidak dicoba
     * ulang otomatis agar tidak boros — pemanggil langsung fallback.
     */
    private suspend fun ensureReady(): Boolean {
        engine?.let { return true }
        if (initFailed) return false

        return mutex.withLock {
            // Double-check di dalam lock.
            engine?.let { return@withLock true }
            if (initFailed) return@withLock false

            if (!GemmaConfig.isOnDeviceModelPresent(context)) {
                Log.i(TAG, "Model Gemma on-device tidak ditemukan; akan fallback online.")
                initFailed = true
                return@withLock false
            }

            try {
                withContext(Dispatchers.IO) {
                    val options = LlmInferenceOptions.builder()
                        .setModelPath(GemmaConfig.onDeviceModelPath(context))
                        .setMaxTokens(GemmaConfig.MAX_TOKENS)
                        .build()
                    engine = LlmInference.createFromOptions(context, options)
                }
                Log.i(TAG, "Gemma on-device siap.")
                true
            } catch (t: Throwable) {
                // Model ada tapi gagal dimuat (korup / tidak kompatibel / OOM).
                Log.w(TAG, "Inisialisasi Gemma on-device gagal: ${t.message}", t)
                initFailed = true
                engine = null
                false
            }
        }
    }

    /** True bila engine on-device sudah terinisialisasi dan siap. */
    val isReady: Boolean get() = engine != null

    /**
     * Menjalankan prompt pada Gemma on-device.
     * @return teks hasil, atau null bila engine belum siap / inferensi gagal.
     */
    suspend fun generate(prompt: String): String? {
        if (!ensureReady()) return null
        val llm = engine ?: return null
        return try {
            withContext(Dispatchers.IO) {
                llm.generateResponse(prompt)?.trim()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Inferensi Gemma on-device gagal: ${t.message}", t)
            null
        }
    }

    /** Lepaskan resource native. Panggil saat aplikasi tidak lagi membutuhkannya. */
    fun close() {
        try {
            engine?.close()
        } catch (_: Throwable) {
            // abaikan
        } finally {
            engine = null
        }
    }

    private companion object {
        const val TAG = "OnDeviceGemma"
    }
}
