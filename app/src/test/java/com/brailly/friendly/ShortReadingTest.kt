package com.brailly.friendly

import com.brailly.friendly.core.scan.BrailleModel
import com.brailly.friendly.core.scan.BraillePageReader
import com.brailly.friendly.core.scan.CellDecoder
import com.brailly.friendly.core.scan.ReaderConfig
import com.brailly.friendly.core.scan.ReadingQuality
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Menguji bacaan pendek seperti tombol lift atau papan nama, yang hanya
 * berisi satu sampai beberapa sel.
 *
 * Ukuran kewajaran bertumpu pada bagian pola yang dikenal, dan bagian itu
 * nyaris selalu 1,0 pada bacaan sependek itu. Karena buktinya sedikit,
 * pencarian orientasi tidak boleh ikut berjalan: orientasi lain bisa menang
 * karena kebetulan dan memberi huruf yang salah.
 */
class ShortReadingTest {

    /** Model tiruan: menaruh [count] sel huruf pada satu baris. */
    private class ShortModel(private val count: Int, private val skor: Float = 1.6f) : BrailleModel {
        override val inputHeight = 256
        override val inputWidth = 192
        var runCount = 0
            private set

        override fun run(input: FloatArray): FloatArray {
            runCount++
            val h = outputHeight
            val w = outputWidth
            val area = h * w
            val maps = FloatArray(11 * area)
            for (i in 0 until area) {
                maps[i] = -8f
                for (d in 1..6) maps[d * area + i] = -6f
                maps[9 * area + i] = 1.6f
                maps[10 * area + i] = 2.0f
            }
            val mask = MASK_G
            // Peta keluaran hanya 48 kolom, dan sel yang berjarak kurang dari
            // sekitar 4 satuan tersaring sebagai deteksi ganda. Karena itu sel
            // disebar enam per baris dengan jarak baris yang cukup lebar.
            for (k in 0 until count) {
                val baris = 12 + (k / 6) * 10
                val kolom = 3 + (k % 6) * 5
                val i = baris * w + kolom
                maps[i] = skor
                for (d in 0 until 6) {
                    maps[(1 + d) * area + i] = if (mask and (1 shl d) != 0) 6f else -6f
                }
            }
            return maps
        }
    }

    private companion object {
        /** 'g' = titik 1,2,4,5 */
        const val MASK_G = 0b011011
    }

    @Test
    fun bacaanSatuSelTidakMemicuPencarianOrientasi() {
        // Keyakinan rendah, tetapi selnya terlalu sedikit untuk dinilai.
        val model = ShortModel(count = 1, skor = 0.2f)
        val hasil = BraillePageReader(model, ReaderConfig(twoPass = false, autoOrient = true))
            .read(IntArray(192 * 256), 192, 256)
        assertEquals("satu lintasan saja", 1, model.runCount)
        assertEquals(0, hasil.orientationDegrees)
        assertEquals("g", hasil.text)
    }

    @Test
    fun bacaanPendekTetapTerbacaUtuh() {
        val model = ShortModel(count = 3)
        val hasil = BraillePageReader(model, ReaderConfig(twoPass = false))
            .read(IntArray(192 * 256), 192, 256)
        assertEquals("ggg", hasil.text)
        assertEquals(3, hasil.cells.size)
    }

    @Test
    fun halamanPenuhTetapMemicuPencarianOrientasi() {
        // Dengan sel yang cukup banyak, keyakinan rendah memang patut dicurigai.
        val model = ShortModel(count = 12, skor = 0.2f)
        BraillePageReader(model, ReaderConfig(twoPass = false, autoOrient = true))
            .read(IntArray(192 * 256), 192, 256)
        assertEquals("empat orientasi dicoba", 4, model.runCount)
    }

    @Test
    fun ambangPeringatanDibedakanMenurutJumlahSel() {
        fun ragu(sel: Int, mutu: Float) = ReadingQuality.isDoubtful(sel, mutu)

        // Bacaan pendek: 0,42 masih diterima (kasus tombol lift yang benar).
        assertEquals(false, ragu(3, 0.42f))
        // Bacaan pendek yang benar-benar buruk tetap diperingatkan.
        assertEquals(true, ragu(3, 0.20f))
        // Halaman penuh memakai ambang yang lebih ketat.
        assertEquals(true, ragu(40, 0.42f))
        assertEquals(false, ragu(40, 0.70f))
        // Tanpa sel sama sekali bukan 'diragukan', melainkan 'tidak ada braille'.
        assertEquals(false, ragu(0, 0f))
    }
}
