package com.brailly.friendly

import com.brailly.friendly.core.scan.BrailleModel
import com.brailly.friendly.core.scan.BraillePageReader
import com.brailly.friendly.core.scan.CellDecoder
import com.brailly.friendly.core.scan.ReaderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menguji orkestrasi pembacaan dengan model tiruan, sehingga alur skala,
 * lintasan ganda, dan penyusunan paragraf dapat diperiksa tanpa perangkat.
 */
class BraillePageReaderTest {

    /** Model tiruan yang menaruh sel braille pada posisi yang kita tentukan. */
    private class FakeModel(
        override val inputHeight: Int = 256,
        override val inputWidth: Int = 192,
        /** Daftar (baris, kolom, mask) pada peta keluaran. */
        private val cells: List<Triple<Int, Int, Int>> = emptyList(),
        private val cellSizeLog: Float = 1.6f,
    ) : BrailleModel {
        var runCount = 0
            private set
        var lastInput: FloatArray? = null
            private set

        override fun run(input: FloatArray): FloatArray {
            runCount++
            lastInput = input
            val h = inputHeight / CellDecoder.STRIDE
            val w = inputWidth / CellDecoder.STRIDE
            val area = h * w
            val maps = FloatArray(11 * area)
            // Logit dasar: tanpa sel, titik mati, ukuran sel tetap.
            for (i in 0 until area) {
                maps[i] = -8f
                for (d in 1..6) maps[d * area + i] = -6f
                maps[9 * area + i] = cellSizeLog
                maps[10 * area + i] = cellSizeLog + 0.4f
            }
            for ((y, x, mask) in cells) {
                val i = y * w + x
                maps[i] = 5f
                for (d in 0 until 6) {
                    maps[(1 + d) * area + i] = if (mask and (1 shl d) != 0) 6f else -6f
                }
            }
            return maps
        }
    }

    @Test
    fun membacaSatuBarisMenjadiTeks() {
        val sel = HALO.mapIndexed { i, mask -> Triple(20, 10 + i * 5, mask) }
        val reader = BraillePageReader(
            FakeModel(cells = sel),
            ReaderConfig(twoPass = false),
        )
        val piksel = IntArray(192 * 256) { 0xFFFFFFFF.toInt() }

        val hasil = reader.read(piksel, 192, 256)
        assertEquals(4, hasil.cells.size)
        assertEquals(1, hasil.layout.lines.size)
        assertEquals("halo", hasil.layout.lines[0].text)
        assertEquals("halo", hasil.text)
    }

    @Test
    fun duaParagrafTerpisahOlehJarakBaris() {
        val sel = mutableListOf<Triple<Int, Int, Int>>()
        // Tiga baris berdekatan, lalu satu baris jauh di bawah. Jumlah baris
        // perlu cukup banyak agar median jarak baris mewakili jarak rapatnya.
        for (baris in listOf(10, 16, 22, 50)) {
            HALO.forEachIndexed { i, mask -> sel.add(Triple(baris, 10 + i * 5, mask)) }
        }
        val reader = BraillePageReader(FakeModel(cells = sel), ReaderConfig(twoPass = false))
        val hasil = reader.read(IntArray(192 * 256) { -1 }, 192, 256)

        assertEquals(4, hasil.layout.lines.size)
        assertEquals(2, hasil.layout.paragraphs.size)
        assertEquals("halo halo halo", hasil.layout.paragraphs[0].text)
        assertEquals("halo", hasil.layout.paragraphs[1].text)
    }

    @Test
    fun lintasanGandaMenjalankanModelDuaKali() {
        // Penyesuaian skala baru aktif bila selnya cukup banyak (minimal lima),
        // sehingga dipakai kata yang lebih panjang.
        val sel = HALAMAN.mapIndexed { i, mask -> Triple(20, 8 + i * 5, mask) }

        val satu = FakeModel(cells = sel)
        BraillePageReader(satu, ReaderConfig(twoPass = false))
            .read(IntArray(192 * 256), 192, 256)
        assertEquals("satu lintasan", 1, satu.runCount)

        // Sel tiruan berukuran jauh dari target, sehingga skala disesuaikan.
        val dua = FakeModel(cells = sel, cellSizeLog = 0.5f)
        val hasil = BraillePageReader(dua, ReaderConfig(twoPass = true, targetCellHeight = 36f))
            .read(IntArray(192 * 256), 192, 256)
        assertEquals("dua lintasan", 2, dua.runCount)
        assertEquals("halaman", hasil.layout.lines[0].text)
    }

    @Test
    fun tensorMasukanSelaluSesuaiUkuranModel() {
        val model = FakeModel()
        val reader = BraillePageReader(model, ReaderConfig(twoPass = false))
        // Gambar jauh lebih besar dan berorientasi lanskap.
        reader.read(IntArray(1600 * 900) { 0xFF123456.toInt() }, 1600, 900)
        assertEquals(3 * 256 * 192, model.lastInput!!.size)
        assertTrue(model.lastInput!!.all { it in 0f..1f })
    }

    @Test
    fun halamanKosongTidakMenghasilkanTeks() {
        val reader = BraillePageReader(FakeModel(), ReaderConfig(twoPass = false))
        val hasil = reader.read(IntArray(192 * 256), 192, 256)
        assertTrue(hasil.isEmpty)
        assertEquals("", hasil.text)
    }

    private companion object {
        /** h a l o */
        val HALO = intArrayOf(0b010011, 0b000001, 0b000111, 0b010101)

        /** h a l a m a n */
        val HALAMAN = intArrayOf(
            0b010011, 0b000001, 0b000111, 0b000001, 0b001101, 0b000001, 0b011101,
        )
    }
}
