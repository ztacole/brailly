package com.brailly.friendly.core.scan

import com.brailly.friendly.core.braille.BrailleMap
import com.brailly.friendly.core.braille.BrailleTranslator
import kotlin.math.abs
import kotlin.math.max

/**
 * Model deteksi Braille. Dipisahkan sebagai antarmuka agar seluruh logika
 * pembacaan dapat diuji tanpa perangkat Android maupun TFLite.
 */
interface BrailleModel {
    /** Tinggi masukan model (piksel). */
    val inputHeight: Int

    /** Lebar masukan model (piksel). */
    val inputWidth: Int

    /**
     * Tinggi peta keluaran. Bawaannya seperempat tinggi masukan, tetapi
     * implementasi sebaiknya membacanya dari model agar tidak mengandalkan
     * asumsi: ukuran buffer yang salah menyebabkan kegagalan di sisi native.
     */
    val outputHeight: Int get() = inputHeight / CellDecoder.STRIDE

    /** Lebar peta keluaran. */
    val outputWidth: Int get() = inputWidth / CellDecoder.STRIDE

    /**
     * Jalankan model. Masukan berupa tensor NCHW 0..1 sepanjang
     * `3 * inputHeight * inputWidth`; keluaran berupa tensor
     * `11 * (inputHeight/4) * (inputWidth/4)`.
     */
    fun run(input: FloatArray): FloatArray
}

/** Setelan pembacaan halaman. */
data class ReaderConfig(
    /** Sisi panjang yang dituju pada lintasan pertama. */
    val longSide: Int = 1024,
    /** Tinggi sel yang paling disukai model, dalam piksel masukan. */
    val targetCellHeight: Float = 36f,
    /** Batas penyimpangan tinggi sel sebelum gambar diskalakan ulang. */
    val cellHeightTolerance: Float = 1.35f,
    val maxSide: Int = 2560,
    val scoreThreshold: Float = 0.3f,
    /**
     * Bila aktif, deteksi dijalankan dua kali: hasil pertama dipakai mengukur
     * ukuran sel sebenarnya, lalu gambar diskalakan ulang dan dideteksi lagi.
     * Mematikannya memangkas waktu hampir setengah, dengan risiko akurasi
     * turun bila jarak foto tidak dipandu.
     */
    val twoPass: Boolean = true,
    val dehyphenate: Boolean = false,
    /**
     * Bila hasil bacaan terlihat tidak wajar, coba juga orientasi 90, 180, dan
     * 270 derajat lalu ambil yang terbaik. Melindungi dari foto yang terbalik
     * atau menyamping. Biayanya hanya muncul pada jepretan yang bermasalah.
     */
    val autoOrient: Boolean = true,
    /** Batas kewajaran di bawah mana orientasi lain ikut dicoba. */
    val minQuality: Float = 0.55f,
    /**
     * Jumlah sel minimum sebelum pencarian orientasi diaktifkan.
     *
     * Ukuran kewajaran bertumpu pada bagian pola yang dikenal, dan bagian itu
     * nyaris selalu 1,0 pada bacaan yang hanya berisi beberapa sel -- misalnya
     * tombol lift atau papan nama. Pada jumlah sedikit, orientasi lain bisa
     * menang karena kebetulan dan justru memberi huruf yang salah.
     */
    val minCellsForOrientation: Int = 10,
)

/**
 * Penilaian kelayakan hasil bacaan.
 *
 * Dipisahkan dari lapisan UI agar aturannya dapat diuji, dan diletakkan
 * berdampingan dengan ukuran kewajaran yang dinilainya.
 */
object ReadingQuality {

    /** Ambang untuk bacaan panjang; dikalibrasi pada halaman Braille penuh. */
    const val THRESHOLD_LONG = 0.55f

    /** Ambang untuk bacaan pendek, yang memberi bukti jauh lebih sedikit. */
    const val THRESHOLD_SHORT = 0.30f

    /** Mulai dari jumlah sel ini, bacaan dinilai dengan ambang panjang. */
    const val LONG_READING_CELLS = 10

    /**
     * Apakah hasil bacaan patut diragukan.
     *
     * Ambangnya dibedakan menurut banyaknya sel. Ukuran kewajaran bertumpu pada
     * bagian pola yang dikenal, dan bagian itu nyaris selalu 1,0 pada bacaan
     * yang hanya berisi beberapa sel -- misalnya tombol lift atau papan nama.
     * Memakai ambang halaman penuh untuk bacaan sependek itu memunculkan
     * peringatan pada hasil yang sebenarnya benar.
     */
    fun isDoubtful(cellCount: Int, quality: Float): Boolean = when {
        cellCount <= 0 -> false
        cellCount >= LONG_READING_CELLS -> quality < THRESHOLD_LONG
        else -> quality < THRESHOLD_SHORT
    }
}

