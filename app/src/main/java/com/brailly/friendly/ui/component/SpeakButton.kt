package com.brailly.friendly.ui.component

import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.data.speech.TextToSpeechManager
import org.koin.compose.koinInject

/**
 * Tombol bacakan/hentikan untuk sepotong teks.
 *
 * Satu tombol dipakai untuk kedua arah perintah: selama teksnya berbunyi,
 * ikonnya berubah menjadi kotak henti. Tombol terpisah "bacakan" dan
 * "hentikan" berarti separuh waktunya ada tombol yang tidak berguna, dan bagi
 * pengguna yang menyusuri layar dengan pembaca layar setiap tombol tambahan
 * adalah satu perhentian lagi.
 *
 * Tombolnya tetap hidup selama ada teks untuk dibacakan, bahkan ketika mesin
 * suaranya belum siap. Tombol kelabu tidak membedakan "belum siap" dari "tidak
 * terpasang" maupun "memang rusak"; menekannya kini mengantre bacaan,
 * mencoba menyiapkan mesinnya lagi, dan menyebutkan kendalanya.
 *
 * [utteranceId] membedakan sumber bacaan ketika beberapa tombol tampil
 * sekaligus, misalnya pada daftar riwayat: hanya baris yang benar-benar
 * berbunyi yang berubah menjadi tombol henti.
 */
@Composable
fun SpeakButton(
    text: String,
    contentDescription: String = "Bacakan teks",
    modifier: Modifier = Modifier,
    utteranceId: String? = null,
) {
    val context = LocalContext.current
    val speaker: TextToSpeechManager = koinInject()
    val speech by speaker.state.collectAsStateWithLifecycle()

    val id = utteranceId ?: remember(text) { "utterance-${text.hashCode()}" }
    val isSpeaking = speech.isSpeaking(id)

    IconActionButton(
        icon = if (isSpeaking) BraillyIcons.Stop else BraillyIcons.Speaker,
        contentDescription = when {
            isSpeaking -> "Hentikan pembacaan"
            !speech.isReady -> "$contentDescription, mesin suara belum siap"
            else -> contentDescription
        },
        onClick = {
            if (speech.isReady) {
                speaker.toggle(text, id)
            } else {
                // Teksnya diantrekan dan penyiapan mesin dicoba ulang, lalu
                // kendalanya disebutkan. Toast dipakai, bukan tulisan di
                // layar, karena tombol ini muncul di banyak tempat dan
                // tidak semuanya punya ruang untuk pesan menetap.
                speaker.speak(text, id)
                Toast.makeText(
                    context,
                    speech.problem ?: "Mesin suara sedang disiapkan, coba lagi sebentar.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        },
        modifier = modifier,
        enabled = text.isNotBlank(),
        tint = if (isSpeaking) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.primary
        },
        container = if (isSpeaking) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    )
}
