package com.brailly.friendly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Papan ketik Braille virtual bergaya Perkins Brailler: 6 titik yang bisa
 * ditekan (toggle). [active] berisi nomor titik yang sedang aktif.
 */
@Composable
fun BrailleKeypad(
    active: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DotButtonColumn(listOf(1, 2, 3), active, onToggle, Modifier.weight(1f))
        DotButtonColumn(listOf(4, 5, 6), active, onToggle, Modifier.weight(1f))
    }
}

@Composable
private fun DotButtonColumn(
    dots: List<Int>,
    active: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        dots.forEach { dot ->
            DotButton(dot = dot, active = active.contains(dot), onClick = { onToggle(dot) })
        }
    }
}

@Composable
private fun DotButton(
    dot: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
    val fg = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Titik $dot ${if (active) "aktif" else "nonaktif"}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            dot.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = fg,
            fontWeight = FontWeight.Bold,
        )
    }
}
