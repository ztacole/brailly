package com.brailly.friendly.core.scan

import com.brailly.friendly.core.braille.BrailleTranslator
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** Satu baris Braille pada halaman, sudah berurutan dari kiri ke kanan. */
data class ScanLine(
    /** Bitmask per sel; nilai -1 menandai sel kosong (spasi). */
    val cells: List<Int>,
    val braille: String,
    val text: String,
    val left: Float,
    val right: Float,
    val y: Float,
)

/** Kelompok baris yang membentuk satu paragraf atau satu judul. */
data class ScanParagraph(
    val lines: List<ScanLine>,
    val text: String,
    val isHeading: Boolean,
)

/** Hasil pembacaan tata letak satu halaman. */
data class PageLayout(
    val angleDegrees: Float,
    val cellPitch: Float,
    val lineSpacing: Float,
    val lines: List<ScanLine>,
    val paragraphs: List<ScanParagraph>,
) {
    /** Seluruh teks halaman, antar paragraf dipisahkan baris kosong. */
    val text: String get() = paragraphs.joinToString("\n\n") { it.text }

    val braille: String get() = lines.joinToString("\n") { it.braille }
}

/**
 * Menyusun sel-sel hasil deteksi menjadi urutan baca: baris, spasi antar kata,
 * lalu paragraf dan judul.
 *
 * Sudut kemiringan halaman diperkirakan dari tetangga horizontal terdekat,
 * sehingga foto yang agak miring tetap terbaca tanpa perlu diluruskan dahulu.
 * Spasi antar kata disimpulkan dari jarak antar sel, bukan dari piksel kosong.
 *
 * Perilakunya disetarakan dengan `brailly/layout.py` dan diuji terhadap
 * fixture yang dihasilkan darinya.
 */
class LayoutAnalyzer(private val translator: BrailleTranslator = BrailleTranslator()) {

    fun analyze(cellsIn: List<DetectedCell>, dehyphenate: Boolean = false): PageLayout {
        val items = cellsIn.filter { it.mask != 0 }
        if (items.isEmpty()) return PageLayout(0f, 0f, 0f, emptyList(), emptyList())

        val n = items.size
        val medW = median(items.map { it.w.toDouble() })
        val medH = median(items.map { it.h.toDouble() })
        val angle = estimateAngle(items, medW)

        // Putar balik agar baris menjadi mendatar; rx dipakai sebagai urutan baca.
        val ca = cos(-angle)
        val sa = sin(-angle)
        val rx = DoubleArray(n) { items[it].cx * ca - items[it].cy * sa }
        val ry = DoubleArray(n) { items[it].cx * sa + items[it].cy * ca }

        var tracks = traceLines(rx, ry, medW, medH)
        tracks = mergeSplitLines(tracks, rx, ry, medH)
        tracks = tracks.map { tr -> tr.sortedBy { rx[it] } }

        val pitch = estimatePitch(tracks, rx, medW)
        val lines = tracks.map { tr -> buildLine(tr, rx, ry, items, pitch) }
            .sortedBy { it.y }

        val spacing = if (lines.size > 1) {
            median((1 until lines.size).map { (lines[it].y - lines[it - 1].y).toDouble() })
        } else {
            medH * 1.6
        }

        val paragraphs = groupParagraphs(lines, pitch, spacing, dehyphenate)
        return PageLayout(
            angleDegrees = Math.toDegrees(angle).toFloat(),
            cellPitch = pitch.toFloat(),
            lineSpacing = spacing.toFloat(),
            lines = lines,
            paragraphs = paragraphs,
        )
    }

    /** Sudut dominan baris teks (radian), dari tetangga horizontal terdekat. */
    private fun estimateAngle(items: List<DetectedCell>, medW: Double): Double {
        if (items.size < 3) return 0.0
        val sudut = mutableListOf<Double>()
        for (i in items.indices) {
            var terdekat = -1
            var jarakTerdekat = Double.MAX_VALUE
            for (j in items.indices) {
                if (j == i) continue
                val dx = (items[j].cx - items[i].cx).toDouble()
                val dy = (items[j].cy - items[i].cy).toDouble()
                val jarak = hypot(dx, dy)
                // Hanya tetangga di sebelah kanan yang jelas-jelas mendatar.
                if (jarak < 3.0 * medW && abs(dx) > abs(dy) * 1.5 && dx > 0 && jarak < jarakTerdekat) {
                    jarakTerdekat = jarak
                    terdekat = j
                }
            }
            if (terdekat >= 0) {
                sudut.add(
                    atan2(
                        (items[terdekat].cy - items[i].cy).toDouble(),
                        (items[terdekat].cx - items[i].cx).toDouble(),
                    )
                )
            }
        }
        return if (sudut.isEmpty()) 0.0 else median(sudut)
    }

