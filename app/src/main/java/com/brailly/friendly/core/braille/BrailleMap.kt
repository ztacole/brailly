package com.brailly.friendly.core.braille

/**
 * Peta Braille Grade 1 (uncontracted) untuk huruf Latin a-z, angka, dan
 * tanda baca dasar. Cukup untuk kebutuhan MVP terjemah dua arah.
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

    /** Tanda baca dasar. */
    val punctuation: Map<Char, BrailleCell> = mapOf(
        ',' to BrailleCell.of(2),
        ';' to BrailleCell.of(2, 3),
        ':' to BrailleCell.of(2, 5),
        '.' to BrailleCell.of(2, 5, 6),
        '?' to BrailleCell.of(2, 3, 6),
        '!' to BrailleCell.of(2, 3, 5),
        '\'' to BrailleCell.of(3),
        '-' to BrailleCell.of(3, 6),
        '(' to BrailleCell.of(1, 2, 6),
        ')' to BrailleCell.of(3, 4, 5),
        '/' to BrailleCell.of(3, 4),
    )

    /** Indikator khusus. */
    val NUMBER_SIGN = BrailleCell.of(3, 4, 5, 6)
    val CAPITAL_SIGN = BrailleCell.of(6)

    /** Peta gabungan cell -> huruf latin (untuk arah Braille -> Text). */
    val cellToLetter: Map<Set<Int>, Char> =
        letters.entries.associate { (c, cell) -> cell.dots to c }

    fun letterFor(cell: BrailleCell): Char? = cellToLetter[cell.dots]
}
