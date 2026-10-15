package com.brailly.friendly

import com.brailly.friendly.core.scan.DetectedCell
import com.brailly.friendly.core.scan.LayoutAnalyzer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Menguji penyusunan tata letak terhadap 12 halaman Braille sungguhan. Untuk
 * setiap halaman, sel masukan dan hasil yang diharapkan berasal dari
 * implementasi acuan Python (`brailly/layout.py`).
 *
 * Ini uji terpenting pada port: baris, spasi antar kata, paragraf, dan judul
 * harus keluar sama persis, bukan sekadar mendekati.
 */
class LayoutAnalyzerFixtureTest {

    private val analyzer = LayoutAnalyzer()
    private val fixture: JSONObject by lazy {
        JSONObject(BrailleCodecFixtureTest.readFixture("layout_cases.json"))
    }

    @Test
    fun tataLetakSamaDenganAcuan() {
        val kasus = fixture.getJSONArray("kasus")
        assertEquals("fixture harus memuat beberapa halaman", true, kasus.length() >= 5)
        var totalBaris = 0
        var totalParagraf = 0

        for (k in 0 until kasus.length()) {
            val o = kasus.getJSONObject(k)
            val nama = o.getString("nama")
            val masukan = o.getJSONArray("masukan")
            val sel = (0 until masukan.length()).map { i ->
                val a = masukan.getJSONArray(i)
                DetectedCell(
                    cx = a.getDouble(0).toFloat(),
                    cy = a.getDouble(1).toFloat(),
                    w = a.getDouble(2).toFloat(),
                    h = a.getDouble(3).toFloat(),
                    score = 1f,
                    mask = a.getInt(4),
                )
            }

            val hasil = analyzer.analyze(sel)
            val harapan = o.getJSONObject("harapan")

            assertEquals("$nama: sudut", harapan.getDouble("sudut_derajat"),
                hasil.angleDegrees.toDouble(), 1e-2)
            assertEquals("$nama: jarak sel", harapan.getDouble("jarak_sel"),
                hasil.cellPitch.toDouble(), 1e-2)

            val barisHarapan = harapan.getJSONArray("baris")
            assertEquals("$nama: jumlah baris", barisHarapan.length(), hasil.lines.size)
            for (i in 0 until barisHarapan.length()) {
                val bh = barisHarapan.getJSONObject(i)
                val selHarapan = bh.getJSONArray("sel").let { a ->
                    List(a.length()) { a.getInt(it) }
                }
                assertEquals("$nama: sel baris ke-$i", selHarapan, hasil.lines[i].cells)
                assertEquals("$nama: teks baris ke-$i", bh.getString("teks"), hasil.lines[i].text)
            }
            totalBaris += hasil.lines.size

            val paraHarapan = harapan.getJSONArray("paragraf")
            assertEquals("$nama: jumlah paragraf", paraHarapan.length(), hasil.paragraphs.size)
            for (i in 0 until paraHarapan.length()) {
                val ph = paraHarapan.getJSONObject(i)
                assertEquals("$nama: teks paragraf ke-$i",
                    ph.getString("teks"), hasil.paragraphs[i].text)
                assertEquals("$nama: jenis paragraf ke-$i",
                    ph.getString("jenis") == "heading", hasil.paragraphs[i].isHeading)
            }
            totalParagraf += hasil.paragraphs.size
        }
        println("tata letak cocok: ${kasus.length()} halaman, $totalBaris baris, $totalParagraf paragraf")
    }

    @Test
    fun halamanTanpaSelMenghasilkanKosong() {
        val hasil = analyzer.analyze(emptyList())
        assertEquals(0, hasil.lines.size)
        assertEquals(0, hasil.paragraphs.size)
        assertEquals("", hasil.text)
    }
}
