package com.brailly.friendly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brailly.friendly.core.braille.BrailleCell

/**
 * Menampilkan satu sel Braille 6 titik.
 * Titik aktif = warna aksen; titik non-aktif = outline lembut.
 */
@Composable
fun BrailleCellView(
    cell: BrailleCell,
    modifier: Modifier = Modifier,
    dotSize: Dp = 14.dp,
    gap: Dp = 6.dp,
    activeColor: Color = MaterialTheme.colorScheme.secondary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentDescription: String? = null,
) {
    val desc = contentDescription
    Row(
        modifier = modifier
//            .clip(RoundedCornerShape(8.dp))
            .then(if (desc != null) Modifier.semantics { this.contentDescription = desc } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Kolom kiri: titik 1,2,3 ; kolom kanan: titik 4,5,6
        DotColumn(listOf(1, 2, 3), cell, dotSize, gap, activeColor, inactiveColor)
        DotColumn(listOf(4, 5, 6), cell, dotSize, gap, activeColor, inactiveColor)
    }
}

@Composable
private fun DotColumn(
    dots: List<Int>,
    cell: BrailleCell,
    dotSize: Dp,
    gap: Dp,
    activeColor: Color,
    inactiveColor: Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (dot in dots) {
            Dot(active = cell.has(dot), size = dotSize, activeColor = activeColor, inactiveColor = inactiveColor)
        }
    }
}

@Composable
private fun Dot(
    active: Boolean,
    size: Dp,
    activeColor: Color,
    inactiveColor: Color,
) {
    Column(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) activeColor else inactiveColor)
            .padding(0.dp)
    ) {}
}
