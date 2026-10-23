package com.brailly.friendly.ui.screen.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleToken
import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.ui.component.BrailleKeypad
import com.brailly.friendly.ui.component.BrailleTextView
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.component.SecondaryButton
import com.brailly.friendly.ui.theme.ErrorRed
import com.brailly.friendly.ui.theme.SuccessGreen

@Composable
fun QuizScreen(
    session: QuizSession,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header: tombol keluar + judul level + progress.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconActionButton(
                icon = BraillyIcons.Back,
                contentDescription = "Keluar kuis",
                onClick = onExit,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Level ${session.levelNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Skor ${session.score}/${session.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (session.finished) {
            ResultCard(session = session, onExit = onExit)
            return@Column
        }

        val progress = (session.current + if (session.lastCorrect != null) 1 else 0).toFloat() /
            session.total.toFloat()
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
        Text(
            "Soal ${session.current + 1} dari ${session.total}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        QuestionCard(session = session, onAnswer = onAnswer, onNext = onNext)
    }
}

@Composable
private fun QuestionCard(
    session: QuizSession,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
) {
    val q = session.question
    val answered = session.lastCorrect != null

    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Instruksi sesuai arah soal.
            val instruction = when {
                q.direction == QuizDirection.BRAILLE_TO_TEXT && q.format == QuizFormat.MULTIPLE_CHOICE ->
                    "Teks apa yang ditunjukkan braille ini?"
                q.direction == QuizDirection.BRAILLE_TO_TEXT ->
                    "Tulis teks dari braille berikut"
                q.format == QuizFormat.MULTIPLE_CHOICE ->
                    "Braille mana yang benar untuk teks ini?"
                else ->
                    "Susun braille untuk teks berikut memakai papan ketik"
            }
            Text(
                instruction,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            // Prompt: tampilkan braille (B->T) atau teks (T->B).
            PromptArea(q)

            if (!answered) {
                when (q.format) {
                    QuizFormat.MULTIPLE_CHOICE -> MultipleChoice(q, onAnswer)
                    QuizFormat.FILL_BLANK ->
                        // Soal teks -> braille memakai papan ketik braille
                        // (Perkins), bukan kotak teks biasa.
                        if (q.direction == QuizDirection.TEXT_TO_BRAILLE) {
                            BrailleInput(onAnswer)
                        } else {
                            FillBlank(onAnswer)
                        }
                }
            } else {
                FeedbackArea(session, onNext)
            }
        }
    }
}

@Composable
private fun PromptArea(q: QuizQuestion) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (q.isBrailleShown) {
            // Soal braille -> teks: tampilkan sel braille jawaban.
            BrailleTextView(tokens = q.tokens)
        } else {
            // Soal teks -> braille: tampilkan teksnya.
            Text(
                q.answerText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MultipleChoice(q: QuizQuestion, onAnswer: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        q.options.forEach { option ->
            // Untuk soal T->B, pilihan ditampilkan sebagai braille; selainnya teks.
            if (q.direction == QuizDirection.TEXT_TO_BRAILLE) {
                BrailleOption(option = option, onClick = { onAnswer(option) })
            } else {
                PrimaryButton(
                    text = option,
                    onClick = { onAnswer(option) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BrailleOption(option: String, onClick: () -> Unit) {
    val translator = remember { com.brailly.friendly.core.braille.BrailleTranslator() }
    val tokens = remember(option) { translator.textToBraille(option) }
    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BrailleTextView(tokens = tokens, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FillBlank(onAnswer: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ketik jawaban") },
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
        )
        PrimaryButton(
            text = "Periksa",
            onClick = { onAnswer(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Masukan braille untuk soal teks -> braille. Pengguna menyusun jawaban sel demi
 * sel memakai papan ketik Perkins ([BrailleKeypad]): titik di-toggle untuk sel
 * yang sedang dibentuk, lalu "Tambah Sel" memindahkannya ke rangkaian jawaban.
 * Tersedia pula tombol spasi (antar kata) dan hapus. Saat diperiksa, rangkaian
 * sel diterjemahkan kembali menjadi teks lalu dibandingkan dengan jawaban benar.
 */
@Composable
private fun BrailleInput(onAnswer: (String) -> Unit) {
    val translator = remember { BrailleTranslator() }
    // Sel yang sudah dikomit; null mewakili spasi antar kata.
    val committed = remember { mutableStateListOf<BrailleCell?>() }
    // Titik aktif pada sel yang sedang disusun.
    var active by remember { mutableStateOf(setOf<Int>()) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Pratinjau rangkaian braille yang sudah dibentuk + sel berjalan.
        val previewTokens = buildList {
            committed.forEach { cell ->
                if (cell == null) add(BrailleToken.Space)
                else add(BrailleToken.Cell(cell, '?'))
            }
            if (active.isNotEmpty()) add(BrailleToken.Cell(BrailleCell(active), '?'))
        }
        BraillyCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (previewTokens.isEmpty()) {
                    Text(
                        "Susun jawaban braille di sini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    BrailleTextView(tokens = previewTokens)
                }
            }
        }

        // Papan ketik 6 titik.
        BrailleKeypad(
            active = active,
            onToggle = { dot ->
                active = if (active.contains(dot)) active - dot else active + dot
            },
        )

        // Aksi menyusun sel.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = "Tambah Sel",
                onClick = {
                    if (active.isNotEmpty()) {
                        committed.add(BrailleCell(active))
                        active = emptySet()
                    }
                },
                enabled = active.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "Spasi",
                onClick = {
                    // Komit sel berjalan dulu bila ada, lalu sisipkan spasi.
                    if (active.isNotEmpty()) {
                        committed.add(BrailleCell(active))
                        active = emptySet()
                    }
                    if (committed.isNotEmpty() && committed.last() != null) {
                        committed.add(null)
                    }
                },
                enabled = active.isNotEmpty() || (committed.isNotEmpty() && committed.last() != null),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = "Hapus",
                onClick = {
                    when {
                        active.isNotEmpty() -> active = emptySet()
                        committed.isNotEmpty() -> committed.removeAt(committed.lastIndex)
                    }
                },
                enabled = active.isNotEmpty() || committed.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "Periksa",
                onClick = {
                    // Komit sel berjalan bila belum, lalu terjemahkan ke teks.
                    val cells = committed.toMutableList()
                    if (active.isNotEmpty()) cells.add(BrailleCell(active))
                    onAnswer(translator.brailleToText(cells))
                },
                enabled = active.isNotEmpty() || committed.any { it != null },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FeedbackArea(session: QuizSession, onNext: () -> Unit) {
    val correct = session.lastCorrect == true
    val q = session.question
    // Untuk soal teks -> braille, jawaban benar ditampilkan sebagai sel braille,
    // bukan huruf biasa, agar sesuai dengan apa yang diminta pada soal.
    val showBrailleAnswer = !correct && q.direction == QuizDirection.TEXT_TO_BRAILLE
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (correct) BraillyIcons.Check else BraillyIcons.Close,
                contentDescription = null,
                tint = if (correct) SuccessGreen else ErrorRed,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                when {
                    correct -> "Benar!"
                    showBrailleAnswer -> "Kurang tepat. Jawaban yang benar:"
                    else -> "Kurang tepat. Jawaban: ${q.answerText}"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (showBrailleAnswer) {
            BraillyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrailleTextView(tokens = q.tokens, modifier = Modifier.fillMaxWidth())
                    Text(
                        "(${q.answerText})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val isLast = session.current + 1 >= session.total
        PrimaryButton(
            text = if (isLast) "Lihat Hasil" else "Soal Berikutnya",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResultCard(session: QuizSession, onExit: () -> Unit) {
    val passed = session.score >= LearnViewModel.PASS_THRESHOLD
    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                BraillyIcons.Trophy,
                contentDescription = null,
                tint = if (passed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
            Text(
                if (passed) "Level Selesai!" else "Coba Lagi",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Skor kamu ${session.score} dari ${session.total}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                if (passed) "Level berikutnya terbuka."
                else "Butuh minimal ${LearnViewModel.PASS_THRESHOLD} benar untuk membuka level berikutnya.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                text = "Kembali ke Jalur",
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
