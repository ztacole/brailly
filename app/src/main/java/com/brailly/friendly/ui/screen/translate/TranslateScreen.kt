package com.brailly.friendly.ui.screen.translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.ui.component.BrailleKeypad
import com.brailly.friendly.ui.component.BrailleTextView
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.component.ScreenHeader
import com.brailly.friendly.ui.component.SecondaryButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TranslateScreen(
    modifier: Modifier = Modifier,
    viewModel: TranslateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = "Terjemah", subtitle = "Braille dua arah")

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.mode == TranslateMode.TEXT_TO_BRAILLE,
                onClick = { viewModel.setMode(TranslateMode.TEXT_TO_BRAILLE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Teks → Braille") }
            SegmentedButton(
                selected = state.mode == TranslateMode.BRAILLE_TO_TEXT,
                onClick = { viewModel.setMode(TranslateMode.BRAILLE_TO_TEXT) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Braille → Teks") }
        }

        when (state.mode) {
            TranslateMode.TEXT_TO_BRAILLE -> TextToBraille(state, viewModel, context)
            TranslateMode.BRAILLE_TO_TEXT -> BrailleToText(state, viewModel, context)
        }
    }
}

@Composable
private fun TextToBraille(
    state: TranslateUiState,
    viewModel: TranslateViewModel,
    context: Context,
) {
    OutlinedTextField(
        value = state.input,
        onValueChange = viewModel::onInputChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Masukkan teks") },
        placeholder = { Text("Contoh: Halo") },
        shape = MaterialTheme.shapes.medium,
        minLines = 2,
    )

    Text("Hasil Braille", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        BrailleTextView(tokens = state.tokens, modifier = Modifier.fillMaxWidth())
    }

    if (state.unicode.isNotBlank()) {
        BraillyCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Column {
                Text("Unicode Braille", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    state.unicode,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconActionButton(BraillyIcons.Copy, "Salin hasil braille", {
            if (state.unicode.isNotBlank()) {
                copyToClipboard(context, state.unicode)
                Toast.makeText(context, "Braille disalin", Toast.LENGTH_SHORT).show()
            }
        })
        IconActionButton(BraillyIcons.Share, "Bagikan hasil braille", {
            if (state.unicode.isNotBlank()) shareText(context, state.unicode)
        })
        IconActionButton(BraillyIcons.Close, "Bersihkan teks", viewModel::clear)
    }
}

@Composable
private fun BrailleToText(
    state: TranslateUiState,
    viewModel: TranslateViewModel,
    context: Context,
) {
    Text(
        "Tekan kombinasi titik lalu tambahkan sebagai huruf",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        BrailleKeypad(active = state.activeDots, onToggle = viewModel::toggleDot)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        PrimaryButton("Tambah huruf", viewModel::commitCell, Modifier.weight(1f), leadingIcon = BraillyIcons.Check)
        SecondaryButton("Spasi", viewModel::commitSpace, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconActionButton(BraillyIcons.Back, "Hapus huruf terakhir", viewModel::deleteLastCell)
        IconActionButton(BraillyIcons.Close, "Bersihkan semua", viewModel::clear)
        IconActionButton(BraillyIcons.Copy, "Salin hasil teks", {
            if (state.decodedText.isNotBlank()) {
                copyToClipboard(context, state.decodedText)
                Toast.makeText(context, "Teks disalin", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Text("Hasil Teks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    BraillyCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            state.decodedText.ifBlank { "Hasil terjemahan akan muncul di sini" },
            style = MaterialTheme.typography.headlineMedium,
            color = if (state.decodedText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Brailly", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan"))
}
