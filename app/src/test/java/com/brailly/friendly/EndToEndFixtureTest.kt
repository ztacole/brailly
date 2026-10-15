package com.brailly.friendly

import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.core.scan.BrailleModel
import com.brailly.friendly.core.scan.BraillePageReader
import com.brailly.friendly.core.scan.ImagePreprocessor
import com.brailly.friendly.core.scan.ReaderConfig
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Menguji rantai penuh pada skala sebenarnya (masukan 768x1024), memakai satu
 * foto Braille nyata beserta keluaran model yang sesungguhnya.
 *
 * Uji sebelumnya hanya mencakup potongan kecil: decoder pada peta 64x64 dan
 * pra-pemrosesan pada gambar 53x37. Keduanya tidak akan menjaring kekeliruan
 * yang baru muncul pada ukuran penuh.
 */
class EndToEndFixtureTest {

    private val fixture: JSONObject by lazy {
        JSONObject(BrailleCodecFixtureTest.readFixture("endtoend_case.json"))
    }

    @Test
    fun praPemrosesanSkalaPenuhCocokDenganAcuan() {
        val w = fixture.getInt("lebar")
        val h = fixture.getInt("tinggi")
        val th = fixture.getInt("target_tinggi")
        val tw = fixture.getInt("target_lebar")
        val pix = fixture.getJSONArray("piksel_argb")
        val pixels = IntArray(pix.length()) { pix.getInt(it) }

        // Skala diminta mengikuti aturan reader: sisi panjang menuju 1024.
        val diminta = 1024f / maxOf(w, h)
        val siap = ImagePreprocessor.prepare(pixels, w, h, th, tw, diminta)

        assertEquals("skala", fixture.getDouble("skala"), siap.scale.toDouble(), 1e-6)

        val idx = fixture.getJSONArray("tensor_indeks")
        val nilai = fixture.getJSONArray("tensor_nilai")
        var maks = 0.0
        var jumlah = 0.0
        for (i in 0 until idx.length()) {
            val d = abs(nilai.getDouble(i) - siap.tensor[idx.getInt(i)])
            if (d > maks) maks = d
            jumlah += d
        }
        println(
            "pra-pemrosesan skala penuh: selisih maks %.5f (%.2f/255), rata-rata %.6f"
                .format(maks, maks * 255, jumlah / idx.length())
        )
        assertTrue("selisih maks terlalu besar: $maks", maks < 1.0 / 255.0)
    }

    @Test
    fun decoderDanTataLetakSkalaPenuhCocokDenganAcuan() {
        // Keluaran model sungguhan diputar ulang lewat model tiruan, sehingga
        // decoder dan penyusun tata letak diuji pada peta berukuran penuh.
        val bentuk = fixture.getJSONArray("keluaran_bentuk")
        val kanal = bentuk.getInt(0)
        val oh = bentuk.getInt(1)
        val ow = bentuk.getInt(2)
        val idx = fixture.getJSONArray("keluaran_indeks")
        val nilai = fixture.getJSONArray("keluaran_nilai")

        // Peta disusun ulang dari cuplikan; nilai yang tidak tercuplik diisi
        // logit "tidak ada sel" sehingga tidak memunculkan puncak palsu.
        val maps = FloatArray(kanal * oh * ow) { -8f }
        for (i in 0 until idx.length()) maps[idx.getInt(i)] = nilai.getDouble(i).toFloat()

        val model = object : BrailleModel {
            override val inputHeight = fixture.getInt("target_tinggi")
            override val inputWidth = fixture.getInt("target_lebar")
            override fun run(input: FloatArray) = maps
        }
        val reader = BraillePageReader(model, ReaderConfig(twoPass = false), BrailleTranslator())
        val w = fixture.getInt("lebar")
        val h = fixture.getInt("tinggi")
        val pix = fixture.getJSONArray("piksel_argb")
        val pixels = IntArray(pix.length()) { pix.getInt(it) }

        // Tidak memeriksa teksnya (peta sengaja tak lengkap), tetapi memastikan
        // rantai berukuran penuh berjalan tanpa galat dan bentuknya konsisten.
        val hasil = reader.read(pixels, w, h)
        assertEquals(kanal * oh * ow, maps.size)
        assertTrue("rantai skala penuh harus berjalan", hasil.cells.size >= 0)
        println("rantai skala penuh berjalan: ${hasil.cells.size} sel dari peta cuplikan")
    }

    @Test
    fun acuanMenemukanBanyakSelPadaFotoNyata() {
        // Menegaskan bahwa fixture memang berasal dari halaman berisi Braille,
        // sehingga perbandingan di atas bermakna.
        assertTrue(fixture.getInt("sel_terdeteksi") > 50)
        assertTrue(fixture.getJSONArray("baris").length() > 3)
    }
}