/** Hasil pembacaan satu halaman. */
data class PageReadResult(
    val layout: PageLayout,
    val cells: List<DetectedCell>,
    /**
     * Rotasi yang akhirnya dipakai. Koordinat pada [cells] mengacu pada gambar
     * setelah diputar sebesar nilai ini.
     */
    val orientationDegrees: Int = 0,
    /** Kewajaran hasil, 0..1. Nilai rendah menandakan bacaan patut diragukan. */
    val quality: Float = 0f,
) {
    val text: String get() = layout.text
    val isEmpty: Boolean get() = cells.isEmpty()

    /** Bacaan patut diragukan: biasanya cahaya kurang atau foto kabur. */
    val isDoubtful: Boolean get() = ReadingQuality.isDoubtful(cells.size, quality)
}

/**
 * Membaca halaman Braille dari piksel gambar: menjalankan model, menguraikan
 * sel, menyaring deteksi palsu, lalu menyusunnya menjadi paragraf.
 *
 * Perilakunya disetarakan dengan `BraillyReader` pada `brailly/pipeline.py`.
 */
class BraillePageReader(
    private val model: BrailleModel,
    private val config: ReaderConfig = ReaderConfig(),
    translator: BrailleTranslator = BrailleTranslator(),
) {
    private val layoutAnalyzer = LayoutAnalyzer(translator)

    fun read(pixels: IntArray, width: Int, height: Int): PageReadResult {
        var gambar = PixelImage(pixels, width, height)
        var terbaik = detectCells(gambar.pixels, gambar.width, gambar.height)
        var mutuTerbaik = quality(terbaik)
        var rotasiTerbaik = 0

        if (config.autoOrient &&
            terbaik.size >= config.minCellsForOrientation &&
            mutuTerbaik < config.minQuality
        ) {
            for (derajat in intArrayOf(90, 180, 270)) {
                val diputar = PixelRotation.rotate(PixelImage(pixels, width, height), derajat)
                val sel = detectCells(diputar.pixels, diputar.width, diputar.height)
                val mutu = quality(sel)
                if (mutu > mutuTerbaik) {
                    terbaik = sel
                    mutuTerbaik = mutu
                    rotasiTerbaik = derajat
                    gambar = diputar
                }
            }
        }
        return PageReadResult(
            layout = layoutAnalyzer.analyze(terbaik, config.dehyphenate),
            cells = terbaik,
            orientationDegrees = rotasiTerbaik,
            quality = mutuTerbaik,
        )
    }

    /**
     * Kewajaran hasil deteksi: keyakinan rata-rata dikali bagian pola yang
     * dikenal. Halaman yang terbaca pada orientasi salah tetap menghasilkan
     * banyak sel, tetapi keyakinannya rendah dan polanya banyak yang asing --
     * dua gejala yang sama-sama tertangkap ukuran ini.
     */
    fun quality(cells: List<DetectedCell>): Float {
        if (cells.isEmpty()) return 0f
        val skor = cells.map { it.score }.average().toFloat()
        val dikenal = cells.count { BrailleMap.isKnown(it.mask) }.toFloat() / cells.size
        return skor * dikenal
    }

    /**
     * Deteksi sel dengan penyesuaian skala otomatis, lalu penyaringan ukuran.
     */
    fun detectCells(pixels: IntArray, width: Int, height: Int): List<DetectedCell> {
        val sisiPanjang = max(width, height)
        val skalaAwal = config.longSide.toFloat() / sisiPanjang
        var cells = detectOnce(pixels, width, height, skalaAwal)

        if (config.twoPass && cells.size >= 5) {
            val skalaTerpakai = skalaTerpakai(width, height, skalaAwal)
            val tinggiSel = CellDecoder.robustCellHeight(cells) * skalaTerpakai
            val rasio = config.targetCellHeight / max(tinggiSel, 1e-3f)
            if (rasio > config.cellHeightTolerance || rasio < 1f / config.cellHeightTolerance) {
                val skalaBaru = minOf(
                    skalaTerpakai * rasio,
                    config.maxSide.toFloat() / sisiPanjang,
                )
                if (abs(skalaBaru / skalaTerpakai - 1f) > 0.15f) {
                    val ulang = detectOnce(pixels, width, height, skalaBaru)
                    // Hasil ulangan hanya dipakai bila tidak kehilangan banyak sel.
                    if (ulang.size >= 0.6f * cells.size) cells = ulang
                }
            }
        }
        return CellDecoder.sizeConsensus(cells)
    }

    private fun detectOnce(
        pixels: IntArray,
        width: Int,
        height: Int,
        requestedScale: Float,
    ): List<DetectedCell> {
        val siap = ImagePreprocessor.prepare(
            pixels, width, height, model.inputHeight, model.inputWidth, requestedScale,
        )
        val maps = model.run(siap.tensor)
        return CellDecoder.decode(
            maps = maps,
            height = model.outputHeight,
            width = model.outputWidth,
            scoreThreshold = config.scoreThreshold,
            scale = siap.inverseScale,
        )
    }

    /** Skala yang benar-benar terpakai setelah dibatasi bentuk masukan tetap. */
    private fun skalaTerpakai(width: Int, height: Int, requested: Float): Float = minOf(
        requested,
        model.inputHeight.toFloat() / height,
        model.inputWidth.toFloat() / width,
    )
}
