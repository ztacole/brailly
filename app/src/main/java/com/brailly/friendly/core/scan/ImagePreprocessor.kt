package com.brailly.friendly.core.scan

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/** Tensor masukan model beserta skala yang dipakai saat menyiapkannya. */
data class PreparedInput(
    /** Nilai piksel urutan NCHW (3 x tinggi x lebar), rentang 0..1. */
    val tensor: FloatArray,
    /** Pengali dari piksel gambar asli ke piksel masukan model. */
    val scale: Float,
    val targetHeight: Int,
    val targetWidth: Int,
) {
    /** Kebalikan [scale]: pemetaan koordinat model kembali ke gambar asli. */
    val inverseScale: Float get() = 1f / scale

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Menyiapkan gambar kamera menjadi tensor masukan model.
 *
 * Model memakai bentuk masukan tetap, sehingga gambar diperkecil sampai muat
 * seluruhnya lalu sisanya dilapisi dengan piksel tepi. Gambar tidak pernah
 * dipotong, agar tidak ada Braille yang hilang.
 *
 * Penyusutan memakai rata-rata area, sama seperti `cv2.INTER_AREA` pada
 * implementasi acuan, dan diuji terhadap fixture yang dihasilkan darinya.
 * Normalisasi ImageNet TIDAK dilakukan di sini karena sudah menyatu di dalam
 * graf model.
 */
object ImagePreprocessor {

    /**
     * @param pixels piksel ARGB sebagaimana dikembalikan `Bitmap.getPixels`
     * @param width lebar gambar asli
     * @param height tinggi gambar asli
     * @param targetHeight tinggi masukan model
     * @param targetWidth lebar masukan model
     * @param requestedScale batas atas skala; dipakai agar sel Braille jatuh
     *   pada ukuran yang disukai model. Skala akhir tetap dibatasi agar gambar
     *   muat di dalam bentuk tetap.
     */
    fun prepare(
        pixels: IntArray,
        width: Int,
        height: Int,
        targetHeight: Int,
        targetWidth: Int,
        requestedScale: Float = Float.MAX_VALUE,
    ): PreparedInput {
        require(width > 0 && height > 0) { "ukuran gambar tidak valid: ${width}x$height" }
        require(pixels.size >= width * height) { "jumlah piksel kurang dari ${width}x$height" }

        val scale = min(
            requestedScale.toDouble(),
            min(targetHeight.toDouble() / height, targetWidth.toDouble() / width),
        )
        // cv2.resize membulatkan ukuran keluaran ke bilangan terdekat.
        val smallW = Math.rint(width * scale).toInt().coerceIn(1, targetWidth)
        val smallH = Math.rint(height * scale).toInt().coerceIn(1, targetHeight)

        val area = targetHeight * targetWidth
        val tensor = FloatArray(3 * area)
        val r = FloatArray(smallW)
        val g = FloatArray(smallW)
        val b = FloatArray(smallW)

        // Langkah pengambilan sampel adalah 1/skala, BUKAN lebar/lebar_kecil.
        // Inilah yang dilakukan cv2.resize ketika ukuran keluaran tidak
        // ditentukan secara eksplisit; memakai lebar/lebar_kecil menggeser
        // jendela sampel dan membuat hasilnya menyimpang jauh.
        val step = 1.0 / scale
        for (ty in 0 until smallH) {
            areaAverageRow(pixels, width, height, smallW, step, ty, r, g, b)
            val rowBase = ty * targetWidth
            for (tx in 0 until smallW) {
                tensor[rowBase + tx] = r[tx]
                tensor[area + rowBase + tx] = g[tx]
                tensor[2 * area + rowBase + tx] = b[tx]
            }
            // Lapisi sisa kanan dengan piksel tepi (BORDER_REPLICATE).
            for (tx in smallW until targetWidth) {
                tensor[rowBase + tx] = r[smallW - 1]
                tensor[area + rowBase + tx] = g[smallW - 1]
                tensor[2 * area + rowBase + tx] = b[smallW - 1]
            }
        }
        // Lapisi sisa bawah dengan menyalin baris terakhir.
        for (ty in smallH until targetHeight) {
            val src = (smallH - 1) * targetWidth
            val dst = ty * targetWidth
            for (c in 0 until 3) {
                System.arraycopy(tensor, c * area + src, tensor, c * area + dst, targetWidth)
            }
        }
        return PreparedInput(tensor, scale.toFloat(), targetHeight, targetWidth)
    }

    /**
     * Hitung satu baris keluaran sebagai rata-rata berbobot area dari kotak
     * piksel masukan yang bersesuaian, termasuk bobot pecahan di tepinya.
     */
    private fun areaAverageRow(
        pixels: IntArray,
        width: Int,
        height: Int,
        smallW: Int,
        step: Double,
        ty: Int,
        outR: FloatArray,
        outG: FloatArray,
        outB: FloatArray,
    ) {
        val y0 = ty * step
        val y1 = y0 + step
        // Di tepi bawah/kanan, kotak sampel terpotong; pembaginya mengikuti
        // bagian yang benar-benar tersedia, seperti cellHeight/cellWidth OpenCV.
        val cellH = min(step, height - y0)
        val yaMin = floor(y0).toInt().coerceAtLeast(0)
        val yaMax = ceil(y1).toInt().coerceAtMost(height)

        for (tx in 0 until smallW) {
            val x0 = tx * step
            val x1 = x0 + step
            val cellW = min(step, width - x0)
            val xaMin = floor(x0).toInt().coerceAtLeast(0)
            val xaMax = ceil(x1).toInt().coerceAtMost(width)

            var sumR = 0.0
            var sumG = 0.0
            var sumB = 0.0
            for (y in yaMin until yaMax) {
                val wy = min(y + 1.0, y1) - maxOf(y.toDouble(), y0)
                if (wy <= 0) continue
                val row = y * width
                val fy = wy / cellH
                for (x in xaMin until xaMax) {
                    val wx = min(x + 1.0, x1) - maxOf(x.toDouble(), x0)
                    if (wx <= 0) continue
                    val bobot = fy * (wx / cellW)
                    val p = pixels[row + x]
                    sumR += bobot * ((p shr 16) and 0xFF)
                    sumG += bobot * ((p shr 8) and 0xFF)
                    sumB += bobot * (p and 0xFF)
                }
            }
            outR[tx] = (sumR / 255.0).toFloat()
            outG[tx] = (sumG / 255.0).toFloat()
            outB[tx] = (sumB / 255.0).toFloat()
        }
    }
}
