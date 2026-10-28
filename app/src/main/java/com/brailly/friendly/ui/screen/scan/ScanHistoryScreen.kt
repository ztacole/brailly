package com.brailly.friendly.ui.screen.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.brailly.friendly.data.scan.ScanHistoryEntity
import com.brailly.friendly.data.speech.TextToSpeechManager
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.component.SpeakButton
import com.brailly.friendly.util.asHistoryTimestamp
import com.brailly.friendly.util.copyToClipboard
import com.brailly.friendly.util.shareText
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Riwayat hasil pemindaian braille.
 *
 * Daftarnya dimuat bertahap lewat Paging 3, jadi membuka layar ini tidak
 * menarik seluruh teks halaman yang pernah dipindai ke memori. Setiap entri
 * bisa dibacakan, disalin, dibagikan, dan dihapus.
 */
@Composable
fun ScanHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanHistoryViewModel = koinViewModel(),
) {
    val speaker: TextToSpeechManager = koinInject()
    val entries = viewModel.entries.collectAsLazyPagingItems()
    val count by viewModel.count.collectAsStateWithLifecycle()

    var expandedId by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<ScanHistoryEntity?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            HistoryHeader(
                count = count,
                onBack = onBack,
                onClearAll = { confirmClearAll = true },
            )
        }

        val refresh = entries.loadState.refresh
        when {
            refresh is LoadState.Loading && entries.itemCount == 0 -> {
                item(key = "loading") { CenteredProgress("Memuat riwayat...") }
            }

            refresh is LoadState.Error -> {
                item(key = "error") {
                    HistoryMessageCard(
                        title = "Riwayat tidak dapat dibaca",
                        body = refresh.error.message ?: "Terjadi kesalahan pada basis data.",
                        isError = true,
                        action = { PrimaryButton("Coba Lagi", entries::retry, Modifier.fillMaxWidth()) },
                    )
                }
            }

            entries.itemCount == 0 -> {
                item(key = "empty") {
                    HistoryMessageCard(
                        title = "Belum ada riwayat",
                        body = "Hasil pemindaian akan tersimpan di sini secara otomatis, " +
                            "sehingga bisa dibaca ulang tanpa memindai lagi.",
                    )
                }
            }
        }

        items(
            count = entries.itemCount,
            key = entries.itemKey { it.id },
        ) { index ->
            // Entri bisa null sesaat saat Paging menukar halaman yang sudah
            // tidak berlaku, misalnya setelah satu entri dihapus.
            val entry = entries[index] ?: return@items
            HistoryItem(
                entry = entry,
                expanded = expandedId == entry.id,
                onToggleExpand = {
                    expandedId = if (expandedId == entry.id) null else entry.id
                },
                onDelete = { pendingDelete = entry },
            )
        }

        if (entries.loadState.append is LoadState.Loading) {
            item(key = "append") { CenteredProgress("Memuat entri berikutnya...") }
        }
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = "Hapus entri ini?",
            body = "Teks hasil pemindaian \u201c${entry.text.previewLine()}\u201d akan " +
                "dihapus dan tidak dapat dikembalikan.",
            confirmLabel = "Hapus",
            onConfirm = {
                // Bacaan yang sedang berjalan dihentikan supaya suara tidak
                // melanjutkan teks dari entri yang sudah tidak ada.
                speaker.stop()
                if (expandedId == entry.id) expandedId = null
                viewModel.delete(entry.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (confirmClearAll) {
        ConfirmDialog(
            title = "Hapus semua riwayat?",
            body = "Seluruh $count hasil pemindaian akan dihapus dan tidak dapat " +
                "dikembalikan.",
            confirmLabel = "Hapus Semua",
            onConfirm = {
                speaker.stop()
                expandedId = null
                viewModel.clearAll()
                confirmClearAll = false
            },
            onDismiss = { confirmClearAll = false },
        )
    }
}

@Composable
private fun HistoryHeader(
    count: Int,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconActionButton(
            icon = BraillyIcons.Back,
            contentDescription = "Kembali ke scan",
            onClick = onBack,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Riwayat Scan",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (count == 0) "Belum ada hasil tersimpan" else "$count hasil tersimpan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (count > 0) {
            IconActionButton(
                icon = BraillyIcons.Trash,
                contentDescription = "Hapus semua riwayat",
                onClick = onClearAll,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun HistoryItem(
    entry: ScanHistoryEntity,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val truncated = entry.text.lineCount() > COLLAPSED_LINES

    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (truncated) onToggleExpand else null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    entry.scannedAt.asHistoryTimestamp(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.doubtful) {
                    Text(
                        "Hasil diragukan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Text(
                entry.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                overflow = TextOverflow.Ellipsis,
            )

            if (truncated) {
                Text(
                    if (expanded) "Ringkas" else "Lihat selengkapnya",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                "Keyakinan ${(entry.confidence * 100).toInt()}% \u00b7 " +
                    "${entry.cellCount} sel \u00b7 ${entry.paragraphCount} paragraf",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpeakButton(
                    text = entry.text,
                    contentDescription = "Bacakan hasil scan ini",
                    // Penanda dibuat dari id entri, bukan dari teksnya, supaya
                    // dua entri dengan teks sama tidak saling menandai sebagai
                    // "sedang dibacakan".
                    utteranceId = entryUtteranceId(entry.id),
                )
                IconActionButton(
                    icon = BraillyIcons.Copy,
                    contentDescription = "Salin teks hasil scan ini",
                    onClick = {
                        context.copyToClipboard(entry.text, confirmation = "Teks riwayat disalin")
                    },
                )
                IconActionButton(
                    icon = BraillyIcons.Share,
                    contentDescription = "Bagikan teks hasil scan ini",
                    onClick = { context.shareText(entry.text) },
                )
                IconActionButton(
                    icon = BraillyIcons.Trash,
                    contentDescription = "Hapus entri riwayat ini",
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HistoryMessageCard(
    title: String,
    body: String,
    isError: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                BraillyIcons.History,
                contentDescription = null,
                tint = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(40.dp),
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            action?.invoke()
        }
    }
}

@Composable
private fun CenteredProgress(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        // Penghapusan tidak dapat dibatalkan, jadi tindakannya selalu lewat
        // konfirmasi dan tombol batal yang berada lebih dekat ke jempol.
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

/** Penanda pembacaan per entri riwayat. */
private fun entryUtteranceId(id: Long): String = "scan-history-$id"

private const val COLLAPSED_LINES = 4

private fun String.lineCount(): Int = count { it == '\n' } + 1

/** Cuplikan satu baris untuk dialog konfirmasi. */
private fun String.previewLine(): String {
    val firstLine = lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    return if (firstLine.length <= PREVIEW_LENGTH) {
        firstLine
    } else {
        firstLine.take(PREVIEW_LENGTH).trimEnd() + "\u2026"
    }
}

private const val PREVIEW_LENGTH = 40
