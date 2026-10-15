package com.brailly.friendly.data.scan

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.core.scan.BrailleModel
import com.brailly.friendly.core.scan.BraillePageReader
import com.brailly.friendly.core.scan.CellDecoder
import com.brailly.friendly.core.scan.PageReadResult
import com.brailly.friendly.core.scan.ReaderConfig
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Satu paragraf hasil pemindaian. */
data class ScannedParagraph(
    val text: String,
    val isHeading: Boolean,
)

/** Satu baris hasil pemindaian, lengkap dengan kode Braille-nya. */
data class ScannedLine(
    val text: String,
    val braille: String,
)

/**
 * Hasil deteksi braille dari sebuah gambar.
 *
 * [rawText] memuat seluruh teks halaman; [paragraphs] dan [lines] menyediakan
 * strukturnya untuk ditampilkan atau dibacakan.
 */
data class BrailleScanResult(
    val cells: List<BrailleCell>,
    val rawText: String,
    val confidence: Float,
    val paragraphs: List<ScannedParagraph> = emptyList(),
    val lines: List<ScannedLine> = emptyList(),
    val cellCount: Int = 0,
    val angleDegrees: Float = 0f,
) {
    val isEmpty: Boolean get() = cellCount == 0

    companion object {
        val EMPTY = BrailleScanResult(emptyList(), "", 0f)
    }
}

/** Gagal memuat atau menjalankan model deteksi. */
class BrailleDetectorException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Detektor braille berbasis model LiteRT (TFLite) `brailly.tflite`.
 *
 * Model mendeteksi pusat setiap sel Braille beserta keenam titiknya, lalu
 * [BraillePageReader] menyusunnya menjadi baris, spasi, dan paragraf.
 *
 * Kelas ini sengaja dibuat tipis: seluruh logika pembacaan berada di paket
 * `core.scan` yang bebas dependensi Android sehingga dapat diuji lewat unit
 * test. Di sini hanya ada pemuatan model, konversi [Bitmap], dan pemanggilan
 * interpreter.
 *
 * Masukan model: `[1, 3, 1024, 768]` float32 NCHW, RGB, rentang 0..1.
 * Normalisasi ImageNet sudah menyatu di dalam graf model, jadi JANGAN
 * dikurangi mean/std lagi di sini.
 */
class BrailleDetector(
    context: Context,
    private val translator: BrailleTranslator,
    private val config: ReaderConfig = ReaderConfig(),
    private val numThreads: Int = 4,
) {
    private val appContext = context.applicationContext
    private var interpreter: Interpreter? = null
    private var reader: BraillePageReader? = null

    /** Pesan kesalahan terakhir saat memuat model, untuk ditampilkan di UI. */
    var lastError: String? = null
        private set

    private fun ensureReader(): BraillePageReader {
        reader?.let { return it }
        val itp = try {
            Interpreter(
                loadModelFile(MODEL_ASSET),
                Interpreter.Options().apply { numThreads = this@BrailleDetector.numThreads },
            )
        } catch (e: Exception) {
            lastError = "Model $MODEL_ASSET gagal dimuat: ${e.message}"
            Log.e(TAG, lastError, e)
            throw BrailleDetectorException(lastError!!, e)
        }

        val shape = itp.getInputTensor(0).shape()
        // Model berbentuk NCHW: [batch, kanal, tinggi, lebar].
        if (shape.size != 4 || shape[1] != 3) {
            itp.close()
            lastError = "Bentuk masukan model tidak dikenali: ${shape.joinToString("x")}"
            Log.e(TAG, lastError!!)
            throw BrailleDetectorException(lastError!!)
        }
        val tinggi = shape[2]
        val lebar = shape[3]
        Log.i(TAG, "Model dimuat: masukan ${lebar}x$tinggi NCHW, $numThreads utas")

        interpreter = itp
        val model = LiteRtModel(itp, tinggi, lebar)
        return BraillePageReader(model, config, translator).also { reader = it }
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        appContext.assets.openFd(assetName).use { fd ->
            FileInputStream(fd.fileDescriptor).use { input ->
                val channel: FileChannel = input.channel
                return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    /**
     * Jalankan deteksi pada [bitmap].
     *
     * Mengembalikan hasil kosong bila tidak ada Braille yang terdeteksi, dan
     * melempar [BrailleDetectorException] bila modelnya sendiri bermasalah.
     * Keduanya dibedakan secara sengaja: "tidak ada Braille" adalah hasil yang
     * sah, sedangkan model gagal dimuat adalah kesalahan yang perlu diketahui.
     */
    fun detect(bitmap: Bitmap): BrailleScanResult {
        val pembaca = ensureReader()
        val siap = toArgbPixels(bitmap)
        val hasil = pembaca.read(siap.pixels, siap.width, siap.height)
        return hasil.toScanResult()
    }

    private class ArgbImage(val pixels: IntArray, val width: Int, val height: Int)

    private fun toArgbPixels(bitmap: Bitmap): ArgbImage {
        // getPixels selalu mengembalikan ARGB_8888, tetapi bitmap berformat
        // HARDWARE harus disalin dahulu agar bisa dibaca.
        val bmp = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
        val piksel = IntArray(bmp.width * bmp.height)
        bmp.getPixels(piksel, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        return ArgbImage(piksel, bmp.width, bmp.height)
    }

    private fun PageReadResult.toScanResult(): BrailleScanResult {
        if (cells.isEmpty()) return BrailleScanResult.EMPTY
        return BrailleScanResult(
            cells = cells.map { it.toBrailleCell() },
            rawText = layout.text,
            confidence = cells.map { it.score }.average().toFloat(),
            paragraphs = layout.paragraphs.map { ScannedParagraph(it.text, it.isHeading) },
            lines = layout.lines.map { ScannedLine(it.text, it.braille) },
            cellCount = cells.size,
            angleDegrees = layout.angleDegrees,
        )
    }

    /** Pembungkus interpreter LiteRT sebagai [BrailleModel]. */
    private class LiteRtModel(
        private val interpreter: Interpreter,
        override val inputHeight: Int,
        override val inputWidth: Int,
    ) : BrailleModel {

        private val outputHeight = inputHeight / CellDecoder.STRIDE
        private val outputWidth = inputWidth / CellDecoder.STRIDE
        private val outputCount = OUTPUT_CHANNELS * outputHeight * outputWidth

        private val inputBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(4 * 3 * inputHeight * inputWidth)
                .order(ByteOrder.nativeOrder())
        private val outputBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(4 * outputCount).order(ByteOrder.nativeOrder())

        override fun run(input: FloatArray): FloatArray {
            require(input.size == 3 * inputHeight * inputWidth) {
                "ukuran tensor masukan salah: ${input.size}"
            }
            inputBuffer.rewind()
            val f = inputBuffer.asFloatBuffer()
            f.rewind()
            f.put(input)
            inputBuffer.rewind()
            outputBuffer.rewind()

            interpreter.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val keluar = FloatArray(outputCount)
            outputBuffer.asFloatBuffer().get(keluar)
            return keluar
        }

        private companion object {
            const val OUTPUT_CHANNELS = 11
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        reader = null
    }

    companion object {
        private const val TAG = "BrailleDetector"
        private const val MODEL_ASSET = "brailly.tflite"
    }
}
