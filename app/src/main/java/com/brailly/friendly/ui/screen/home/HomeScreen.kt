package com.brailly.friendly.ui.screen.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.brailly.friendly.core.braille.BrailleMap
import com.brailly.friendly.ui.component.BrailleCellView
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsDim
import com.brailly.friendly.ui.designsystem.readableContent
import com.brailly.friendly.ui.navigation.BottomTab

@Composable
fun HomeScreen(
    onNavigate: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Buka tautan banner keluar aplikasi: pakai aplikasi YouTube bila ada,
    // selain itu jatuh ke browser melalui ACTION_VIEW biasa.
    val openLink: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Tidak ada aplikasi untuk membuka tautan", Toast.LENGTH_SHORT)
                .show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsDim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Sapaan
        Column {
            Text(
                "Selamat datang di",
                style = Ds.text.helper,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Brailly",
                style = Ds.text.display,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Kartu streak
        StreakCard()

        // Shortcut cepat
        Text(
            "Fitur Cepat",
            style = Ds.text.sectionHeader,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = BraillyIcons.Translate,
                title = "Terjemah",
                subtitle = "Teks ke Braille",
                onClick = { onNavigate(BottomTab.TRANSLATE) },
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = BraillyIcons.Scan,
                title = "Scan",
                subtitle = "Foto braille",
                onClick = { onNavigate(BottomTab.SCAN) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = BraillyIcons.Learn,
                title = "Belajar",
                subtitle = "Alfabet & kuis",
                onClick = { onNavigate(BottomTab.LEARN) },
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = BraillyIcons.Profile,
                title = "Profil",
                subtitle = "Pengaturan",
                onClick = { onNavigate(BottomTab.PROFILE) },
            )
        }

        // Sorotan alfabet
        Text(
            "Cuplikan Alfabet",
            style = Ds.text.sectionHeader,
            color = MaterialTheme.colorScheme.onBackground,
        )
        BraillyCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf('a', 'b', 'c', 'd', 'e').forEach { ch ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BrailleCellView(
                            cell = BrailleMap.letters[ch]!!,
                            dotSize = 10.dp,
                            gap = 4.dp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            ch.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Text(
            "Apa Yang Dirasakan Tunanetra",
            style = Ds.text.sectionHeader,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HomeBanner(
            items = placeholderBanners,
            onOpenLink = openLink,
        )
    }
}

@Composable
private fun StreakCard() {
    val streak = Ds.colors.streak
    BraillyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = streak,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                BraillyIcons.Streak,
                contentDescription = null,
                // Glyph dan teks memakai nada yang kontras di atas muka
                // oranye, bukan `onPrimary` yang bernilai putih.
                tint = streak.readableContent(),
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    "3 hari beruntun",
                    style = Ds.text.sectionHeader,
                    color = streak.readableContent(),
                )
                Text(
                    "Lanjutkan belajar hari ini untuk menjaga streak-mu",
                    style = Ds.text.helper,
                    color = streak.readableContent(),
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillyCard(modifier = modifier, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(title, style = Ds.text.skillLabel, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = Ds.text.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
