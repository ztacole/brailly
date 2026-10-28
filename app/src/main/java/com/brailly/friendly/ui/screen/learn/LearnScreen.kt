package com.brailly.friendly.ui.screen.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.ui.component.BrailleCellView
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsDim
import com.brailly.friendly.ui.designsystem.DsTextField
import org.koin.compose.viewmodel.koinViewModel

/**
 * Menu Belajar: menampilkan Kamus Braille lebih dulu, dengan satu tombol di
 * bagian atas untuk membuka Jalur Kuis (pathway) di layar terpisah.
 */
@Composable
fun LearnScreen(
    onOpenQuiz: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = DsDim.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Belajar",
                    style = Ds.text.screenTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // Tombol menuju jalur kuis (pathway) di layar terpisah.
                QuizEntryCard(state = state, onOpenQuiz = onOpenQuiz)

                Text(
                    "Kamus Braille",
                    style = Ds.text.sectionHeader,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // Tab pemilih kamus: Alfabet / Angka / Simbol.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DictionaryTab.entries.forEach { tab ->
                        FilterChip(
                            selected = state.tab == tab,
                            onClick = { viewModel.onTabChange(tab) },
                            // Gaya label disebut eksplisit: `labelLarge` kini
                            // 17sp Black, dan tiga chip seukuran itu tidak
                            // muat sebaris pada layar sempit.
                            label = { Text(tab.label, style = Ds.text.tile) },
                        )
                    }
                }

                if (state.tab == DictionaryTab.ALPHABET) {
                    DsTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = "Cari huruf",
                        placeholder = "Ketik huruf, misalnya A",
                        singleLine = true,
                    )
                }
            }
        }

        when (state.tab) {
            DictionaryTab.ALPHABET -> items(state.alphabet) { entry ->
                DictionaryCell(symbol = entry.letter.uppercase(), cell = entry.cell)
            }

            DictionaryTab.NUMBER -> items(state.numbers) { entry ->
                DictionaryCell(symbol = entry.symbol, cell = entry.cell, caption = entry.description)
            }

            DictionaryTab.SYMBOL -> items(state.symbols) { entry ->
                DictionaryCell(symbol = entry.symbol, cell = entry.cell, caption = entry.description)
            }
        }
    }
}

@Composable
private fun QuizEntryCard(state: LearnUiState, onOpenQuiz: () -> Unit) {
    val done = state.levels.count { it.completed }
    val total = state.levels.size
    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    BraillyIcons.Trophy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Jalur Kuis",
                        style = Ds.text.skillLabel,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        if (total > 0) "$done dari $total level tuntas"
                        else "Mulai belajar braille lewat kuis bertingkat",
                        style = Ds.text.helper,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            PrimaryButton(
                text = if (done > 0) "Lanjutkan Kuis" else "Mulai Kuis",
                onClick = onOpenQuiz,
                leadingIcon = BraillyIcons.Play,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DictionaryCell(
    symbol: String,
    cell: com.brailly.friendly.core.braille.BrailleCell,
    caption: String? = null,
) {
    BraillyCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BrailleCellView(cell = cell, dotSize = 9.dp, gap = 3.dp)
            Text(
                symbol,
                style = Ds.text.skillLabel,
                color = MaterialTheme.colorScheme.primary,
            )
            if (caption != null) {
                Text(
                    caption,
                    style = Ds.text.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
