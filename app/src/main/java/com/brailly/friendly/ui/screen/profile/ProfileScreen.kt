package com.brailly.friendly.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.data.speech.SpeechEngineStatus
import com.brailly.friendly.data.speech.SpeechRate
import com.brailly.friendly.data.speech.TextToSpeechManager
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.ScreenHeader
import com.brailly.friendly.ui.component.SecondaryButton
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsDim
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsDim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = "Profil", subtitle = "Pengaturan & statistik")

        // Kartu profil ringkas
        BraillyCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    BraillyIcons.Profile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.size(16.dp))
                Column {
                    Text("Pengguna Brailly", style = Ds.text.sectionHeader, color = MaterialTheme.colorScheme.onPrimary)
                    Text("Pelajar Braille", style = Ds.text.helper, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Statistik
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), BraillyIcons.Streak, "3", "Hari streak")
            StatCard(Modifier.weight(1f), BraillyIcons.Trophy, "12", "Huruf dikuasai")
        }

        Text("Pengaturan", style = Ds.text.sectionHeader, color = MaterialTheme.colorScheme.onBackground)

        // Toggle tema
        BraillyCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (darkTheme) BraillyIcons.Moon else BraillyIcons.Sun,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("Mode Gelap", style = Ds.text.skillLabel, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (darkTheme) "Aktif" else "Nonaktif",
                            style = Ds.text.helper,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(checked = darkTheme, onCheckedChange = onToggleTheme)
            }
        }

        SpeechRateSetting()
        // "Ukuran Teks" masih placeholder MVP: belum ada yang membacanya.
        SettingItem(BraillyIcons.Edit, "Ukuran Teks", "Normal")

        Spacer(Modifier.size(8.dp))
        Text(
            "Brailly MVP · v1.0",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Setelan kecepatan baca untuk fitur bacakan (text-to-speech).
 *
 * Pilihannya langsung diterapkan dan dibacakan sebagai contoh, karena
 * "Lambat" dan "Cepat" hanya berarti setelah didengar. Nilainya disimpan oleh
 * [com.brailly.friendly.data.speech.TextToSpeechManager] sehingga bertahan
 * antar sesi.
 */
@Composable
private fun SpeechRateSetting() {
    val speaker: TextToSpeechManager = koinInject()
    val speech by speaker.state.collectAsStateWithLifecycle()

    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    BraillyIcons.Speaker,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        "Kecepatan Suara",
                        style = Ds.text.skillLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        // `problem` memuat alasan paling akhir, baik mesinnya
                        // yang gagal disiapkan maupun satu permintaan baca
                        // yang ditolak, jadi diutamakan atas teks biasa.
                        speech.problem ?: when {
                            !speech.isReady -> "Menyiapkan mesin suara..."
                            speech.usingFallbackLanguage ->
                                "Mesin suara tidak punya bahasa Indonesia, " +
                                    "memakai bahasa bawaan"
                            else -> "Dipakai saat membacakan hasil scan dan riwayat"
                        },
                        style = Ds.text.helper,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SpeechRate.entries.forEachIndexed { index, rate ->
                    SegmentedButton(
                        selected = speech.rate == rate,
                        onClick = {
                            speaker.setRate(rate)
                            speaker.speak(
                                "Kecepatan suara ${rate.label}",
                                utteranceId = SPEECH_RATE_PREVIEW_ID,
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SpeechRate.entries.size,
                        ),
                    ) { Text(rate.label) }
                }
            }

            // Jalan keluar saat mesin suaranya gagal disiapkan, misalnya
            // karena baru dipasang setelah aplikasi berjalan. Tanpa ini
            // pengguna harus menutup aplikasinya dulu agar fitur bacakan
            // hidup kembali.
            if (speech.status is SpeechEngineStatus.Unavailable) {
                SecondaryButton(
                    text = "Siapkan Ulang Mesin Suara",
                    onClick = speaker::retryInit,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = BraillyIcons.Speaker,
                )
            }
        }
    }
}

/** Penanda contoh suara, dipisahkan dari bacaan hasil scan maupun riwayat. */
private const val SPEECH_RATE_PREVIEW_ID = "profile-speech-rate-preview"

@Composable
private fun StatCard(modifier: Modifier, icon: ImageVector, value: String, label: String) {
    BraillyCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            Text(value, style = Ds.text.heroNumber, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = Ds.text.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingItem(icon: ImageVector, title: String, value: String) {
    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.size(12.dp))
                Text(title, style = Ds.text.skillLabel, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(value, style = Ds.text.helper, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
