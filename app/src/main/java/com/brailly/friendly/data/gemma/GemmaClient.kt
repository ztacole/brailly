package com.brailly.friendly.data.gemma

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Klien interpretasi Braille berbasis Gemma dengan strategi dua lapis:
 *
 * 1. **On-device (offline)** — jalur utama via MediaPipe LLM Inference ([OnDeviceGemma]).
 * 2. **Gemini API (online)** — fallback bila model on-device belum terinstall sempurna
 *    (tidak ada / gagal init) dan API key tersedia.
 *
 * Bila keduanya tidak tersedia, mengembalikan null (pemanggil memakai hasil detektor mentah).
 */
class GemmaClient(
    private val http: HttpClient,
    private val onDevice: OnDeviceGemma,
) {

    /**
     * Rapikan teks braille mentah (hasil detektor) menjadi kalimat yang koheren.
     * @return teks bersih, atau null bila semua jalur tidak tersedia / gagal.
     */
    suspend fun refineBrailleText(rawText: String): String? {
        if (rawText.isBlank()) return null
        val prompt = buildPrompt(rawText)

        // 1) Coba on-device (offline) lebih dulu.
        onDevice.generate(prompt)?.let { result ->
            if (result.isNotBlank()) {
                Log.d(TAG, "Hasil dari Gemma on-device.")
                return result
            }
        }

        // 2) Fallback ke Gemini API bila on-device belum siap / gagal.
        Log.d(TAG, "On-device tidak tersedia; mencoba fallback Gemini API.")
        return refineViaGeminiApi(prompt)
    }

    private suspend fun refineViaGeminiApi(prompt: String): String? {
        if (!GemmaConfig.isFallbackEnabled) return null
        return try {
            val url = "${GemmaConfig.BASE_URL}/${GemmaConfig.FALLBACK_MODEL}:generateContent"
            val response: GenerateContentResponse = http.post(url) {
                header("x-goog-api-key", GemmaConfig.API_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    GenerateContentRequest(
                        contents = listOf(
                            Content(parts = listOf(Part(text = prompt)))
                        )
                    )
                )
            }.body()
            response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Fallback Gemini API gagal: ${e.message}", e)
            null
        }
    }

    private fun buildPrompt(rawText: String): String =
        "Berikut hasil pemindaian teks Braille (Grade 1) yang mungkin mengandung " +
            "kesalahan kecil karena kualitas gambar: \"$rawText\". " +
            "Perbaiki menjadi kalimat Bahasa Indonesia yang wajar dan koheren. " +
            "Balas hanya dengan teks hasil perbaikan, tanpa penjelasan."

    private companion object {
        const val TAG = "GemmaClient"
    }
}

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null,
)

@Serializable
data class Part(
    val text: String,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
)

@Serializable
data class Candidate(
    val content: Content? = null,
    @SerialName("finishReason") val finishReason: String? = null,
)
