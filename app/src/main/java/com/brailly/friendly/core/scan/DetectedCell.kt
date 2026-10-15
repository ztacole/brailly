package com.brailly.friendly.core.scan

import com.brailly.friendly.core.braille.BrailleCell

/**
 * Satu sel Braille hasil deteksi, berikut posisi dan ukurannya dalam piksel
 * gambar asli.
 *
 * @param cx titik pusat sel pada sumbu x
 * @param cy titik pusat sel pada sumbu y
 * @param w lebar sel
 * @param h tinggi sel
 * @param score keyakinan keberadaan sel (0..1)
 * @param mask bitmask titik; bit 0 = titik 1, ... bit 5 = titik 6
 */
data class DetectedCell(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val score: Float,
    val mask: Int,
) {
    val left: Float get() = cx - w / 2
    val top: Float get() = cy - h / 2
    val right: Float get() = cx + w / 2
    val bottom: Float get() = cy + h / 2

    fun toBrailleCell(): BrailleCell = BrailleCell.ofMask(mask)
}