    /**
     * Runut baris dari kiri ke kanan secara rakus. Pendekatan ini tahan
     * terhadap baris yang melengkung akibat perspektif, karena setiap sel
     * hanya dibandingkan dengan ujung kanan baris yang sedang terbentuk.
     */
    private fun traceLines(rx: DoubleArray, ry: DoubleArray, medW: Double, medH: Double): List<List<Int>> {
        val tracks = mutableListOf<MutableList<Int>>()
        val urutan = rx.indices.sortedBy { rx[it] }
        for (i in urutan) {
            var terbaik = -1
            var selisihTerbaik = 0.5 * medH
            for ((t, tr) in tracks.withIndex()) {
                val j = tr.last()
                if (rx[i] - rx[j] > 25 * medW) continue
                val dy = abs(ry[i] - ry[j])
                if (dy < selisihTerbaik) {
                    terbaik = t
                    selisihTerbaik = dy
                }
            }
            if (terbaik >= 0) tracks[terbaik].add(i) else tracks.add(mutableListOf(i))
        }
        return tracks
    }

    /** Gabungkan dua runutan yang sebenarnya satu baris, terpisah jarak lebar. */
    private fun mergeSplitLines(
        tracks: List<List<Int>>,
        rx: DoubleArray,
        ry: DoubleArray,
        medH: Double,
    ): List<List<Int>> {
        val urut = tracks.sortedBy { tr -> tr.map { ry[it] }.average() }
        val hasil = mutableListOf<List<Int>>()
        for (tr in urut) {
            if (hasil.isNotEmpty()) {
                val prev = hasil.last()
                val sejajar = abs(median(tr.map { ry[it] }) - median(prev.map { ry[it] })) < 0.35 * medH
                val trMin = tr.minOf { rx[it] }
                val trMax = tr.maxOf { rx[it] }
                val prevMin = prev.minOf { rx[it] }
                val prevMax = prev.maxOf { rx[it] }
                if (sejajar && (trMin > prevMax || prevMin > trMax)) {
                    hasil[hasil.size - 1] = (prev + tr).sortedBy { rx[it] }
                    continue
                }
            }
            hasil.add(tr)
        }
        return hasil
    }

    /** Jarak antar sel, diambil dari celah-celah terkecil antar sel berurutan. */
    private fun estimatePitch(tracks: List<List<Int>>, rx: DoubleArray, medW: Double): Double {
        val celah = mutableListOf<Double>()
        for (tr in tracks) {
            for (a in 1 until tr.size) celah.add(rx[tr[a]] - rx[tr[a - 1]])
        }
        var pitch = if (celah.isNotEmpty()) {
            val dasar = median(celah)
            val kecil = celah.filter { it < 1.4 * dasar }
            if (kecil.isNotEmpty()) median(kecil) else dasar
        } else {
            medW * 1.6
        }
        if (pitch < medW * 1.05) pitch = medW * 1.05
        return pitch
    }

    private fun buildLine(
        tr: List<Int>,
        rx: DoubleArray,
        ry: DoubleArray,
        items: List<DetectedCell>,
        pitch: Double,
    ): ScanLine {
        val cells = mutableListOf<Int>()
        for ((a, k) in tr.withIndex()) {
            if (a > 0) {
                // Jarak lebih dari satu pitch berarti ada sel kosong = spasi.
                // Math.rint membulatkan 0,5 ke bilangan genap, sama seperti
                // round() pada implementasi acuan Python.
                val kosong = Math.rint((rx[k] - rx[tr[a - 1]]) / pitch).toInt() - 1
                repeat(max(0, kosong)) { cells.add(-1) }
            }
            cells.add(items[k].mask)
        }
        return ScanLine(
            cells = cells,
            braille = cells.joinToString("") {
                if (it < 0) " " else (0x2800 + (it and 0x3F)).toChar().toString()
            },
            text = translator.decodeMasks(cells),
            left = rx[tr.first()].toFloat(),
            right = rx[tr.last()].toFloat(),
            y = tr.map { ry[it] }.average().toFloat(),
        )
    }

