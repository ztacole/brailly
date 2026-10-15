package com.brailly.friendly.core.scan

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln

/**
 * Mengubah keluaran mentah model menjadi daftar sel Braille.
 *
 * Keluaran model berbentuk `[1, 11, H/4, W/4]`:
 * - kanal 0      : logit keberadaan pusat sel
 * - kanal 1..6   : logit titik 1..6
 * - kanal 7, 8   : geseran sub-piksel pusat (x, y) dalam satuan stride
 * - kanal 9, 10  : logaritma ukuran sel (lebar, tinggi) dalam satuan stride
 *
 * Perilakunya disetarakan dengan `brailly/postprocess.py` dan diuji terhadap
 * fixture yang dihasilkan darinya.
 */
object CellDecoder {

    /** Rasio ukuran masukan terhadap peta keluaran. */
    const val STRIDE = 4

    private const val CHANNELS = 11

    /**
     * @param maps keluaran model yang sudah dipipihkan, urutan `[kanal][y][x]`
     * @param height tinggi peta keluaran
     * @param width lebar peta keluaran
     * @param scoreThreshold ambang keyakinan minimum
     * @param scale pengali untuk memetakan piksel masukan model ke gambar asli
     */
    fun decode(
        maps: FloatArray,
        height: Int,
        width: Int,
        scoreThreshold: Float = 0.3f,
        scale: Float = 1f,
    ): List<DetectedCell> {
        require(maps.size >= CHANNELS * height * width) {
            "ukuran keluaran tidak cukup: ${maps.size} untuk ${CHANNELS}x${height}x$width"
        }
        val area = height * width
        val heat = FloatArray(area) { sigmoid(maps[it]) }
        val hasil = mutableListOf<DetectedCell>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val v = heat[i]
                if (v < scoreThreshold) continue
                if (v != localMax(heat, height, width, y, x)) continue

                var mask = 0
                var maxDot = 0f
                for (d in 0 until 6) {
                    val p = sigmoid(maps[(1 + d) * area + i])
                    if (p > 0.5f) mask = mask or (1 shl d)
                    if (p > maxDot) maxDot = p
                }
                // Sel harus memiliki setidaknya satu titik timbul.
                if (maxDot <= 0.5f) continue

                val ox = maps[7 * area + i]
                val oy = maps[8 * area + i]
                val w = exp(maps[9 * area + i].coerceIn(-3f, 6f))
                val h = exp(maps[10 * area + i].coerceIn(-3f, 6f))
                hasil.add(
                    DetectedCell(
                        cx = (x + ox) * STRIDE * scale,
                        cy = (y + oy) * STRIDE * scale,
                        w = w * STRIDE * scale,
                        h = h * STRIDE * scale,
                        score = v,
                        mask = mask,
                    )
                )
            }
        }
        return suppressOverlaps(hasil)
    }

    /** Nilai tertinggi pada jendela 3x3 di sekitar (y, x), tepi diabaikan. */
    private fun localMax(heat: FloatArray, height: Int, width: Int, y: Int, x: Int): Float {
        var m = Float.NEGATIVE_INFINITY
        for (dy in -1..1) {
            val yy = y + dy
            if (yy < 0 || yy >= height) continue
            for (dx in -1..1) {
                val xx = x + dx
                if (xx < 0 || xx >= width) continue
                val v = heat[yy * width + xx]
                if (v > m) m = v
            }
        }
        return m
    }

    /**
     * Buang deteksi ganda untuk sel yang sama. Sel bertetangga berjarak sekitar
     * 1,5 kali lebarnya, sehingga apa pun yang lebih dekat dari itu adalah
     * hipotesis bersaing untuk sel yang sama.
     */
    fun suppressOverlaps(cells: List<DetectedCell>): List<DetectedCell> {
        if (cells.size < 2) return cells
        val urut = cells.sortedByDescending { it.score }
        val simpan = BooleanArray(urut.size) { true }
        for (i in urut.indices) {
            if (!simpan[i]) continue
            val a = urut[i]
            for (j in i + 1 until urut.size) {
                if (!simpan[j]) continue
                val b = urut[j]
                if (abs(b.cx - a.cx) < 0.75f * a.w && abs(b.cy - a.cy) < 0.5f * a.h) {
                    simpan[j] = false
                }
            }
        }
        return urut.filterIndexed { i, _ -> simpan[i] }
    }

    /**
     * Tinggi sel yang didukung paling banyak deteksi, dihitung sebagai modus
     * berbobot keyakinan atas log tinggi.
     *
     * Deteksi palsu dari tekstur latar cenderung kecil dan berkeyakinan rendah,
     * sedangkan Braille sungguhan pada satu halaman berukuran seragam.
     */
    fun robustCellHeight(cells: List<DetectedCell>): Float {
        val hs = DoubleArray(cells.size) { ln(cells[it].h.toDouble()) }
        val bobot = DoubleArray(cells.size) { cells[it].score.toDouble() * cells[it].score }
        val min = hs.min()
        val max = hs.max()
        // Setara np.arange(min - 0.1, max + 0.2, 0.1)
        val mulai = min - 0.1
        val akhir = max + 0.2
        val n = ceil((akhir - mulai) / 0.1 - 1e-12).toInt().coerceAtLeast(2)
        val tepi = DoubleArray(n) { mulai + it * 0.1 }
        val kantong = DoubleArray(n - 1)
        for (k in hs.indices) {
            val v = hs[k]
            if (v < tepi[0] || v > tepi[n - 1]) continue
            var idx = ((v - mulai) / 0.1).toInt()
            if (idx >= n - 1) idx = n - 2   // nilai di tepi kanan masuk kantong terakhir
            kantong[idx] += bobot[k]
        }
        // Setara np.convolve(hist, [0.5, 1, 0.5], mode="same")
        val halus = DoubleArray(kantong.size) {
            0.5 * (if (it > 0) kantong[it - 1] else 0.0) +
                kantong[it] +
                0.5 * (if (it < kantong.size - 1) kantong[it + 1] else 0.0)
        }
        var puncak = 0
        for (i in halus.indices) if (halus[i] > halus[puncak]) puncak = i
        val pusat = (tepi[puncak] + tepi[puncak + 1]) / 2
        val terpilih = hs.filter { abs(it - pusat) < 0.25 }
        return exp(if (terpilih.isNotEmpty()) median(terpilih) else median(hs.toList())).toFloat()
    }

    /**
     * Sisakan hanya sel yang ukurannya sejalan dengan mayoritas. Inilah
     * penyaring utama terhadap deteksi palsu dari tekstur latar (kayu, kain).
     */
    fun sizeConsensus(cells: List<DetectedCell>, tolerance: Double = 0.4): List<DetectedCell> {
        if (cells.size < 5) return cells
        val acuan = robustCellHeight(cells)
        return cells.filter { abs(ln(it.h / acuan).toDouble()) < tolerance }
    }

    private fun median(v: List<Double>): Double {
        val s = v.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2
    }

    private fun sigmoid(x: Float): Float = (1.0 / (1.0 + exp(-x.toDouble()))).toFloat()
}
