package com.brailly.friendly.core.braille

/**
 * Token hasil terjemahan teks -> braille. Setiap token merepresentasikan satu
 * sel braille yang bisa ditampilkan sebagai dot pattern, atau spasi.
 */
sealed interface BrailleToken {
    data class Cell(val cell: BrailleCell, val sourceChar: Char) : BrailleToken
    data object Space : BrailleToken
}

/**
 * Penerjemah dua arah Latin <-> Braille (Grade 1).
 */
class BrailleTranslator {

    /**
     * Terjemah teks Latin menjadi daftar token braille.
     * Menyisipkan tanda kapital (dot 6) dan tanda angka (dots 3-4-5-6) sesuai aturan.
     */
    fun textToBraille(text: String): List<BrailleToken> {
        val tokens = mutableListOf<BrailleToken>()
        var inNumberMode = false

        for (ch in text) {
            when {
                ch == ' ' || ch == '\n' || ch == '\t' -> {
                    tokens.add(BrailleToken.Space)
                    inNumberMode = false
                }

                ch.isDigit() -> {
                    if (!inNumberMode) {
                        tokens.add(BrailleToken.Cell(BrailleMap.NUMBER_SIGN, '#'))
                        inNumberMode = true
                    }
                    BrailleMap.digits[ch]?.let {
                        tokens.add(BrailleToken.Cell(it, ch))
                    }
                }

                ch.isLetter() -> {
                    inNumberMode = false
                    if (ch.isUpperCase()) {
                        tokens.add(BrailleToken.Cell(BrailleMap.CAPITAL_SIGN, '^'))
                    }
                    val lower = ch.lowercaseChar()
                    BrailleMap.letters[lower]?.let {
                        tokens.add(BrailleToken.Cell(it, ch))
                    }
                }

                else -> {
                    inNumberMode = false
                    BrailleMap.punctuation[ch]?.let {
                        tokens.add(BrailleToken.Cell(it, ch))
                    }
                }
            }
        }
        return tokens
    }

    /** Ubah teks menjadi rangkaian karakter Unicode Braille (untuk copy/share). */
    fun textToUnicode(text: String): String = buildString {
        for (token in textToBraille(text)) {
            when (token) {
                is BrailleToken.Cell -> append(token.cell.unicodeChar)
                BrailleToken.Space -> append(' ')
            }
        }
    }

    /**
     * Terjemah rangkaian sel braille menjadi teks Latin.
     * Menghormati tanda angka dan tanda kapital.
     */
    fun brailleToText(cells: List<BrailleCell?>): String = buildString {
        var inNumberMode = false
        var capitalNext = false

        for (cell in cells) {
            if (cell == null || cell.dots.isEmpty()) {
                append(' ')
                inNumberMode = false
                continue
            }
            when {
                cell.dots == BrailleMap.NUMBER_SIGN.dots -> inNumberMode = true
                cell.dots == BrailleMap.CAPITAL_SIGN.dots -> capitalNext = true
                inNumberMode -> {
                    val letter = BrailleMap.letterFor(cell)
                    val digit = digitFromLetter(letter)
                    if (digit != null) append(digit) else {
                        inNumberMode = false
                        appendLetterOrPunct(cell, capitalNext)
                        capitalNext = false
                    }
                }
                else -> {
                    appendLetterOrPunct(cell, capitalNext)
                    capitalNext = false
                }
            }
        }
    }

    private fun StringBuilder.appendLetterOrPunct(cell: BrailleCell, capital: Boolean) {
        val letter = BrailleMap.letterFor(cell)
        if (letter != null) {
            append(if (capital) letter.uppercaseChar() else letter)
            return
        }
        val punct = BrailleMap.punctuation.entries.firstOrNull { it.value.dots == cell.dots }?.key
        if (punct != null) append(punct)
    }

    private fun digitFromLetter(letter: Char?): Char? {
        if (letter == null) return null
        return BrailleMap.digits.entries.firstOrNull {
            it.value.dots == BrailleMap.letters[letter]?.dots
        }?.key
    }
}