    /**
     * Kelompokkan baris menjadi paragraf berdasarkan indentasi, jarak baris,
     * dan baris yang berakhir lebih awal. Baris pendek yang terpusat di tengah
     * ditandai sebagai judul.
     */
    private fun groupParagraphs(
        lines: List<ScanLine>,
        pitch: Double,
        spacing: Double,
        dehyphenate: Boolean,
    ): List<ScanParagraph> {
        if (lines.isEmpty()) return emptyList()
        val penuh = lines.filter { it.cells.size >= 4 }.ifEmpty { lines }
        val marginL = percentile(penuh.map { it.left.toDouble() }, 10.0)
        val marginR = percentile(penuh.map { it.right.toDouble() }, 90.0)
        val lebar = max(marginR - marginL, pitch)

        data class Kelompok(val lines: MutableList<ScanLine>, val isHeading: Boolean)

        val kelompok = mutableListOf<Kelompok>()
        var sedang = mutableListOf<ScanLine>()
        var prev: ScanLine? = null
        for (ln in lines) {
            val indent = (ln.left - marginL) / pitch
            val sisaKanan = (marginR - ln.right) / pitch
            val judul = indent >= 3 && sisaKanan >= 3 &&
                abs(indent - sisaKanan) <= 3 && lebar / pitch > 12

            var baru = prev == null
            val p = prev
            if (p != null) {
                baru = when {
                    ln.y - p.y > 1.6 * spacing -> true
                    indent >= 1.5 -> true
                    (marginR - p.right) / pitch > 6 && berakhirTuntas(p.text) -> true
                    kelompok.isNotEmpty() && kelompok.last().isHeading && sedang.isEmpty() -> true
                    else -> false
                }
            }
            if (judul) {
                if (sedang.isNotEmpty()) {
                    kelompok.add(Kelompok(sedang, false))
                    sedang = mutableListOf()
                }
                kelompok.add(Kelompok(mutableListOf(ln), true))
            } else {
                if (baru && sedang.isNotEmpty()) {
                    kelompok.add(Kelompok(sedang, false))
                    sedang = mutableListOf()
                }
                sedang.add(ln)
            }
            prev = ln
        }
        if (sedang.isNotEmpty()) kelompok.add(Kelompok(sedang, false))

        return kelompok.map { k ->
            ScanParagraph(k.lines, gabungBaris(k.lines, dehyphenate), k.isHeading)
        }.filter { it.text.isNotEmpty() }
    }

    /** Teks diakhiri tanda yang menandakan kalimat selesai. */
    private fun berakhirTuntas(text: String): Boolean {
        val akhir = text.trimEnd()
        // Baris kosong dianggap tuntas, menyamai perilaku implementasi acuan.
        if (akhir.isEmpty()) return true
        return akhir.last() in ".?!:\u201D)"
    }

    private fun gabungBaris(lines: List<ScanLine>, dehyphenate: Boolean): String {
        var hasil = ""
        for (ln in lines) {
            val t = ln.text.trim()
            if (t.isEmpty()) continue
            hasil = when {
                hasil.isEmpty() -> t
                // Kata yang terpotong di akhir baris disambung tanpa spasi.
                hasil.endsWith("-") -> (if (dehyphenate) hasil.dropLast(1) else hasil) + t
                else -> "$hasil $t"
            }
        }
        return hasil
    }

    private fun median(v: List<Double>): Double {
        if (v.isEmpty()) return 0.0
        val s = v.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2
    }

    /** Persentil dengan interpolasi linear, menyamai `numpy.percentile`. */
    private fun percentile(v: List<Double>, q: Double): Double {
        if (v.isEmpty()) return 0.0
        val s = v.sorted()
        val pos = (s.size - 1) * q / 100.0
        val bawah = pos.toInt()
        val atas = if (bawah + 1 < s.size) bawah + 1 else bawah
        val sisa = pos - bawah
        return s[bawah] + sisa * (s[atas] - s[bawah])
    }
}
