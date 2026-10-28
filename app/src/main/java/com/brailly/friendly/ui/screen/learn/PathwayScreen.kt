package com.brailly.friendly.ui.screen.learn

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsDim
import com.brailly.friendly.ui.designsystem.DsHudChip
import com.brailly.friendly.ui.designsystem.DsNodeState
import com.brailly.friendly.ui.designsystem.DsProgressBar
import com.brailly.friendly.ui.designsystem.DsSlabNode
import com.brailly.friendly.ui.designsystem.dsAnimationsEnabled
import com.brailly.friendly.ui.designsystem.readableContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * Jalur kuis: deretan node slab yang berkelok naik–turun secara vertikal. Ada
 * dua jenis node yang berselang-seling, yaitu materi video pembuka unit dan
 * level kuis unit tersebut. Setiap node menampilkan statusnya (terkunci,
 * tersedia, berjalan, atau selesai).
 *
 * Node yang sedang berjalan dibuat lebih besar dan dikelilingi cincin
 * berdenyut, sehingga titik lanjut pengguna terlihat tanpa harus membaca label
 * satu per satu.
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

    val done = state.levels.count { it.completed }
    // Langkah berjalan: yang pertama terbuka tetapi belum selesai.
    val currentIndex = state.steps.indexOfFirst { it.unlocked && !it.completed }
    val animate = dsAnimationsEnabled()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsDim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: tombol kembali + judul + HUD trofi.
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
                    style = Ds.text.sectionHeader,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "$done dari ${state.levels.size} level tuntas",
                    style = Ds.text.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DsHudChip(
                icon = BraillyIcons.Trophy,
                tint = Ds.colors.reward,
                value = "$done",
                contentDescription = "$done level tuntas",
            )
        }

        DsProgressBar(
            progress = if (state.levels.isEmpty()) 0f else done.toFloat() / state.levels.size,
        )

        Spacer(Modifier.height(8.dp))

        // Node materi dan node level, dengan pergeseran horizontal berpola zig-zag.
        state.steps.forEachIndexed { i, step ->
            val nodeState = when {
                !step.unlocked -> DsNodeState.Locked
                step.completed -> DsNodeState.Completed
                i == currentIndex -> DsNodeState.Current
                else -> DsNodeState.Available
            }

            when (step) {
                is PathStep.Video -> VideoPathNode(
                    step = step,
                    nodeState = nodeState,
                    position = i % 4,
                    animate = animate,
                    onClick = { viewModel.openVideo(step.lesson.index) },
                )

                is PathStep.Quiz -> QuizPathNode(
                    level = step.level,
                    nodeState = nodeState,
                    position = i % 4,
                    animate = animate,
                    onClick = { viewModel.startLevel(step.level.index) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Pergeseran horizontal node agar jalur tampak berkelok: tengah → kanan →
 * tengah → kiri, lalu berulang.
 */
private fun shiftFor(position: Int): Dp = when (position) {
    0 -> 0.dp
    1 -> 60.dp
    2 -> 0.dp
    else -> (-60).dp
}

/** Ukuran node: yang sedang berjalan dibuat lebih besar agar menonjol. */
private fun nodeSize(state: DsNodeState): Dp =
    if (state == DsNodeState.Current) 96.dp else 80.dp

/** Node materi video. Dibedakan dari node level lewat bentuk dan ikonnya. */
@Composable
private fun VideoPathNode(
    step: PathStep.Video,
    nodeState: DsNodeState,
    position: Int,
    animate: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val face = when (nodeState) {
        DsNodeState.Completed -> cs.secondary
        else -> cs.tertiary
    }
    val glyphTint = if (nodeState == DsNodeState.Locked) {
        cs.onSurfaceVariant
    } else {
        face.readableContent()
    }
    val description = when {
        step.completed -> "Materi ${step.lesson.title} sudah tuntas ditonton"
        step.unlocked -> "Tonton materi ${step.lesson.title}"
        else -> "Materi ${step.lesson.title} terkunci"
    }
    val size = nodeSize(nodeState)

    PathNodeLayout(
        position = position,
        title = step.lesson.title,
        titleHighlighted = step.unlocked,
        caption = if (step.completed) "Tuntas" else "Tonton dulu",
        description = description,
    ) {
        DsSlabNode(
            state = nodeState,
            // Bentuk persegi membulat, bukan bulat penuh, supaya node materi
            // langsung bisa dibedakan dari node level saat jalur di-scroll.
            shape = RoundedCornerShape(size / 4),
            face = face,
            onClick = onClick,
            size = size,
            animate = animate,
        ) {
            Icon(
                if (step.unlocked) BraillyIcons.Play else BraillyIcons.Lock,
                contentDescription = null,
                tint = glyphTint,
                modifier = Modifier.size(size * 0.38f),
            )
        }
    }
}

/** Node level kuis. */
@Composable
private fun QuizPathNode(
    level: LevelStatus,
    nodeState: DsNodeState,
    position: Int,
    animate: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val face = when (nodeState) {
        DsNodeState.Completed -> Ds.colors.reward
        else -> cs.primary
    }
    val glyphTint = if (nodeState == DsNodeState.Locked) {
        cs.onSurfaceVariant
    } else {
        face.readableContent()
    }
    val description = when {
        level.completed -> "Level ${level.number} tuntas, skor ${level.bestScore}"
        level.blockedByVideo -> "Level ${level.number} terkunci, tonton materi videonya dulu"
        !level.unlocked -> "Level ${level.number} terkunci"
        else -> "Mulai level ${level.number}"
    }
    val size = nodeSize(nodeState)

    PathNodeLayout(
        position = position,
        title = level.title,
        titleHighlighted = level.unlocked,
        caption = when {
            level.completed -> "${level.bestScore}/${LearnViewModel.QUESTIONS_PER_LEVEL}"
            level.blockedByVideo -> "Tonton materi dulu"
            else -> null
        },
        description = description,
    ) {
        DsSlabNode(
            state = nodeState,
            shape = CircleShape,
            face = face,
            onClick = onClick,
            size = size,
            animate = animate,
        ) {
            NodeGlyph(level = level, tint = glyphTint, size = size)
        }
    }
}

@Composable
private fun NodeGlyph(level: LevelStatus, tint: Color, size: Dp) {
    when {
        !level.unlocked -> Icon(
            BraillyIcons.Lock,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.38f),
        )

        level.completed -> Icon(
            BraillyIcons.Check,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.44f),
        )

        else -> Text(
            level.number.toString(),
            style = Ds.text.question,
            color = tint,
        )
    }
}

/**
 * Tata letak bersama kedua jenis node: tombol, judul, lalu keterangan.
 *
 * Seluruh kelompok diberi satu keterangan semantik, bukan satu per elemen,
 * supaya pembaca layar menyebut "Level 3 selesai" sekali alih-alih membacakan
 * ikon, judul, dan caption sebagai tiga perhentian terpisah.
 */
@Composable
private fun PathNodeLayout(
    position: Int,
    title: String,
    titleHighlighted: Boolean,
    caption: String?,
    description: String,
    node: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .offset(x = shiftFor(position))
                .padding(vertical = 10.dp)
                // mergeDescendants, bukan clearAndSetSemantics: aksi ketuk milik
                // node harus tetap ikut terangkat, kalau tidak TalkBack bisa
                // membacakan judulnya tetapi tidak bisa membukanya.
                .semantics(mergeDescendants = true) { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            node()
            Text(
                title,
                style = Ds.text.skillLabel,
                color = if (titleHighlighted) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.width(140.dp),
            )
            if (caption != null) {
                Text(
                    caption,
                    style = Ds.text.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(140.dp),
                )
            }
        }
    }
}
