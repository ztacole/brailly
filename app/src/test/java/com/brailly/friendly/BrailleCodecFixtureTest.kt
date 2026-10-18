package com.brailly.friendly

import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleToken
import com.brailly.friendly.core.braille.BrailleTranslator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Menguji penerjemah terhadap fixture emas yang dihasilkan implementasi acuan
 * Python (`brailly-model/scripts/make_fixtures.py`). Keluaran Kotlin wajib
 * identik, bukan sekadar mirip.
 */
class BrailleCodecFixtureTest {

    private val translator = BrailleTranslator()
    private val fixture: JSONObject by lazy { JSONObject(readFixture("codec_cases.json")) }

    @Test
    fun maskDanDotsSalingKonsisten() {
        val peta = fixture.getJSONObject("mask_ke_dots")
        for (m in 0 until 64) {
            val harapan = peta.getString(m.toString())
            val sel = BrailleCell.ofMask(m)
            val didapat = sel.dots.sorted().joinToString("")
            assertEquals("mask $m", harapan, didapat)
            assertEquals("mask bolak-balik $m", m, sel.mask)
        }
    }

    @Test
    fun teksKeBrailleSamaDenganAcuan() {
        val kasus = fixture.getJSONArray("encode")
        for (k in 0 until kasus.length()) {
            val o = kasus.getJSONObject(k)
            val teks = o.getString("teks")
            val harapan = o.getJSONArray("sel").let { a -> List(a.length()) { a.getInt(it) } }
            val didapat = translator.textToBraille(teks).map { t ->
                when (t) {
                    is BrailleToken.Cell -> t.cell.mask
                    BrailleToken.Space -> -1
                }
            }
            assertEquals("encode: $teks", harapan, didapat)
        }
    }

    @Test
    fun brailleKeTeksSamaDenganAcuan() {
        val kasus = fixture.getJSONArray("decode")
        for (k in 0 until kasus.length()) {
            val o = kasus.getJSONObject(k)
            val masks = o.getJSONArray("sel").let { a -> List(a.length()) { a.getInt(it) } }
            val harapan = o.getString("teks")
            assertEquals("decode: $harapan", harapan, translator.decodeMasks(masks))
        }
    }

    @Test
    fun bolakBalikLewatApiLama() {
        // API lama (dipakai fitur keypad) harus tetap memberi hasil yang sama.
        val kasus = fixture.getJSONArray("decode")
        for (k in 0 until kasus.length()) {
            val o = kasus.getJSONObject(k)
            val sel = o.getJSONArray("sel").let { a ->
                List(a.length()) { i ->
                    val m = a.getInt(i)
                    if (m < 0) null else BrailleCell.ofMask(m)
                }
            }
            assertEquals(o.getString("teks"), translator.brailleToText(sel))
        }
    }

    companion object {
        fun readFixture(name: String): String {
            val stream = BrailleCodecFixtureTest::class.java
                .getResourceAsStream("/fixtures/$name")
                ?: error("fixture tidak ditemukan: $name")
            return stream.bufferedReader().use { it.readText() }
        }
    }
}
