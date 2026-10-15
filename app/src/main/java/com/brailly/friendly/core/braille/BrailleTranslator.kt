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
 * Penerjemah dua arah Latin <-> Braille (Grade 1, profil Braille Indonesia).
 *
 * Arah Braille -> Latin diuraikan per kata, karena beberapa aturan hanya bisa
 * ditentukan dari posisi di dalam kata: tanda kapital ganda menandai seluruh
 * kata kapital, titik 236 bisa berarti tanda tanya atau kutip buka, dan titik
 * 2356 bisa berarti kurung buka atau tutup.
 *
 * Perilakunya disetarakan dengan implementasi acuan pada `brailly/braille.py`
 * dan diuji terhadap fixture yang dihasilkan darinya.
 */
class BrailleTranslator {

    // ----------------------------------------------------------- teks -> braille
    /**
     * Terjemah teks Latin menjadi daftar token braille, lengkap dengan tanda
     * kapital, tanda angka, dan tanda huruf.
     */
    fun textToBraille(text: String): List<BrailleToken> {
        val tokens = mutableListOf<BrailleToken>()
        for ((index, word) in text.split(' ', '\n', '\t').withIndex()) {
            if (index > 0) tokens.add(BrailleToken.Space)
            if (word.isEmpty()) continue
            encodeWord(word, tokens)
        }
        return tokens
    }

