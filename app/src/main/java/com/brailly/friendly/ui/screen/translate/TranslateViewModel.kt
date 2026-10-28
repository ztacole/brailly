package com.brailly.friendly.ui.screen.translate

import androidx.lifecycle.ViewModel
import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleToken
import com.brailly.friendly.core.braille.BrailleTranslator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TranslateMode { TEXT_TO_BRAILLE, BRAILLE_TO_TEXT }

data class TranslateUiState(
    val mode: TranslateMode = TranslateMode.TEXT_TO_BRAILLE,
    // Text -> Braille
    val input: String = "",
    val tokens: List<BrailleToken> = emptyList(),
    val unicode: String = "",
    // Braille -> Text
    val activeDots: Set<Int> = emptySet(),
    val committedCells: List<BrailleCell> = emptyList(),
    val decodedText: String = "",
)

class TranslateViewModel(
    private val translator: BrailleTranslator,
) : ViewModel() {

    private val _state = MutableStateFlow(TranslateUiState())
    val state: StateFlow<TranslateUiState> = _state.asStateFlow()

    fun setMode(mode: TranslateMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    // --- Text -> Braille ---
    fun onInputChange(text: String) {
        _state.value = _state.value.copy(
            input = text,
            tokens = translator.textToBraille(text),
            unicode = translator.textToUnicode(text),
        )
    }

    /**
     * Sambungkan hasil dikte ke teks yang sudah ada.
     *
     * Disambung, bukan ditimpa: pengguna sering mendiktekan kalimat demi
     * kalimat, dan menimpa isi kolom akan menghapus hasil dikte sebelumnya
     * tanpa cara mengembalikannya.
     */
    fun appendDictation(text: String) {
        val addition = text.trim()
        if (addition.isEmpty()) return
        val current = _state.value.input.trimEnd()
        onInputChange(if (current.isEmpty()) addition else "$current $addition")
    }

    // --- Braille -> Text ---
    fun toggleDot(dot: Int) {
        val current = _state.value.activeDots
        val next = if (current.contains(dot)) current - dot else current + dot
        _state.value = _state.value.copy(activeDots = next)
    }

    /** Konfirmasi kombinasi titik saat ini menjadi satu sel. */
    fun commitCell() {
        val dots = _state.value.activeDots
        if (dots.isEmpty()) return
        val cells = _state.value.committedCells + BrailleCell(dots)
        _state.value = _state.value.copy(
            committedCells = cells,
            activeDots = emptySet(),
            decodedText = translator.brailleToText(cells),
        )
    }

    fun commitSpace() {
        val cells = _state.value.committedCells + BrailleCell.EMPTY
        _state.value = _state.value.copy(
            committedCells = cells,
            activeDots = emptySet(),
            decodedText = translator.brailleToText(cells),
        )
    }

    fun deleteLastCell() {
        val cells = _state.value.committedCells.dropLast(1)
        _state.value = _state.value.copy(
            committedCells = cells,
            decodedText = translator.brailleToText(cells),
        )
    }

    fun clear() {
        _state.value = TranslateUiState(mode = _state.value.mode)
    }
}
