package com.brailly.friendly.ui.screen.learn

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import org.koin.compose.viewmodel.koinViewModel

/**
 * Jalur kuis bergaya Duolingo: deretan node yang berkelok naik–turun (zig-zag)
 * secara vertikal. Ada dua jenis node yang berselang-seling, yaitu materi video
 * pembuka unit dan level kuis unit tersebut. Setiap node menampilkan statusnya
 * (terkunci, tersedia, atau selesai).
 *
 * Saat sebuah node dibuka, layar berganti ke [VideoLessonScreen] atau
 * [QuizScreen], mengikuti pola yang sama seperti sesi kuis.
 */
@Composable
fun PathwayScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Saat sedang bermain level, tampilkan layar kuis.
    state.session?.let { session ->
        QuizScreen(
            session = session,
            onAnswer = viewModel::answer,
            onNext = viewModel::nextQuestion,
            onExit = viewModel::exitQuiz,
            modifier = modifier,
        )
        return
    }

    // Saat sedang menonton materi, tampilkan layar video.
    state.video?.let { video ->
        // Tombol kembali sistem menutup materi lebih dulu (sekaligus menyimpan
        // progres tontonan), bukan langsung keluar dari jalur kuis.
        BackHandler { viewModel.closeVideo() }
        VideoLessonScreen(
            video = video,
            onBack = viewModel::closeVideo,
            onContinue = viewModel::continueFromVideo,
            onDuration = viewModel::onVideoDuration,
            onProgress = viewModel::onVideoProgress,
            onEnded = viewModel::onVideoEnded,
            onForceComplete = viewModel::forceCompleteVideo,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header: tombol kembali + judul + penghitung progres.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconActionButton(
                icon = BraillyIcons.Back,
                contentDescription = "Kembali ke kamus",
                onClick = onBack,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Jalur Kuis",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                val done = state.levels.count { it.completed }
                Text(
                    "$done dari ${state.levels.size} level selesai",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    BraillyIcons.Trophy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "${state.levels.count { it.completed }}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Node materi dan node level, dengan pergeseran horizontal berpola zig-zag.
        state.steps.forEachIndexed { i, step ->
            when (step) {
                is PathStep.Video -> VideoPathNode(
                    step = step,
                    position = i % 4,
                    onClick = { viewModel.openVideo(step.lesson.index) },
                )

                is PathStep.Quiz -> QuizPathNode(
                    level = step.level,
                    position = i % 4,
                    onClick = { viewModel.startLevel(step.level.index) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Pergeseran horizontal node agar jalur tampak berkelok seperti pathway
 * Duolingo: tengah → kanan → tengah → kiri, lalu berulang.
 */
private fun shiftFor(position: Int) = when (position) {
    0 -> 0.dp
    1 -> 64.dp
    2 -> 0.dp
    else -> (-64).dp
}

/** Node materi video. Dibedakan dari node level lewat bentuk dan ikonnya. */
@Composable
private fun VideoPathNode(
    step: PathStep.Video,
    position: Int,
    onClick: () -> Unit,
) {
    PathNodeLayout(
        position = position,
        title = step.lesson.title,
        titleHighlighted = step.unlocked,
        caption = if (step.completed) "Materi selesai" else "Tonton dulu",
    ) {
        val container = when {
            step.completed -> MaterialTheme.colorScheme.secondary
            step.unlocked -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        val contentColor = when {
            step.completed -> MaterialTheme.colorScheme.onSecondary
            step.unlocked -> MaterialTheme.colorScheme.onTertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        // Bentuk persegi membulat, bukan bulat penuh, supaya node materi
        // langsung bisa dibedakan dari node level saat jalur di-scroll.
        val shape = RoundedCornerShape(20.dp)
        val icon = if (step.unlocked) BraillyIcons.Play else BraillyIcons.Lock
        val description = when {
            step.completed -> "Materi ${step.lesson.title} selesai ditonton"
            step.unlocked -> "Tonton materi ${step.lesson.title}"
            else -> "Materi ${step.lesson.title} terkunci"
        }

        NodeSurface(
            enabled = step.unlocked,
            shape = shape,
            container = container,
            onClick = onClick,
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = contentColor,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

/** Node level kuis. */
@Composable
private fun QuizPathNode(
    level: LevelStatus,
    position: Int,
    onClick: () -> Unit,
) {
    PathNodeLayout(
        position = position,
        title = level.title,
        titleHighlighted = level.unlocked,
        caption = when {
            level.completed -> "${level.bestScore}/${LearnViewModel.QUESTIONS_PER_LEVEL}"
            level.blockedByVideo -> "Tonton materi dulu"
            else -> null
        },
    ) {
        val container = when {
            level.completed -> MaterialTheme.colorScheme.secondary
            level.unlocked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        val contentColor = when {
            level.completed -> MaterialTheme.colorScheme.onSecondary
            level.unlocked -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        NodeSurface(
            enabled = level.unlocked,
            shape = CircleShape,
            container = container,
            onClick = onClick,
        ) {
            when {
                !level.unlocked -> Icon(
                    BraillyIcons.Lock,
                    contentDescription = if (level.blockedByVideo)
                        "Level ${level.number} terkunci, tonton materi videonya dulu"
                    else "Level ${level.number} terkunci",
                    tint = contentColor,
                    modifier = Modifier.size(30.dp),
                )

                level.completed -> Icon(
                    BraillyIcons.Check,
                    contentDescription = "Level ${level.number} selesai",
                    tint = contentColor,
                    modifier = Modifier.size(34.dp),
                )

                else -> Text(
                    level.number.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
        }
    }
}

/** Tata letak bersama kedua jenis node: tombol, judul, lalu keterangan. */
@Composable
private fun PathNodeLayout(
    position: Int,
    title: String,
    titleHighlighted: Boolean,
    caption: String?,
    node: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .offset(x = shiftFor(position))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            node()
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = if (titleHighlighted) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp),
            )
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}

/**
 * Lingkaran/persegi node berukuran 72dp. Node terkunci digambar tanpa
 * [Surface] yang bisa diklik agar tidak ikut menerima fokus sentuh.
 */
@Composable
private fun NodeSurface(
    enabled: Boolean,
    shape: Shape,
    container: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val nodeModifier = Modifier.size(72.dp)
    if (enabled) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = container,
            modifier = nodeModifier,
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    } else {
        Box(
            modifier = nodeModifier
                .clip(shape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
