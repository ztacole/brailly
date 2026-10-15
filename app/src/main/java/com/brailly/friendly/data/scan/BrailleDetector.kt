package com.brailly.friendly.data.scan

import android.content.Context
import android.graphics.Bitmap
import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleTranslator
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Hasil deteksi braille dari sebuah gambar.
 */
data class BrailleScanResult(
    val cells: List<BrailleCell>,
    val rawText: String,
    val confidence: Float,
)

/**
 * Detektor braille menggunakan model LiteRT (TFLite) `braille_detection.tflite`
 * yang tersimpan di assets.
 *
 * Catatan MVP: model dimuat via Interpreter API. Karena bentuk I/O model dapat
 * bervariasi, [detect] menerapkan pra-pemrosesan gambar standar (resize +
 * normalisasi) dan mengurai output menjadi sel-sel braille. Bila model tidak
 * dapat dimuat/dijalankan, dikembalikan hasil kosong dengan confidence 0 agar
 * pipeline scan tetap aman (tidak crash).
 */
class BrailleDetector(
    context: Context,
    private val translator: BrailleTranslator,
) {
    private val appContext = context.applicationContext
    private var interpreter: Interpreter? = null

    // Ukuran input default; disesuaikan otomatis dari tensor bila tersedia.
    private var inputWidth = 224
    private var inputHeight = 224

    private fun ensureLoaded(): Interpreter? {
        interpreter?.let { return it }
        return try {
            val model = loadModelFile(MODEL_ASSET)
            val itp = Interpreter(model, Interpreter.Options().apply { numThreads = 2 })
            // Sesuaikan dimensi input dari tensor model bila berbentuk NHWC.
            val shape = itp.getInputTensor(0).shape()
            if (shape.size == 4) {
                inputHeight = shape[1].takeIf { it > 0 } ?: inputHeight
                inputWidth = shape[2].takeIf { it > 0 } ?: inputWidth
            }
            interpreter = itp
            itp
        } catch (e: Exception) {
            null
        }
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        val fd = appContext.assets.openFd(assetName)
        FileInputStream(fd.fileDescriptor).use { input ->
            val channel: FileChannel = input.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    /**
     * Jalankan deteksi pada [bitmap]. Untuk MVP, output model diinterpretasikan
     * sebagai skor per titik braille pada grid sel. Bila format tak dikenal,
     * fungsi mengembalikan hasil kosong (confidence 0) alih-alih melempar error.
     */
    fun detect(bitmap: Bitmap): BrailleScanResult {
        val itp = ensureLoaded() ?: return BrailleScanResult(emptyList(), "", 0f)
        return try {
            val input = preprocess(bitmap)
            val outputTensor = itp.getOutputTensor(0)
            val outShape = outputTensor.shape()
            val outputCount = outShape.fold(1) { acc, i -> acc * i }
            val output = ByteBuffer.allocateDirect(4 * outputCount).order(ByteOrder.nativeOrder())

            itp.run(input, output)
            output.rewind()

            val scores = FloatArray(outputCount) { output.float }
            val cells = decodeCells(scores)
            val text = translator.brailleToText(cells)
            BrailleScanResult(cells, text, confidenceOf(scores))
        } catch (e: Exception) {
            BrailleScanResult(emptyList(), "", 0f)
        }
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val buffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        resized.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            buffer.putFloat(r); buffer.putFloat(g); buffer.putFloat(b)
        }
        buffer.rewind()
        return buffer
    }

    /**
     * Ubah skor output menjadi sel braille. Setiap kelipatan 6 skor dipetakan
     * ke 6 titik sebuah sel (threshold 0.5). Implementasi generik untuk MVP.
     */
    private fun decodeCells(scores: FloatArray): List<BrailleCell> {
        if (scores.size < 6) return emptyList()
        val cells = mutableListOf<BrailleCell>()
        var i = 0
        while (i + 6 <= scores.size) {
            val dots = mutableSetOf<Int>()
            for (d in 0 until 6) {
                if (scores[i + d] >= THRESHOLD) dots.add(d + 1)
            }
            if (dots.isNotEmpty()) cells.add(BrailleCell(dots))
            i += 6
        }
        return cells
    }

    private fun confidenceOf(scores: FloatArray): Float {
        if (scores.isEmpty()) return 0f
        val strong = scores.count { it >= THRESHOLD }
        return (strong.toFloat() / scores.size).coerceIn(0f, 1f)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    companion object {
        private const val MODEL_ASSET = "braille_detection.tflite"
        private const val THRESHOLD = 0.5f
    }
}
