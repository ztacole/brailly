package com.brailly.friendly

import com.brailly.friendly.core.scan.ImagePreprocessor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Menguji pra-pemrosesan terhadap acuan OpenCV (`cv2.INTER_AREA` +
 * `BORDER_REPLICATE`). Penyusutan ditulis ulang di Kotlin tanpa OpenCV,
 * sehingga selisihnya perlu diukur, bukan diasumsikan nol.
 */
class ImagePreprocessorFixtureTest {

    private val fixture: JSONObject by lazy {
        JSONObject(BrailleCodecFixtureTest.readFixture("preprocess_case.json"))
    }

    @Test
    fun tensorCocokDenganAcuanOpenCv() {
        val w = fixture.getInt("lebar")
        val h = fixture.getInt("tinggi")
        val th = fixture.getInt("target_tinggi")
        val tw = fixture.getInt("target_lebar")
        val pix = fixture.getJSONArray("piksel_argb")
        val pixels = IntArray(pix.length()) { pix.getInt(it) }

        val siap = ImagePreprocessor.prepare(pixels, w, h, th, tw)

        assertEquals("skala", fixture.getDouble("skala"), siap.scale.toDouble(), 1e-6)
        assertEquals("panjang tensor", 3 * th * tw, siap.tensor.size)

        val harapan = fixture.getJSONArray("tensor")
        assertEquals(harapan.length(), siap.tensor.size)

        var maksSelisih = 0.0
        var jumlahSelisih = 0.0
        for (i in 0 until harapan.length()) {
            val d = abs(harapan.getDouble(i) - siap.tensor[i])
            if (d > maksSelisih) maksSelisih = d
            jumlahSelisih += d
        }
        val rataSelisih = jumlahSelisih / harapan.length()
        println(
            "pra-pemrosesan vs OpenCV: selisih maks %.5f (%.2f/255), rata-rata %.6f"
                .format(maksSelisih, maksSelisih * 255, rataSelisih)
        )
        // Satu tingkat warna 8-bit = 1/255 = 0,0039. Selisih di bawah itu
        // berarti tensornya setara setelah pembulatan ke piksel.
        assertTrue("selisih maks terlalu besar: $maksSelisih", maksSelisih < 1.0 / 255.0)
    }

    @Test
    fun gambarTidakPernahDipotong() {
        // Gambar lanskap pada masukan tegak: harus diperkecil, bukan dipotong.
        val w = 400
        val h = 100
        val pixels = IntArray(w * h) { 0xFF808080.toInt() }
        val siap = ImagePreprocessor.prepare(pixels, w, h, 1024, 768)
        assertEquals(768.0 / 400.0, siap.scale.toDouble(), 1e-6)
        assertEquals(3 * 1024 * 768, siap.tensor.size)
    }

    @Test
    fun skalaDimintaDibatasiAgarMuat() {
        val pixels = IntArray(200 * 300) { 0xFF404040.toInt() }
        // Skala 10x mustahil dipenuhi; harus dipangkas agar gambar tetap muat.
        val siap = ImagePreprocessor.prepare(pixels, 200, 300, 256, 256, requestedScale = 10f)
        assertTrue(siap.scale <= 256f / 300f + 1e-6)
    }
}
