package com.brailly.friendly.ui.screen.translate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.data.speech.DictationRecognizer
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import org.koin.compose.koinInject

/**
 * Tombol dikte beserta keterangan keadaannya.
 *
 * Hasil ucapan dikirim ke [onDictated] sebagai teks utuh. Pemanggil yang
 * memutuskan teks itu menggantikan atau menyambung isi kolom, karena hanya
 * pemanggil yang tahu apa yang sudah ada di sana.
 *
 * Mikrofon hanya menyala selama satu ucapan: layanan pengenalan suara sistem
 * menutup sesinya sendiri ketika pengguna berhenti berbicara.
 */
@Composable
fun DictationControls(
    onDictated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recognizer: DictationRecognizer = koinInject()
    val dictation by recognizer.state.collectAsStateWithLifecycle()
    val latestOnDictated = rememberUpdatedState(onDictated)

    /** Ditandai saat pengguna menolak izin, agar penjelasannya menetap. */
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
        if (granted) recognizer.start()
    }

    LaunchedEffect(recognizer) {
        recognizer.results.collect { text -> latestOnDictated.value(text) }
    }

    // Mikrofon dilepas saat layar Terjemah ditinggalkan. Sesi yang dibiarkan
    // terbuka menahan mikrofon dan membuat aplikasi lain gagal merekam.
    DisposableEffect(recognizer) {
        onDispose { recognizer.cancel() }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconActionButton(
                icon = if (dictation.isBusy) BraillyIcons.Stop else BraillyIcons.Mic,
                contentDescription = if (dictation.isBusy) {
                    "Hentikan dikte"
                } else {
                    "Diktekan teks dengan suara"
                },
                onClick = {
                    permissionDenied = false
                    when {
                        dictation.isBusy -> recognizer.cancel()

                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED -> recognizer.start()

                        // Layanan pengenalnya tidak ada: memulai tetap
                        // dilakukan supaya alasannya muncul, tanpa lebih dulu
                        // meminta akses mikrofon yang tidak akan terpakai.
                        !recognizer.isAvailable -> recognizer.start()

                        else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                tint = if (dictation.isBusy) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                container = if (dictation.isBusy) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )

            if (dictation.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                Icon(
                    BraillyIcons.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = when {
                    dictation.isListening -> "Mendengarkan, silakan bicara..."
                    dictation.isBusy -> "Mengubah ucapan menjadi teks..."
                    else -> "Diktekan teks dengan suara"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Perubahan status diumumkan pembaca layar tanpa pengguna
                // perlu menjelajah ulang isi layar.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (dictation.partialText.isNotBlank()) {
            Text(
                "\u201c${dictation.partialText}\u201d",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val problem = dictation.errorMessage
            ?: "Izin mikrofon diperlukan untuk mendiktekan teks.".takeIf { permissionDenied }
        if (problem != null) {
            Text(
                problem,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
    }
}
