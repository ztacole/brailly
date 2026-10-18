package com.brailly.friendly

import com.brailly.friendly.core.scan.CellDecoder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menguji decoder terhadap potongan keluaran model sungguhan beserta sel yang
 * dihasilkan implementasi acuan Python (`brailly/postprocess.py`).
 */
class CellDecoderFixtureTest {

    private val fixture: JSONObject by lazy {
        JSONObject(BrailleCodecFixtureTest.readFixture("decoder_case.json"))
    }

    @Test
    fun decodeSamaDenganAcuan() {
        val bentuk = fixture.getJSONArray("bentuk")
        val kanal = bentuk.getInt(0)
        val tinggi = bentuk.getInt(1)
        val lebar = bentuk.getInt(2)
        assertEquals(11, kanal)

        val arr = fixture.getJSONArray("tensor")
        val maps = FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
        val ambang = fixture.getDouble("ambang").toFloat()

        val didapat = CellDecoder.decode(maps, tinggi, lebar, ambang, 1f)
        val harapan = fixture.getJSONArray("harapan")

        assertEquals("jumlah sel", harapan.length(), didapat.size)
        for (i in 0 until harapan.length()) {
            val h = harapan.getJSONObject(i)
            val d = didapat[i]
            val label = "sel ke-$i"
            assertEquals("$label mask", h.getInt("mask"), d.mask)
            assertEquals("$label cx", h.getDouble("cx"), d.cx.toDouble(), 2e-3)
            assertEquals("$label cy", h.getDouble("cy"), d.cy.toDouble(), 2e-3)
            assertEquals("$label w", h.getDouble("w"), d.w.toDouble(), 2e-3)
            assertEquals("$label h", h.getDouble("h"), d.h.toDouble(), 2e-3)
            assertEquals("$label skor", h.getDouble("skor"), d.score.toDouble(), 2e-3)
        }
    }

    @Test
    fun konsensusUkuranMenyaringSelGanjil() {
        val bentuk = fixture.getJSONArray("bentuk")
        val arr = fixture.getJSONArray("tensor")
        val maps = FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
        val sel = CellDecoder.decode(maps, bentuk.getInt(1), bentuk.getInt(2), 0.3f, 1f)

        // Sel asli pada satu halaman berukuran seragam, jadi tidak ada yang dibuang.
        assertEquals(sel.size, CellDecoder.sizeConsensus(sel).size)

        // Sel palsu yang jauh lebih kecil harus tersingkir.
        val palsu = sel.first().copy(cx = 5f, cy = 5f, w = 2f, h = 3f, score = 0.35f)
        val campur = sel + palsu
        val hasil = CellDecoder.sizeConsensus(campur)
        assertTrue("sel palsu harus dibuang", hasil.none { it.h == 3f })
        assertEquals(sel.size, hasil.size)
    }
}
