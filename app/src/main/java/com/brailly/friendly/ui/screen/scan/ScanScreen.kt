package com.brailly.friendly.ui.screen.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.component.ScreenHeader
import com.brailly.friendly.ui.component.SecondaryButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = "Scan Braille", subtitle = "Arahkan kamera ke teks braille fisik")

        when (val s = state) {
            is ScanState.Result -> ResultView(s, onScanAgain = viewModel::reset)
            is ScanState.Processing -> ProcessingView()
            is ScanState.Error -> ErrorView(s.message, onRetry = viewModel::reset)
            else -> {
                if (!hasPermission) {
                    PermissionView { permissionLauncher.launch(Manifest.permission.CAMERA) }
                } else {
                    CameraView(onCaptured = viewModel::onImageCaptured)
                }
            }
        }
    }
}

@Composable
private fun CameraView(onCaptured: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )
        // Frame guide sederhana
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        )
    }

    PrimaryButton(
        text = "Ambil Gambar",
        onClick = {
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        // toBitmap() memberi gambar dalam orientasi sensor, bukan
                        // orientasi layar. Pada ponsel tegak nilainya biasanya 90
                        // derajat. PreviewView menerapkannya sendiri sehingga
                        // pratinjau tampak benar, tetapi bitmap hasil jepretan
                        // tetap miring -- dan sel Braille yang miring membuat
                        // pola titiknya terbaca salah.
                        val derajat = image.imageInfo.rotationDegrees
                        val bitmap = image.toBitmap()
                        image.close()
                        onCaptured(bitmap.diputar(derajat))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        // Kegagalan capture diabaikan; pengguna dapat mencoba lagi.
                    }
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = BraillyIcons.Camera,
    )
}


/**
 * Putar bitmap sesuai orientasi sensor kamera. Tanpa ini, sel Braille ikut
 * miring dan pola titiknya terbaca salah meski posisi selnya tetap ditemukan.
 */
private fun Bitmap.diputar(degrees: Int): Bitmap {
    val d = ((degrees % 360) + 360) % 360
    if (d == 0) return this
    val matrix = Matrix().apply { postRotate(d.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ResultPreview() {
    MaterialTheme {
        ResultView(
            result = ScanState.Result(
                text = "Kemerdekaan Indonesia",
                paragraphs = listOf(
                    com.brailly.friendly.data.scan.ScannedParagraph("Kemerdekaan Indonesia", true),
                    com.brailly.friendly.data.scan.ScannedParagraph(
                        "Pada tanggal 17 Agustus 1945, bangsa Indonesia memproklamasikan kemerdekaan.",
                        false,
                    ),
                ),
                lines = emptyList(),
                confidence = 0.93f,
                cellCount = 86,
                quality = 0.82f,
                knownFraction = 0.95f,
            ),
            onScanAgain = {},
        )
    }
}

@Composable
private fun PermissionView(onRequest: () -> Unit) {
    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                BraillyIcons.Camera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text("Izin kamera diperlukan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Brailly membutuhkan akses kamera untuk memindai teks braille.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton("Izinkan Kamera", onRequest, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProcessingView() {
    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text("Memproses braille...", style = MaterialTheme.typography.titleMedium)
            Text(
                "Menganalisis pola titik dan menyusun teks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultView(result: ScanState.Result, onScanAgain: () -> Unit) {
    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hasil Scan", style = MaterialTheme.typography.titleMedium)
            if (result.isEmpty) {
                Text(
                    "Tidak ada braille terdeteksi. Coba lagi dengan cahaya dari samping " +
                        "agar titik-titiknya membentuk bayangan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                // Judul ditampilkan tebal, paragraf dipisah agar mudah dibaca.
                result.paragraphs.forEach { paragraf ->
                    Text(
                        paragraf.text,
                        style = if (paragraf.isHeading) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (result.isLikelyOtherLanguage) {
                    Text(
                        "Titik-titiknya terbaca jelas, tetapi banyak polanya tidak ada " +
                            "dalam tabel Braille Indonesia. Halaman ini kemungkinan " +
                            "memakai Braille bahasa lain, sehingga sebagian huruf " +
                            "ditampilkan sebagai simbol Braille apa adanya.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (result.isDoubtful) {
                    Text(
                        "Hasil ini kurang dapat diandalkan. Coba foto ulang dengan cahaya " +
                            "dari samping agar titik-titiknya membentuk bayangan, dan " +
                            "pastikan halaman tidak miring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "Keyakinan deteksi: ${(result.confidence * 100).toInt()}% \u00b7 " +
                        "${result.cellCount} sel \u00b7 ${result.paragraphs.size} paragraf",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    SecondaryButton("Scan Lagi", onScanAgain, Modifier.fillMaxWidth(), leadingIcon = BraillyIcons.Camera)
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    BraillyCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.errorContainer) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Terjadi kesalahan", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            PrimaryButton("Coba Lagi", onRetry, Modifier.fillMaxWidth())
        }
    }
}
