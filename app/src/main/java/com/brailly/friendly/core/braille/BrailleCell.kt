package com.brailly.friendly.core.braille

/**
 * Representasi satu sel Braille standar 6 titik.
 * Penomoran titik (Perkins Brailler):
 *   1  4
 *   2  5
 *   3  6
 *
 * [dots] berisi nomor titik yang aktif (menonjol).
 */
data class BrailleCell(val dots: Set<Int>) {

    /** True jika titik [n] (1..6) aktif. */
    fun has(n: Int): Boolean = dots.contains(n)

    /** Karakter Unicode Braille (blok U+2800). */
    val unicodeChar: Char
        get() {
            var pattern = 0
            // Peta nomor titik -> bit sesuai standar Unicode Braille
            val bitFor = mapOf(1 to 0, 2 to 1, 3 to 2, 4 to 3, 5 to 4, 6 to 5)
            for (dot in dots) {
                bitFor[dot]?.let { pattern = pattern or (1 shl it) }
            }
            return (0x2800 + pattern).toChar()
        }

    /**
     * Bitmask titik: bit 0 = titik 1, ... bit 5 = titik 6. Konvensi yang sama
     * dipakai model pemindaian dan blok Unicode Braille.
     */
    val mask: Int
        get() {
            var m = 0
            for (dot in dots) if (dot in 1..6) m = m or (1 shl (dot - 1))
            return m
        }

    companion object {
        val EMPTY = BrailleCell(emptySet())

        fun of(vararg dots: Int): BrailleCell = BrailleCell(dots.toSet())

        /** Bentuk sel dari bitmask titik (kebalikan [mask]). */
        fun ofMask(mask: Int): BrailleCell {
            val dots = mutableSetOf<Int>()
            for (bit in 0 until 6) if (mask and (1 shl bit) != 0) dots.add(bit + 1)
            return BrailleCell(dots)
        }
    }
}
