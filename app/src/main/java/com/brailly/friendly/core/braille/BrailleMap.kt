package com.brailly.friendly.core.braille

/**
 * Peta Braille Grade 1 (tanpa singkatan) untuk huruf Latin a-z, angka, dan
 * tanda baca, mengikuti profil Braille Indonesia.
 *
 * Tanda bacanya mengikuti standar Braille internasional: deret tanda baca
 * diturunkan dari pola huruf a-j yang digeser satu baris ke bawah, sedangkan
 * apostrof dan tanda hubung digeser dua posisi.
 *
 * Dua pola dipakai ganda dan dibedakan dari posisinya dalam kata:
 *   - titik 236 : tanda tanya sekaligus kutip buka
 *   - titik 2356: kurung buka sekaligus kurung tutup
 * Penguraiannya ditangani [BrailleTranslator].
 */
object BrailleMap {

    /** Huruf a-z -> sel braille. */
    val letters: Map<Char, BrailleCell> = mapOf(
        'a' to BrailleCell.of(1),
        'b' to BrailleCell.of(1, 2),
        'c' to BrailleCell.of(1, 4),
        'd' to BrailleCell.of(1, 4, 5),
        'e' to BrailleCell.of(1, 5),
        'f' to BrailleCell.of(1, 2, 4),
        'g' to BrailleCell.of(1, 2, 4, 5),
        'h' to BrailleCell.of(1, 2, 5),
        'i' to BrailleCell.of(2, 4),
        'j' to BrailleCell.of(2, 4, 5),
        'k' to BrailleCell.of(1, 3),
        'l' to BrailleCell.of(1, 2, 3),
        'm' to BrailleCell.of(1, 3, 4),
        'n' to BrailleCell.of(1, 3, 4, 5),
        'o' to BrailleCell.of(1, 3, 5),
        'p' to BrailleCell.of(1, 2, 3, 4),
        'q' to BrailleCell.of(1, 2, 3, 4, 5),
        'r' to BrailleCell.of(1, 2, 3, 5),
        's' to BrailleCell.of(2, 3, 4),
        't' to BrailleCell.of(2, 3, 4, 5),
        'u' to BrailleCell.of(1, 3, 6),
        'v' to BrailleCell.of(1, 2, 3, 6),
        'w' to BrailleCell.of(2, 4, 5, 6),
        'x' to BrailleCell.of(1, 3, 4, 6),
        'y' to BrailleCell.of(1, 3, 4, 5, 6),
        'z' to BrailleCell.of(1, 3, 5, 6),
    )

    /** Angka 0-9 memakai pola huruf a-j (didahului tanda angka). */
    val digits: Map<Char, BrailleCell> = mapOf(
        '1' to letters['a']!!,
        '2' to letters['b']!!,
        '3' to letters['c']!!,
        '4' to letters['d']!!,
        '5' to letters['e']!!,
        '6' to letters['f']!!,
        '7' to letters['g']!!,
        '8' to letters['h']!!,
        '9' to letters['i']!!,
        '0' to letters['j']!!,
    )

    /** Urutan digit menurut huruf: indeks = nilai digit. */
    const val DIGIT_LETTERS = "jabcdefghi"

    /**
     * Tanda baca. Untuk arah teks -> braille, kunci dipakai apa adanya.
     * Untuk arah sebaliknya gunakan [BrailleTranslator] karena ada pola ganda.
     */
    val punctuation: Map<Char, BrailleCell> = mapOf(
        ',' to BrailleCell.of(2),
        ';' to BrailleCell.of(2, 3),
        ':' to BrailleCell.of(2, 5),
        '.' to BrailleCell.of(2, 5, 6),
        '?' to BrailleCell.of(2, 3, 6),
        '!' to BrailleCell.of(2, 3, 5),
        '\'' to BrailleCell.of(3),
        '-' to BrailleCell.of(3, 6),
        '/' to BrailleCell.of(3, 4),
        '*' to BrailleCell.of(3, 5),
        '(' to BrailleCell.of(2, 3, 5, 6),
        ')' to BrailleCell.of(2, 3, 5, 6),
        '\u201C' to BrailleCell.of(2, 3, 6),
        '\u201D' to BrailleCell.of(3, 5, 6),
    )

    /**
     * Tanda baca yang boleh berada di tengah angka dan tidak mengakhiri mode
     * angka: koma desimal dan titik pemisah ribuan.
     */
    val numberPunctuation: Map<Char, BrailleCell> = mapOf(
        ',' to BrailleCell.of(2),
        '.' to BrailleCell.of(3),
    )

