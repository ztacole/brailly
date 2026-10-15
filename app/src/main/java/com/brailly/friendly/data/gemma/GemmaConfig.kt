package com.brailly.friendly.data.gemma

import android.content.Context
import com.brailly.friendly.BuildConfig
import java.io.File

/**
 * Konfigurasi untuk interpretasi teks Braille menggunakan Gemma.
 *
 * Strategi dua lapis:
 * 1. On-device (offline) — model Gemma dijalankan lokal via MediaPipe LLM Inference.
 *    Ini adalah jalur utama; bekerja tanpa koneksi internet.
 * 2. Fallback Gemini API (online) — dipakai bila model on-device belum terinstall
 *    sempurna (file model tidak ada / gagal inisialisasi) DAN [API_KEY] tersedia.
 */
object GemmaConfig {

    // ------- On-device (MediaPipe LLM Inference) -------

    /**
     * Nama file model Gemma (format .task / .litertlm) yang diharapkan berada di
     * penyimpanan internal aplikasi. Model diunduh/di-push terpisah karena ukurannya
     * besar (ratusan MB), sehingga tidak dibundel dalam APK.
     *
     * Contoh cara push saat pengembangan:
     *   adb push gemma3-1b-it-int4.task /data/local/tmp/
     *   lalu salin ke filesDir aplikasi, atau letakkan langsung di filesDir.
     */
    const val ON_DEVICE_MODEL_FILE: String = "gemma3-1b-it-int4.task"

    /**
     * URL unduhan model Gemma on-device. Model tidak dibundel dalam APK karena
     * ukurannya besar (ratusan MB), jadi diunduh saat runtime lalu disimpan di
     * [onDeviceModelPath].
     *
     * Contoh sumber: Hugging Face LiteRT Community (Gemma-3 1B, 4-bit .task).
     * Beberapa sumber memerlukan header Authorization; lihat [MODEL_DOWNLOAD_AUTH_HEADER].
     *
     * TODO: Ganti dengan URL model .task yang dapat diunduh langsung.
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task"

    /**
     * Header Authorization opsional untuk mengunduh model (mis. token Hugging Face).
     * Dibangun dari [BuildConfig.HF_TOKEN] yang berasal dari local.properties.
     * Kosong bila token tidak diisi (URL publik tanpa autentikasi).
     */
    val MODEL_DOWNLOAD_AUTH_HEADER: String
        get() = BuildConfig.HF_TOKEN.trim().let { token ->
            if (token.isBlank()) "" else "Bearer $token"
        }

    /**
     * Perkiraan ukuran unduhan (byte) untuk validasi & tampilan progress.
     * Gemma-3 1B IT int4 (.task) ≈ 555 MB. 0 bila tak diketahui.
     */
    const val EXPECTED_MODEL_SIZE_BYTES: Long = 554_000_000L

    /** Apakah unduhan model dikonfigurasi (URL terisi). */
    val isDownloadConfigured: Boolean get() = MODEL_DOWNLOAD_URL.isNotBlank()

    /** Batas token maksimum untuk sesi inferensi on-device. */
    const val MAX_TOKENS: Int = 1024

    /** Path absolut model on-device di penyimpanan internal aplikasi. */
    fun onDeviceModelPath(context: Context): String =
        File(context.filesDir, ON_DEVICE_MODEL_FILE).absolutePath

    /**
     * Apakah file model on-device tersedia dan tampak valid (ada & tidak kosong).
     * Ini pengecekan ringan; validitas penuh baru diketahui saat inisialisasi.
     */
    fun isOnDeviceModelPresent(context: Context): Boolean {
        val file = File(context.filesDir, ON_DEVICE_MODEL_FILE)
        return file.exists() && file.length() > 0L
    }

    // ------- Fallback: Google AI (Gemini) API -------

    /**
     * API key Google AI Studio untuk fallback online. Berasal dari local.properties
     * (BuildConfig.GEMINI_API_KEY) — TIDAK di-hardcode agar tidak bocor ke VCS/APK.
     * Kosong bila tidak dikonfigurasi; fallback online otomatis nonaktif.
     */
    val API_KEY: String get() = BuildConfig.GEMINI_API_KEY

    /** Model Gemini yang dipakai sebagai fallback online. */
    const val FALLBACK_MODEL: String = "gemini-2.5-flash"
    const val BASE_URL: String = "https://generativelanguage.googleapis.com/v1beta/models"

    /** Fallback online hanya aktif bila API key terisi. */
    val isFallbackEnabled: Boolean get() = API_KEY.isNotBlank()
}
