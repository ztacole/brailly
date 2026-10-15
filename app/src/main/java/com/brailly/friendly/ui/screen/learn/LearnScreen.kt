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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.ui.component.BrailleCellView
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.theme.SuccessGreen
import com.brailly.friendly.ui.theme.ErrorRed
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Belajar",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // Kuis harian
                QuizCard(state, viewModel)

                Text(
                    "Panduan Alfabet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cari huruf") },
                    leadingIcon = { Icon(BraillyIcons.Search, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )
            }
        }

        items(state.alphabet) { entry ->
            BraillyCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BrailleCellView(cell = entry.cell, dotSize = 9.dp, gap = 3.dp)
                    Text(
                        entry.letter.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizCard(state: LearnUiState, viewModel: LearnViewModel) {
    val quiz = state.quiz ?: return
    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Kuis: Huruf apa ini?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        BraillyIcons.Trophy,
                        contentDescription = "Skor",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "${state.score}/${state.answered}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BrailleCellView(cell = quiz.cell, dotSize = 18.dp, gap = 8.dp)
            }

            if (state.lastCorrect == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    quiz.options.forEach { option ->
                        PrimaryButton(
                            text = option.uppercase(),
                            onClick = { viewModel.answer(option) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                val correct = state.lastCorrect == true
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (correct) BraillyIcons.Check else BraillyIcons.Close,
                        contentDescription = null,
                        tint = if (correct) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (correct) "Benar! Itu huruf ${quiz.correct.uppercase()}"
                        else "Kurang tepat. Jawaban: ${quiz.correct.uppercase()}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                PrimaryButton(
                    text = "Soal Berikutnya",
                    onClick = viewModel::nextQuestion,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
