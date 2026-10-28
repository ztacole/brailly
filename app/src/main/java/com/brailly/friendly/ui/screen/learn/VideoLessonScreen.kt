package com.brailly.friendly.ui.screen.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brailly.friendly.BuildConfig
import com.brailly.friendly.core.learn.VideoLessonCatalog
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.component.SecondaryButton
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsProgressBar
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * Layar materi video pembuka sebuah unit level.
 *
 * Video diputar lewat YouTube IFrame Player di dalam WebView, sehingga
 * tontonan tercatat di YouTube dan iklannya tetap tampil. Dua batasan dari
 * ketentuan YouTube ikut menentukan tata letak layar ini:
 *
 * 1. Tidak boleh ada elemen apa pun di depan pemutar atau kontrolnya. Karena
 *    itu seluruh keterangan, bilah progres, dan tombol diletakkan di bawah
 *    pemutar, bukan menimpanya.
 * 2. Kontrol pemutar tidak boleh dinonaktifkan, jadi pengguna tetap bisa
 *    menggeser bilah waktu. Gerbang unit dijaga dari sisi penghitungan di
 *    [LearnViewModel.onVideoProgress], bukan dengan memblokir kontrol.
 */
@Composable
fun VideoLessonScreen(
    video: VideoPlaybackState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onDuration: (Float) -> Unit,
    onProgress: (Float) -> Unit,
    onEnded: () -> Unit,
    onForceComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lesson = video.lesson
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val playerReady = remember(lesson.videoId) { mutableStateOf(false) }
    val playerError = remember(lesson.videoId) { mutableStateOf<PlayerConstants.PlayerError?>(null) }

    // Callback terbaru dibaca lewat rememberUpdatedState agar listener pemutar
    // cukup dibuat sekali per video, tanpa memegang lambda yang kedaluwarsa.
    val latestDuration by rememberUpdatedState(onDuration)
    val latestProgress by rememberUpdatedState(onProgress)
    val latestEnded by rememberUpdatedState(onEnded)

    val listener = remember(lesson.videoId) {
        object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                playerReady.value = true
                // cueVideo, bukan loadVideo: video tidak diputar otomatis,
                // pengguna menekan tombol putar sendiri.
                youTubePlayer.cueVideo(lesson.videoId, 0f)
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                latestDuration(duration)
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                latestProgress(second)
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState,
            ) {
                if (state == PlayerConstants.PlayerState.ENDED) latestEnded()
            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError,
            ) {
                playerError.value = error
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconActionButton(
                icon = BraillyIcons.Back,
                contentDescription = "Kembali ke jalur kuis",
                onClick = onBack,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    lesson.title,
                    style = Ds.text.sectionHeader,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Materi ${lesson.index + 1} dari ${VideoLessonCatalog.lessons.size}",
                    style = Ds.text.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Pemutar: lebar penuh, rasio 16:9 diatur sendiri oleh YouTubePlayerView.
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                // Inisialisasi otomatis dimatikan agar pemutar bisa dibuat
                // dengan IFramePlayerOptions sendiri. Tanpa ini pemutar memakai
                // UI native pustaka, dan tombol putar pada video yang hanya
                // di-cue tidak selalu memicu pemutaran di dalam WebView.
                YouTubePlayerView(context).apply {
                    enableAutomaticInitialization = false

                    // controls=1 + fs=0 menampilkan tombol putar dan bilah
                    // kontrol resmi YouTube di dalam WebView, sehingga pengguna
                    // benar-benar bisa memulai video. origin disetel eksplisit
                    // supaya permintaan IFrame diterima YouTube.
                    val options = IFramePlayerOptions.Builder(context)
                        .controls(1)
                        .fullscreen(0)
                        .rel(0)
                        .build()

                    // Diikutkan ke siklus hidup host supaya pemutaran berhenti
                    // saat aplikasi ke latar belakang. Pemutaran di latar
                    // belakang dilarang ketentuan YouTube.
                    lifecycle.addObserver(this)
                    initialize(listener, true, options)
                }
            },
            onRelease = { view ->
                lifecycle.removeObserver(view)
                view.release()
            },
        )

        Text(
            lesson.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GateStatusCard(
            video = video,
            playerReady = playerReady.value,
            error = playerError.value,
        )

        PrimaryButton(
            text = if (video.requirementMet) "Lanjut ke Level" else "Tonton untuk Melanjutkan",
            onClick = onContinue,
            enabled = video.requirementMet,
            leadingIcon = BraillyIcons.Play,
            modifier = Modifier.fillMaxWidth(),
        )

        // Pintasan khusus build debug: menguji level tanpa menonton video utuh.
        if (BuildConfig.DEBUG && !video.requirementMet) {
            SecondaryButton(
                text = "Tandai selesai (debug)",
                onClick = onForceComplete,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** Kartu status gerbang: progres tontonan, pesan tunggu, dan pesan galat. */
@Composable
private fun GateStatusCard(
    video: VideoPlaybackState,
    playerReady: Boolean,
    error: PlayerConstants.PlayerError?,
) {
    val message = when {
        error != null -> errorMessage(error)
        video.completed -> "Materi sudah tuntas. Level pada unit ini terbuka."
        !playerReady -> "Menyiapkan pemutar. Materi video memerlukan koneksi internet."
        video.durationSeconds <= 0f ->
            "Tekan tombol putar untuk memulai materi."
        else -> {
            val percent = (LearnViewModel.VIDEO_PASS_RATIO * 100).toInt()
            "Tonton minimal $percent% materi untuk membuka level. " +
                "Sisa sekitar ${formatSeconds(video.remainingSeconds)}."
        }
    }

    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (video.completed) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DsProgressBar(progress = video.gateProgress)
            Text(
                message,
                style = Ds.text.helper,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Diumumkan ulang oleh pembaca layar setiap statusnya berubah.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private fun errorMessage(error: PlayerConstants.PlayerError): String = when (error) {
    PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
        "Video materi tidak ditemukan. Periksa videoId di VideoLessonCatalog."

    PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
        "Pemilik video melarang pemutaran di luar YouTube, jadi materi ini " +
            "tidak bisa diputar di dalam aplikasi."

    PlayerConstants.PlayerError.REQUEST_MISSING_HTTP_REFERER ->
        "YouTube menolak permintaan karena identitas aplikasi tidak terkirim."

    PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST ->
        "Parameter permintaan ke pemutar YouTube tidak valid."

    PlayerConstants.PlayerError.HTML_5_PLAYER ->
        "Pemutar YouTube gagal dimuat di perangkat ini."

    PlayerConstants.PlayerError.UNKNOWN ->
        "Materi gagal diputar. Periksa koneksi internet, lalu coba lagi."
}

/** Format detik menjadi `m:ss`, atau `h:mm:ss` untuk materi panjang. */
private fun formatSeconds(total: Int): String {
    val safe = total.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
