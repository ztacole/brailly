package com.brailly.friendly

import com.brailly.friendly.core.braille.BrailleMap
import com.brailly.friendly.core.scan.BrailleModel
import com.brailly.friendly.core.scan.BraillePageReader
import com.brailly.friendly.core.scan.CellDecoder
import com.brailly.friendly.core.scan.PixelImage
import com.brailly.friendly.core.scan.PixelRotation
import com.brailly.friendly.core.scan.ReaderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menguji pemutaran piksel dan pemilihan orientasi otomatis.
 *
 * Orientasi menentukan arti pola Braille: sel yang miring 90 derajat tetap
 * terdeteksi sebagai sel, tetapi keenam titiknya terbaca pada posisi yang
 * salah. Gejalanya adalah keyakinan rendah disertai banyak pola tak dikenal.
 */
class OrientationTest {

    @Test
    fun putar90DuaKaliSamaDengan180() {
        val w = 5
        val h = 3
        val asal = PixelImage(IntArray(w * h) { it }, w, h)

        val sekali = PixelRotation.rotate(asal, 90)
        assertEquals("lebar dan tinggi bertukar", h, sekali.width)
        assertEquals(w, sekali.height)

        val duaKali = PixelRotation.rotate(sekali, 90)
        val langsung = PixelRotation.rotate(asal, 180)
        assertEquals(langsung.width, duaKali.width)
        assertEquals(langsung.height, duaKali.height)
        assertTrue(langsung.pixels.contentEquals(duaKali.pixels))
    }

    @Test
    fun putar360KembaliKeAsal() {
        val asal = PixelImage(IntArray(12) { it * 7 }, 4, 3)
        var kini = asal
        repeat(4) { kini = PixelRotation.rotate(kini, 90) }
        assertEquals(asal.width, kini.width)
        assertEquals(asal.height, kini.height)
        assertTrue(asal.pixels.contentEquals(kini.pixels))
    }

    @Test
    fun putar90MemindahkanPikselKeTempatYangBenar() {
        // 2x2: [a b / c d] diputar 90 searah jarum jam -> [c a / d b]
        val asal = PixelImage(intArrayOf(1, 2, 3, 4), 2, 2)
        val hasil = PixelRotation.rotate(asal, 90)
        assertTrue(intArrayOf(3, 1, 4, 2).contentEquals(hasil.pixels))
    }

    @Test
    fun polaMiringDikenaliSebagaiTidakWajar() {
        // Huruf 'h' (titik 1,2,5) adalah pola sah.
        assertTrue(BrailleMap.isKnown(0b010011))
        // Pola enam titik penuh bukan huruf maupun tanda baca.
        assertFalse(BrailleMap.isKnown(0b111111))
    }

    @Test
    fun orientasiLainDicobaSaatHasilTidakWajar() {
        // Model tiruan yang selalu memberi pola tak dikenal: mutunya rendah,
        // sehingga ketiga orientasi lain harus ikut dicoba.
        val model = HitungModel(mask = 0b111111)
        BraillePageReader(model, ReaderConfig(twoPass = false, autoOrient = true))
            .read(IntArray(192 * 256), 192, 256)
        assertEquals("empat orientasi dicoba", 4, model.runCount)
    }

    @Test
    fun orientasiTidakDicobaUlangSaatHasilSudahWajar() {
        // Pola sah dengan keyakinan tinggi: cukup satu orientasi.
        val model = HitungModel(mask = 0b010011)
        val hasil = BraillePageReader(model, ReaderConfig(twoPass = false, autoOrient = true))
            .read(IntArray(192 * 256), 192, 256)
        assertEquals("satu orientasi saja", 1, model.runCount)
        assertEquals(0, hasil.orientationDegrees)
        assertTrue("mutu harus tinggi", hasil.quality > 0.9f)
    }

    @Test
    fun autoOrientDapatDimatikan() {
        val model = HitungModel(mask = 0b111111)
        BraillePageReader(model, ReaderConfig(twoPass = false, autoOrient = false))
            .read(IntArray(192 * 256), 192, 256)
        assertEquals(1, model.runCount)
    }

    /**
     * Model tiruan: menaruh satu baris sel dengan mask tetap.
     *
     * Jumlahnya dibuat di atas [ReaderConfig.minCellsForOrientation] agar
     * pencarian orientasi memang diizinkan berjalan; bacaan pendek diuji
     * terpisah di ShortReadingTest.
     */
    private class HitungModel(private val mask: Int, private val count: Int = 12) : BrailleModel {
        override val inputHeight = 256
        override val inputWidth = 192
        var runCount = 0
            private set

        override fun run(input: FloatArray): FloatArray {
            runCount++
            val h = inputHeight / CellDecoder.STRIDE
            val w = inputWidth / CellDecoder.STRIDE
            val area = h * w
            val maps = FloatArray(11 * area)
            for (i in 0 until area) {
                maps[i] = -8f
                for (d in 1..6) maps[d * area + i] = -6f
                maps[9 * area + i] = 1.6f
                maps[10 * area + i] = 2.0f
            }
            // Peta keluaran hanya 48 kolom, dan sel yang berjarak kurang dari
            // sekitar 4 satuan tersaring sebagai deteksi ganda. Karena itu sel
            // disebar enam per baris dengan jarak baris yang cukup lebar.
            for (k in 0 until count) {
                val baris = 12 + (k / 6) * 10
                val kolom = 3 + (k % 6) * 5
                val i = baris * w + kolom
                maps[i] = 5f
                for (d in 0 until 6) {
                    maps[(1 + d) * area + i] = if (mask and (1 shl d) != 0) 6f else -6f
                }
            }
            return maps
        }
    }
}
