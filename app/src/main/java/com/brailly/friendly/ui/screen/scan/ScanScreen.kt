package com.brailly.friendly.ui.screen.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.material3.LinearProgressIndicator
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

        val modelState by viewModel.modelState.collectAsStateWithLifecycle()
        ModelBanner(state = modelState, onDownload = viewModel::downloadModel)

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
private fun ModelBanner(state: ModelState, onDownload: () -> Unit) {
    when (state) {
        // Model siap → tidak perlu banner apa pun.
        is ModelState.Ready -> Unit

        is ModelState.NotDownloaded -> {
            BraillyCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mode offline (Gemma)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (state.canDownload) {
                            "Unduh model Gemma agar perapian teks bekerja offline. " +
                                "Tanpa model, aplikasi memakai fallback online bila tersedia."
                        } else {
                            "Model offline belum dikonfigurasi. Aplikasi memakai fallback online bila tersedia."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.canDownload) {
                        PrimaryButton("Unduh Model Offline", onDownload, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        is ModelState.Downloading -> {
            BraillyCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mengunduh model Gemma…", style = MaterialTheme.typography.titleSmall)
                    val fraction = state.fraction
                    if (fraction != null) {
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${(fraction * 100).toInt()}% · " +
                                "${formatBytes(state.bytesDownloaded)} / ${formatBytes(state.totalBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "Terunduh ${formatBytes(state.bytesDownloaded)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        is ModelState.Error -> {
            BraillyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gagal mengunduh model", style = MaterialTheme.typography.titleSmall)
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    PrimaryButton("Coba Lagi", onDownload, Modifier.fillMaxWidth())
                }
            }
        }

        is ModelState.Paused -> {
            BraillyCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unduhan dijeda", style = MaterialTheme.typography.titleSmall)
                    val fraction = state.fraction
                    if (fraction != null) {
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${(fraction * 100).toInt()}% · " +
                                "${formatBytes(state.bytesDownloaded)} / ${formatBytes(state.totalBytes)} tersimpan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "${formatBytes(state.bytesDownloaded)} tersimpan. ${state.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PrimaryButton("Lanjutkan Unduhan", onDownload, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "${value.toInt()} ${units[unit]}"
    else String.format("%.1f %s", value, units[unit])
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
                        val bitmap = image.toBitmap()
                        image.close()
                        onCaptured(bitmap)
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CameraPreview() {
    MaterialTheme {
        ModelBanner(ModelState.Ready) {  }
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
    val shown = result.refinedText?.takeIf { it.isNotBlank() } ?: result.detectedText
    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hasil Scan", style = MaterialTheme.typography.titleMedium)
            Text(
                shown.ifBlank { "Tidak ada braille terdeteksi. Coba lagi dengan pencahayaan lebih baik." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                buildString {
                    append("Keyakinan deteksi: ${(result.confidence * 100).toInt()}%")
                    if (result.usedGemma) append(" · Dirapikan oleh Gemma")
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