    /** Indikator khusus. */
    val NUMBER_SIGN = BrailleCell.of(3, 4, 5, 6)
    val CAPITAL_SIGN = BrailleCell.of(6)

    /** Tanda huruf: mengakhiri mode angka bila disusul huruf a-j. */
    val LETTER_SIGN = BrailleCell.of(5, 6)

    /** Pola ganda yang perlu diurai dari konteks. */
    val QUOTE_OPEN = BrailleCell.of(2, 3, 6)
    val QUOTE_CLOSE = BrailleCell.of(3, 5, 6)
    val PAREN = BrailleCell.of(2, 3, 5, 6)

    /** Peta sel -> huruf latin (untuk arah Braille -> Text). */
    val cellToLetter: Map<Set<Int>, Char> =
        letters.entries.associate { (c, cell) -> cell.dots to c }

    /** Peta mask -> huruf latin, varian cepat untuk jalur pemindaian. */
    val maskToLetter: Map<Int, Char> =
        letters.entries.associate { (c, cell) -> cell.mask to c }

    /**
     * Peta mask -> tanda baca. Pola ganda dipetakan ke makna bawaannya
     * (titik 236 -> '?', titik 2356 -> '('); [BrailleTranslator] yang
     * membetulkannya menurut posisi.
     */
    val maskToPunctuation: Map<Int, Char> = buildMap {
        for ((ch, cell) in punctuation) putIfAbsent(cell.mask, ch)
    }

    val maskToNumberPunctuation: Map<Int, Char> =
        numberPunctuation.entries.associate { (c, cell) -> cell.mask to c }

    // ------------------------------------------------------------- kamus belajar
    /**
     * Satu entri kamus Braille untuk ditampilkan pada menu Belajar: pola sel,
     * lambang/tulisan, dan deskripsi singkat (dibacakan screen reader).
     */
    data class DictionaryEntry(
        val symbol: String,
        val cell: BrailleCell,
        val description: String,
    )

    /**
     * Kamus angka 0-9. Dalam penulisan Braille, angka didahului [NUMBER_SIGN],
     * namun di kamus kita tampilkan sel digit-nya saja agar mudah dikenali.
     */
    val numberDictionary: List<DictionaryEntry> =
        listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0').map { d ->
            DictionaryEntry(
                symbol = d.toString(),
                cell = digits.getValue(d),
                description = "Angka $d (didahului tanda angka)",
            )
        }

    /** Nama tanda baca & simbol untuk deskripsi aksesibilitas pada kamus. */
    private val symbolNames: Map<Char, String> = mapOf(
        ',' to "koma",
        ';' to "titik koma",
        ':' to "titik dua",
        '.' to "titik",
        '?' to "tanda tanya",
        '!' to "tanda seru",
        '\'' to "apostrof",
        '-' to "tanda hubung",
        '/' to "garis miring",
        '*' to "tanda bintang",
        '(' to "kurung buka",
        ')' to "kurung tutup",
        '\u201C' to "kutip buka",
        '\u201D' to "kutip tutup",
    )

    /**
     * Kamus simbol & tanda baca. Urutannya mengikuti [symbolNames] agar stabil;
     * pola ganda (mis. kurung) tetap ditampilkan sesuai lambangnya.
     */
    val symbolDictionary: List<DictionaryEntry> =
        symbolNames.mapNotNull { (ch, name) ->
            val cell = punctuation[ch] ?: return@mapNotNull null
            DictionaryEntry(symbol = ch.toString(), cell = cell, description = name)
        }

    /** Kamus khusus penanda (tanda angka, tanda kapital, tanda huruf). */
    val indicatorDictionary: List<DictionaryEntry> = listOf(
        DictionaryEntry("#", NUMBER_SIGN, "tanda angka"),
        DictionaryEntry("^", CAPITAL_SIGN, "tanda kapital"),
        DictionaryEntry("_", LETTER_SIGN, "tanda huruf"),
    )

    fun letterFor(cell: BrailleCell): Char? = cellToLetter[cell.dots]

    /**
     * Apakah [mask] merupakan pola yang dikenal: huruf, tanda baca, atau
     * penanda. Dipakai menilai kewajaran hasil deteksi -- halaman yang terbaca
     * pada orientasi salah menghasilkan banyak pola tak dikenal.
     */
    fun isKnown(mask: Int): Boolean =
        maskToLetter.containsKey(mask) || maskToPunctuation.containsKey(mask) ||
            mask == NUMBER_SIGN.mask || mask == CAPITAL_SIGN.mask || mask == LETTER_SIGN.mask
}
