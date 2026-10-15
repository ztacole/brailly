package com.brailly.friendly.core.scan

/** Gambar dalam bentuk piksel ARGB beserta ukurannya. */
data class PixelImage(val pixels: IntArray, val width: Int, val height: Int) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Pemutaran piksel pada kelipatan 90 derajat.
 *
 * Diperlukan karena orientasi sel Braille menentukan arti polanya: sel yang
 * miring 90 derajat tetap terdeteksi sebagai sel, tetapi keenam titiknya
 * terbaca pada posisi yang salah sehingga menghasilkan huruf yang keliru.
 */
object PixelRotation {

    /** Putar searah jarum jam sebesar [degrees] (0, 90, 180, atau 270). */
    fun rotate(image: PixelImage, degrees: Int): PixelImage {
        val d = ((degrees % 360) + 360) % 360
        require(d % 90 == 0) { "rotasi harus kelipatan 90 derajat, bukan $degrees" }
        if (d == 0) return image

        val w = image.width
        val h = image.height
        val src = image.pixels
        return when (d) {
            90 -> {
                val out = IntArray(w * h)
                // (x, y) -> (h - 1 - y, x); lebar dan tinggi bertukar
                for (y in 0 until h) {
                    val row = y * w
                    for (x in 0 until w) out[x * h + (h - 1 - y)] = src[row + x]
                }
                PixelImage(out, h, w)
            }

            180 -> {
                val n = w * h
                val out = IntArray(n)
                for (i in 0 until n) out[n - 1 - i] = src[i]
                PixelImage(out, w, h)
            }

            else -> {
                val out = IntArray(w * h)
                // 270 derajat: (x, y) -> (y, w - 1 - x)
                for (y in 0 until h) {
                    val row = y * w
                    for (x in 0 until w) out[(w - 1 - x) * h + y] = src[row + x]
                }
                PixelImage(out, h, w)
            }
        }
    }
}
