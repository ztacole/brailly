package com.brailly.friendly.ui.screen.translate

import android.content.Context
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.brailly.friendly.ui.component.SpeakButton
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsDim
import com.brailly.friendly.ui.designsystem.DsTextField
import com.brailly.friendly.util.copyToClipboard
import com.brailly.friendly.util.shareText
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
            .padding(DsDim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = "Terjemah", subtitle = "Braille dua arah")

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.mode == TranslateMode.TEXT_TO_BRAILLE,
                onClick = { viewModel.setMode(TranslateMode.TEXT_TO_BRAILLE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                // Gaya label disebut eksplisit: `labelLarge` kini 17sp Black,
                // dan "Teks ke Braille" tidak muat di separuh lebar layar.
                //
                // Dipakai kata "ke", bukan tanda panah: Fredoka dan Varela
                // Round hanya mencakup subset latin, sehingga U+2192 akan
                // dipinjam dari huruf sistem dan terlihat berbeda wajah.
                label = { Text("Teks ke Braille", style = Ds.text.meta, maxLines = 1) },
            )
            SegmentedButton(
                selected = state.mode == TranslateMode.BRAILLE_TO_TEXT,
                onClick = { viewModel.setMode(TranslateMode.BRAILLE_TO_TEXT) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Braille ke Teks", style = Ds.text.meta, maxLines = 1) },
            )
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
    DsTextField(
        value = state.input,
        onValueChange = viewModel::onInputChange,
        modifier = Modifier.fillMaxWidth(),
        label = "Masukkan teks",
        placeholder = "Contoh: Halo",
        minLines = 2,
    )

    DictationControls(
        onDictated = viewModel::appendDictation,
        modifier = Modifier.fillMaxWidth(),
    )

    Text("Hasil Braille", style = Ds.text.sectionHeader, color = MaterialTheme.colorScheme.onBackground)
    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        BrailleTextView(tokens = state.tokens, modifier = Modifier.fillMaxWidth())
    }

    if (state.unicode.isNotBlank()) {
        BraillyCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Column {
                Text("Unicode Braille", style = Ds.text.labelUpper, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    state.unicode,
                    style = Ds.text.brailleUnicode,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconActionButton(BraillyIcons.Copy, "Salin hasil braille", {
            context.copyToClipboard(state.unicode, confirmation = "Braille disalin")
        })
        IconActionButton(BraillyIcons.Share, "Bagikan hasil braille", {
            context.shareText(state.unicode)
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
        style = Ds.text.helper,
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
            context.copyToClipboard(state.decodedText)
        })
        IconActionButton(BraillyIcons.Share, "Bagikan hasil teks", {
            context.shareText(state.decodedText)
        })
        SpeakButton(text = state.decodedText, contentDescription = "Bacakan hasil teks")
    }

    Text("Hasil Teks", style = Ds.text.sectionHeader, color = MaterialTheme.colorScheme.onBackground)
    BraillyCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            state.decodedText.ifBlank { "Hasil terjemahan akan muncul di sini" },
            style = Ds.text.display,
            color = if (state.decodedText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}
