package com.brailly.friendly.ui.screen.learn

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import org.koin.compose.viewmodel.koinViewModel

/**
 * Jalur kuis bergaya Duolingo: deretan node level yang berkelok naik–turun
 * (zig-zag) secara vertikal. Setiap node menampilkan status (terkunci,
 * tersedia, atau selesai). Saat sebuah level dimainkan, layar berganti ke
 * [QuizScreen].
 */
@Composable
fun PathwayScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Saat sedang bermain level, tampilkan layar kuis.
    if (state.session != null) {
        QuizScreen(
            session = state.session!!,
            onAnswer = viewModel::answer,
            onNext = viewModel::nextQuestion,
            onExit = viewModel::exitQuiz,
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

        // Node-node level dengan pergeseran horizontal berpola zig-zag.
        state.levels.forEachIndexed { i, level ->
            PathNode(
                level = level,
                position = i % 4,
                onClick = { viewModel.startLevel(level.index) },
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Satu node pada jalur. [position] (0..3) menentukan pergeseran horizontal agar
 * jalur tampak berkelok seperti pathway Duolingo.
 */
@Composable
private fun PathNode(
    level: LevelStatus,
    position: Int,
    onClick: () -> Unit,
) {
    // Pergeseran: tengah → kanan → tengah → kiri, lalu berulang.
    val shift = when (position) {
        0 -> 0.dp
        1 -> 64.dp
        2 -> 0.dp
        else -> (-64).dp
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .offset(x = shift)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LevelNodeButton(level = level, onClick = onClick)
            Text(
                level.title,
                style = MaterialTheme.typography.labelMedium,
                color = if (level.unlocked) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp),
            )
            if (level.completed) {
                Text(
                    "${level.bestScore}/${LearnViewModel.QUESTIONS_PER_LEVEL}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LevelNodeButton(level: LevelStatus, onClick: () -> Unit) {
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

    val nodeModifier = Modifier.size(72.dp)
    val inner: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            when {
                !level.unlocked -> Icon(
                    BraillyIcons.Lock,
                    contentDescription = "Level ${level.number} terkunci",
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

    if (level.unlocked) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = container,
            modifier = nodeModifier,
        ) { inner() }
    } else {
        Box(
            modifier = nodeModifier
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) { inner() }
    }
}
