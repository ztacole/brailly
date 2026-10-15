package com.brailly.friendly.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brailly.friendly.core.braille.BrailleToken

/**
 * Menampilkan hasil terjemahan (list token) sebagai deretan sel braille yang
 * membungkus otomatis. Spasi ditampilkan sebagai jeda kosong.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrailleTextView(
    tokens: List<BrailleToken>,
    modifier: Modifier = Modifier,
) {
    if (tokens.isEmpty()) {
        Text(
            "Hasil braille akan muncul di sini",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tokens.forEach { token ->
            when (token) {
                is BrailleToken.Cell -> BrailleCellView(
                    cell = token.cell,
                    contentDescription = "Braille untuk ${token.sourceChar}",
                )
                BrailleToken.Space -> androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
            }
        }
    }
}
