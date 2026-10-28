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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleToken
import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.ui.component.BrailleKeypad
import com.brailly.friendly.ui.component.BrailleTextView
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.IconActionButton
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.component.SecondaryButton
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsAnswerTile
import com.brailly.friendly.ui.designsystem.DsButton
import com.brailly.friendly.ui.designsystem.DsButtonVariant
import com.brailly.friendly.ui.designsystem.DsConfetti
import com.brailly.friendly.ui.designsystem.DsDim
import com.brailly.friendly.ui.designsystem.DsFeedbackSheet
import com.brailly.friendly.ui.designsystem.DsHudChip
import com.brailly.friendly.ui.designsystem.DsProgressBar
import com.brailly.friendly.ui.designsystem.DsSlabSurface
import com.brailly.friendly.ui.designsystem.DsSound
import com.brailly.friendly.ui.designsystem.DsTextField
import com.brailly.friendly.ui.designsystem.DsTileState
import com.brailly.friendly.ui.designsystem.LocalDsSounds
import com.brailly.friendly.ui.designsystem.dsAnimationsEnabled
import com.brailly.friendly.ui.designsystem.dsTileTextColor

/**
 * Satu soal kuis.
 *
 * Tata letaknya mengikuti pola layar pelajaran: tombol tutup dan bilah progres
 * di atas, soal pada kanvas rata, pilihan jawaban sebagai tile slab, lalu
 * lembar umpan balik di bawah. Benar dan salah tidak pernah ditandai warna
 * saja: tile yang salah bergetar, dan lembar umpan balik membawa glyph.
 */
