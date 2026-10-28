package com.brailly.friendly.ui.screen.scan

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.layout.Row
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
import com.brailly.friendly.data.speech.TextToSpeechManager
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.component.ScreenHeader
import com.brailly.friendly.ui.component.SecondaryButton
import com.brailly.friendly.ui.component.SpeakButton
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.util.copyToClipboard
import com.brailly.friendly.util.shareText
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Penanda pembacaan hasil pemindaian yang sedang tampil. */
private const val SCAN_RESULT_UTTERANCE_ID = "scan-result"

@Composable
fun ScanScreen(
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val speaker: TextToSpeechManager = koinInject()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // ImageCapture dipegang di tingkat layar, bukan di dalam CameraView,
    // supaya tombol jepret dan pemanggil lain memakai use case yang sama.
    val imageCapture = remember { ImageCapture.Builder().build() }
    val takePicture = { captureTo(context, imageCapture, viewModel::onImageCaptured) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = "Scan Braille",
            subtitle = "Arahkan kamera ke teks braille fisik",
            trailing = {
                IconActionButton(
                    icon = BraillyIcons.History,
                    contentDescription = "Buka riwayat scan",
                    onClick = onOpenHistory,
                )
            },
        )

        when (val s = state) {
            is ScanState.Result -> ResultView(
                result = s,
                onScanAgain = {
                    speaker.stop()
                    viewModel.reset()
                },
            )
            is ScanState.Processing -> ProcessingView()
            is ScanState.Error -> ErrorView(s.message, onRetry = viewModel::reset)
            else -> {
                if (!hasPermission) {
                    PermissionView { permissionLauncher.launch(Manifest.permission.CAMERA) }
                } else {
                    CameraView(imageCapture = imageCapture, onCapture = takePicture)
                }
            }
        }
    }
}

@Composable
private fun CameraView(imageCapture: ImageCapture, onCapture: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

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
        onClick = onCapture,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = BraillyIcons.Camera,
    )
}

private fun captureTo(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Bitmap) -> Unit,
) {
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
    val context = LocalContext.current

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

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SpeakButton(
                        text = result.text,
                        contentDescription = "Bacakan hasil scan",
                        utteranceId = SCAN_RESULT_UTTERANCE_ID,
                    )
                    IconActionButton(
                        icon = BraillyIcons.Copy,
                        contentDescription = "Salin hasil scan",
                        onClick = {
                            context.copyToClipboard(
                                result.text,
                                confirmation = "Hasil scan disalin",
                            )
                        },
                    )
                    IconActionButton(
                        icon = BraillyIcons.Share,
                        contentDescription = "Bagikan hasil scan",
                        onClick = { context.shareText(result.text) },
                    )
                }
            }
        }
    }
    SecondaryButton("Scan Lagi", onScanAgain, Modifier.fillMaxWidth(), leadingIcon = BraillyIcons.Camera)
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    BraillyCard(modifier = Modifier.fillMaxWidth(), containerColor = Ds.colors.wrongContainer) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Terjadi kesalahan", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            PrimaryButton("Coba Lagi", onRetry, Modifier.fillMaxWidth())
        }
    }
}