    private fun encodeWord(word: String, target: MutableList<BrailleToken>) {
        // Dikumpulkan per kata, karena tanda kutip ditentukan dari posisinya di
        // dalam kata: di awal berarti kutip buka, selainnya kutip tutup.
        val out = mutableListOf<BrailleToken>()
        // Kata yang seluruhnya kapital ditandai dua kali tanda kapital.
        val wordCaps = word.length > 1 && word.all { it.isLetter() } && word.all { it.isUpperCase() }
        if (wordCaps) {
            out.add(BrailleToken.Cell(BrailleMap.CAPITAL_SIGN, '^'))
            out.add(BrailleToken.Cell(BrailleMap.CAPITAL_SIGN, '^'))
        }
        var inNumber = false
        for ((i, ch) in word.withIndex()) {
            val next = if (i + 1 < word.length) word[i + 1] else null
            when {
                ch.isDigit() -> {
                    if (!inNumber) {
                        out.add(BrailleToken.Cell(BrailleMap.NUMBER_SIGN, '#'))
                        inNumber = true
                    }
                    BrailleMap.digits[ch]?.let { out.add(BrailleToken.Cell(it, ch)) }
                }

                inNumber && BrailleMap.numberPunctuation.containsKey(ch) && next?.isDigit() == true -> {
                    out.add(BrailleToken.Cell(BrailleMap.numberPunctuation.getValue(ch), ch))
                }

                BrailleMap.letters.containsKey(ch.lowercaseChar()) -> {
                    val lower = ch.lowercaseChar()
                    // Tanda huruf diperlukan bila huruf a-j menyusul angka,
                    // agar tidak terbaca sebagai digit.
                    if (inNumber && BrailleMap.DIGIT_LETTERS.contains(lower)) {
                        out.add(BrailleToken.Cell(BrailleMap.LETTER_SIGN, '_'))
                    }
                    inNumber = false
                    if (ch.isUpperCase() && !wordCaps) {
                        out.add(BrailleToken.Cell(BrailleMap.CAPITAL_SIGN, '^'))
                    }
                    out.add(BrailleToken.Cell(BrailleMap.letters.getValue(lower), ch))
                }

                else -> {
                    inNumber = false
                    val resolved = if (ch == '"') {
                        if (out.isEmpty()) '\u201C' else '\u201D'
                    } else ch
                    BrailleMap.punctuation[resolved]?.let { out.add(BrailleToken.Cell(it, ch)) }
                }
            }
        }
        target.addAll(out)
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

    // ----------------------------------------------------------- braille -> teks
    /**
     * Terjemah rangkaian sel braille menjadi teks Latin. Sel `null` atau kosong
     * dianggap spasi (pembatas kata).
     */
    fun brailleToText(cells: List<BrailleCell?>): String =
        decodeMasks(cells.map { it?.mask ?: 0 })

    /**
     * Varian untuk jalur pemindaian: bitmask titik per sel, dengan nilai <= 0
     * sebagai spasi.
     */
    fun decodeMasks(masks: List<Int>): String {
        val words = mutableListOf<String>()
        val current = mutableListOf<Int>()
        for (m in masks + listOf(0)) {
            if (m <= 0) {
                if (current.isNotEmpty()) {
                    words.add(decodeWord(current))
                    current.clear()
                }
            } else {
                current.add(m)
            }
        }
        return words.joinToString(" ")
    }

    /** Uraikan satu kata (tanpa spasi di dalamnya). */
    fun decodeWord(cells: List<Int>): String {
        val out = StringBuilder()
        var inNumber = false
        var capNext = false
        var capWord = false
        val n = cells.size
        var i = 0
        while (i < n) {
            val m = cells[i]
            if (m == CAP) {
                if (i + 1 < n && cells[i + 1] == CAP) {
                    capWord = true; i += 2; continue
                }
                // Titik 6 yang berdiri sendiri di akhir kata bukan tanda kapital.
                if (i + 1 < n) {
                    capNext = true; i += 1; continue
                }
            }
            if (m == NUM) {
                inNumber = true; i += 1; continue
            }
            if (m == LET && i + 1 < n) {
                inNumber = false; i += 1; continue
            }
            if (inNumber) {
                val letter = BrailleMap.maskToLetter[m]
                if (letter != null && BrailleMap.DIGIT_LETTERS.contains(letter)) {
                    out.append(BrailleMap.DIGIT_LETTERS.indexOf(letter).toString())
                    i += 1; continue
                }
                val sep = BrailleMap.maskToNumberPunctuation[m]
                if (sep != null && i + 1 < n && nextIsDigitLetter(cells[i + 1])) {
                    out.append(sep); i += 1; continue
                }
                inNumber = false
            }
            val letter = BrailleMap.maskToLetter[m]
            if (letter != null) {
                out.append(if (capNext || capWord) letter.uppercaseChar() else letter)
                capNext = false
                i += 1; continue
            }
            capNext = false
            out.append(resolvePunctuation(m, out, cells, i))
            i += 1
        }
        return out.toString()
    }

    /**
     * Tentukan arti pola yang dipakai ganda dari posisinya: kutip buka dan
     * kurung buka hanya mungkin bila belum ada huruf sebelumnya dan masih ada
     * huruf sesudahnya di dalam kata yang sama.
     */
    private fun resolvePunctuation(mask: Int, out: StringBuilder, cells: List<Int>, i: Int): Char {
        val lettersBefore = out.any { it.isLetterOrDigit() }
        var lettersAfter = false
        for (k in i + 1 until cells.size) {
            if (BrailleMap.maskToLetter.containsKey(cells[k]) || cells[k] == NUM) {
                lettersAfter = true; break
            }
        }
        val opening = !lettersBefore && lettersAfter
        return when (mask) {
            QUOTE_OPEN -> if (opening) '\u201C' else '?'
            PAREN -> if (opening) '(' else ')'
            else -> BrailleMap.maskToPunctuation[mask] ?: unicodeOf(mask)
        }
    }

    /** Pola yang tak dikenal ditampilkan sebagai karakter Braille Unicode. */
    private fun unicodeOf(mask: Int): Char = (0x2800 + (mask and 0x3F)).toChar()

    /**
     * Setara `letter_of.get(next, "") in DIGIT_LETTERS` pada implementasi acuan:
     * sel yang bukan huruf pun lolos, karena string kosong dianggap termuat.
     * Dipertahankan agar hasilnya sama persis dengan acuan.
     */
    private fun nextIsDigitLetter(mask: Int): Boolean {
        val letter = BrailleMap.maskToLetter[mask] ?: return true
        return BrailleMap.DIGIT_LETTERS.contains(letter)
    }

    private companion object {
        val CAP = BrailleMap.CAPITAL_SIGN.mask
        val NUM = BrailleMap.NUMBER_SIGN.mask
        val LET = BrailleMap.LETTER_SIGN.mask
        val QUOTE_OPEN = BrailleMap.QUOTE_OPEN.mask
        val PAREN = BrailleMap.PAREN.mask
    }
}