@Composable
fun QuizScreen(
    session: QuizSession,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (session.finished) {
        QuizResult(session = session, onExit = onExit, modifier = modifier)
        return
    }

    val q = session.question
    val answered = session.lastCorrect != null
    val progress = (session.current + if (answered) 1 else 0).toFloat() / session.total.toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsDim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // HUD pelajaran: tutup di kiri, progres di tengah, skor di kanan.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconActionButton(
                icon = BraillyIcons.Close,
                contentDescription = "Keluar kuis",
                onClick = onExit,
            )
            DsProgressBar(progress = progress, modifier = Modifier.weight(1f))
            DsHudChip(
                icon = BraillyIcons.Trophy,
                tint = Ds.colors.reward,
                value = "${session.score}",
                contentDescription = "Skor ${session.score} dari ${session.total}",
            )
        }

        Text(
            "Level ${session.levelNumber} \u00b7 Soal ${session.current + 1} dari ${session.total}",
            style = Ds.text.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            instructionFor(q),
            style = Ds.text.question,
            color = MaterialTheme.colorScheme.onBackground,
        )

        PromptArea(q)

        when (q.format) {
            QuizFormat.MULTIPLE_CHOICE -> MultipleChoice(session, onAnswer)
            QuizFormat.FILL_BLANK ->
                if (!answered) {
                    // Soal teks -> braille memakai papan ketik braille
                    // (Perkins), bukan kotak teks biasa.
                    if (q.direction == QuizDirection.TEXT_TO_BRAILLE) {
                        BrailleInput(onAnswer)
                    } else {
                        FillBlank(onAnswer)
                    }
                }
        }

        if (answered) {
            FeedbackSheet(session = session, onNext = onNext)
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun instructionFor(q: QuizQuestion): String = when {
    q.direction == QuizDirection.BRAILLE_TO_TEXT && q.format == QuizFormat.MULTIPLE_CHOICE ->
        "Teks apa yang ditunjukkan braille ini?"
    q.direction == QuizDirection.BRAILLE_TO_TEXT ->
        "Tulis teks dari braille berikut"
    q.format == QuizFormat.MULTIPLE_CHOICE ->
        "Braille mana yang benar untuk teks ini?"
    else ->
        "Susun braille untuk teks berikut memakai papan ketik"
}

/** Panel soal: sel braille (B->T) atau teksnya (T->B). */
@Composable
private fun PromptArea(q: QuizQuestion) {
    DsSlabSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (q.isBrailleShown) {
                BrailleTextView(tokens = q.tokens)
            } else {
                Text(
                    q.answerText,
                    style = Ds.text.display,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Pilihan ganda sebagai tile slab.
 *
 * Pilihan tetap terlihat setelah dijawab, dengan tile yang benar ditandai hijau
 * dan tile yang dipilih keliru ditandai merah sekaligus bergetar. Menyembunyikan
 * pilihannya akan menghilangkan kesempatan pengguna membandingkan jawabannya
 * dengan yang benar.
 */
@Composable
private fun MultipleChoice(session: QuizSession, onAnswer: (String) -> Unit) {
    val q = session.question
    val answered = session.lastCorrect != null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        q.options.forEach { option ->
            val state = when {
                !answered -> DsTileState.Idle
                option == q.answerText -> DsTileState.Correct
                option == session.lastAnswer -> DsTileState.Wrong
                else -> DsTileState.Idle
            }
            val label = when (state) {
                DsTileState.Correct -> "Pilihan $option, jawaban benar"
                DsTileState.Wrong -> "Pilihan $option, jawaban kamu keliru"
                else -> "Pilihan $option"
            }

            DsAnswerTile(
                state = state,
                onTap = { if (!answered) onAnswer(option) },
                modifier = Modifier.fillMaxWidth(),
                contentDescription = label,
            ) {
                // Soal teks -> braille: pilihannya ditampilkan sebagai sel
                // braille, sesuai dengan apa yang diminta soal.
                if (q.direction == QuizDirection.TEXT_TO_BRAILLE) {
                    val translator = remember { BrailleTranslator() }
                    val tokens = remember(option) { translator.textToBraille(option) }
                    BrailleTextView(tokens = tokens, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(option, style = Ds.text.tile, color = dsTileTextColor(state))
                }
            }
        }
    }
}

@Composable
private fun FillBlank(onAnswer: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DsTextField(
            value = text,
            onValueChange = { text = it },
            label = "Ketik jawaban",
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
        DsSlabSurface(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (previewTokens.isEmpty()) {
                    Text(
                        "Susun jawaban braille di sini",
                        style = Ds.text.helper,
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
private fun FeedbackSheet(session: QuizSession, onNext: () -> Unit) {
    val correct = session.lastCorrect == true
    val q = session.question
    // Untuk soal teks -> braille, jawaban benar ditampilkan sebagai sel braille,
    // bukan huruf biasa, agar sesuai dengan apa yang diminta pada soal.
    val showBrailleAnswer = !correct && q.direction == QuizDirection.TEXT_TO_BRAILLE
    val isLast = session.current + 1 >= session.total

    DsFeedbackSheet(
        correct = correct,
        title = if (correct) "Tepat sekali!" else "Belum tepat",
        glyph = if (correct) BraillyIcons.Check else BraillyIcons.Close,
        modifier = Modifier.fillMaxWidth(),
        detail = if (correct) {
            null
        } else {
            {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (showBrailleAnswer) {
                            "Yang benar seperti ini:"
                        } else {
                            "Jawabannya: ${q.answerText}"
                        },
                        style = Ds.text.body,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (showBrailleAnswer) {
                        DsSlabSurface(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BrailleTextView(
                                    tokens = q.tokens,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    "(${q.answerText})",
                                    style = Ds.text.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        DsButton(
            title = if (isLast) "Lihat Hasil" else "Lanjut",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            variant = if (correct) DsButtonVariant.Primary else DsButtonVariant.Destructive,
        )
    }
}

/**
 * Layar hasil level.
 *
 * Saat lulus, kanvasnya berganti menjadi warna primer dengan konfeti, mengikuti
 * pola perayaan akhir pelajaran. Saat belum lulus, kanvasnya tetap netral:
 * perayaan untuk hasil yang gagal akan terasa seperti olok-olok.
 */
@Composable
private fun QuizResult(
    session: QuizSession,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val passed = session.score >= LearnViewModel.PASS_THRESHOLD
    val animate = dsAnimationsEnabled()
    val sounds = LocalDsSounds.current

    // Dibunyikan sekali saat layar hasil muncul, bukan pada setiap penyusunan
    // ulang. Yang gagal tidak diberi nada: bunyi perayaan untuk hasil yang
    // belum lulus akan terasa seperti olok-olok.
    LaunchedEffect(passed) {
        if (passed) sounds.play(DsSound.Complete)
    }

    val canvas = if (passed) cs.primary else cs.background
    val onCanvas = if (passed) cs.onPrimary else cs.onBackground

    Box(modifier.fillMaxSize().background(canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DsDim.screenPadding)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DsDim.sectionGap),
        ) {
            Icon(
                BraillyIcons.Trophy,
                contentDescription = null,
                tint = if (passed) c.reward else cs.onSurfaceVariant,
                modifier = Modifier.size(96.dp),
            )
            Text(
                if (passed) "Level Tuntas!" else "Hampir!",
                style = Ds.text.screenTitle,
                color = onCanvas,
                textAlign = TextAlign.Center,
            )
            Text(
                "${session.score}/${session.total}",
                style = Ds.text.heroNumber,
                color = onCanvas,
            )
            Text(
                if (passed) {
                    "Level berikutnya sudah terbuka. Lanjutkan selagi hangat."
                } else {
                    "Kumpulkan minimal ${LearnViewModel.PASS_THRESHOLD} jawaban benar " +
                        "untuk membuka level berikutnya. Coba sekali lagi."
                },
                style = Ds.text.body,
                color = onCanvas,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            DsButton(
                title = "Kembali ke Jalur",
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(),
                // Di atas kanvas primer, tombol primer akan lesap ke latarnya.
                variant = if (passed) DsButtonVariant.Neutral else DsButtonVariant.Primary,
            )
        }
        if (passed && animate) DsConfetti()
    }
}
