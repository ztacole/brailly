package com.brailly.friendly.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsDim

/**
 * Bilah navigasi bawah.
 *
 * Batas atasnya digambar sebagai garis 2dp, bukan bayangan elevation, mengikuti
 * aturan kanvas rata: seluruh kedalaman di aplikasi ini berasal dari garis dan
 * ledge. Tab aktif ditandai pil bernada lembut agar ikonnya tetap terbaca.
 *
 * Logika dan rute navigasi tidak berubah; komponen ini hanya menerima tab yang
 * terpilih beserta callback-nya.
 */
@Composable
fun BraillyBottomBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        HorizontalDivider(
            thickness = DsDim.borderWidth,
            color = Ds.colors.neutralBorder,
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            BottomTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                    label = { Text(tab.label, style = Ds.text.tab, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
