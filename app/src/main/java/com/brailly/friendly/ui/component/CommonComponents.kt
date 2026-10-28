package com.brailly.friendly.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsButton
import com.brailly.friendly.ui.designsystem.DsButtonVariant
import com.brailly.friendly.ui.designsystem.DsIconButton
import com.brailly.friendly.ui.designsystem.DsSlabSurface

/**
 * Komponen umum aplikasi, kini digambar bergaya slab 3D.
 *
 * Tanda tangan setiap composable di berkas ini tidak berubah, sehingga seluruh
 * layar ikut berganti tampilan tanpa satu pun pemanggilnya perlu disentuh.
 * Isinya mendelegasikan ke komponen di `ui/designsystem`.
 */

/** Tombol utama: muka 56dp di atas ledge 4dp, label kapital. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    DsButton(
        title = text,
        onClick = onClick,
        modifier = modifier,
        variant = DsButtonVariant.Primary,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

/** Tombol sekunder: muka permukaan dengan border 2dp, bentuk slab yang sama. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    DsButton(
        title = text,
        onClick = onClick,
        modifier = modifier,
        variant = DsButtonVariant.Neutral,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

/**
 * Tombol ikon dengan label semantik untuk pembaca layar.
 *
 * Mukanya tetap 48dp penuh dan ledge ditambahkan di bawahnya, bukan dipotong
 * dari dalamnya, supaya sasaran sentuhnya tidak menyusut di bawah 48dp.
 */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    DsIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        tint = tint,
        face = container,
    )
}

/**
 * Kartu slab: permukaan rata, border 2dp, ledge 4dp, tanpa bayangan.
 *
 * Kedalaman sengaja datang dari ledge, bukan `CardDefaults.cardElevation`,
 * sehingga tampilannya tetap sama di mode terang maupun gelap. Elevation
 * Material menipis sampai hampir tak terlihat di atas permukaan gelap.
 */
@Composable
fun BraillyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    DsSlabSurface(
        modifier = modifier,
        faceColor = containerColor,
        onClick = onClick,
        content = content,
    )
}

/** Header layar. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                title,
                style = Ds.text.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = Ds.text.helper,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) trailing()
    }
}
