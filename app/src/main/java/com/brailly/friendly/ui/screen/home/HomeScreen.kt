package com.brailly.friendly.ui.screen.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brailly.friendly.core.braille.BrailleMap
import com.brailly.friendly.data.learn.LearnProgressRepository
import com.brailly.friendly.ui.component.BrailleCellView
import com.brailly.friendly.ui.component.BraillyCard
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.component.PrimaryButton
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsDim
import com.brailly.friendly.ui.designsystem.DsProgressBar
import com.brailly.friendly.ui.designsystem.DsSound
import com.brailly.friendly.ui.designsystem.LocalDsSounds
import com.brailly.friendly.ui.designsystem.dsAnimationsEnabled
import com.brailly.friendly.ui.designsystem.ledge
import com.brailly.friendly.ui.navigation.BottomTab
import org.koin.compose.koinInject

/**
 * Beranda Brailly: ragam kartu yang dinamis, tidak kaku, dan tidak berlebihan.
 *
 * Tiap kartu memiliki kepribadian visual yang berbeda sesuai fungsinya:
 * 1. StreakCard: Kartu kebiasaan dengan denyut api & pelacak 7 hari yang bersih
 * 2. LearningCard: Kartu hero pelajaran (progres kuis + satu tombol slab utama yang tegas)
 * 3. TranslateCard: Sepasang kartu slab alat yang taktil berdampingan (Terjemah & Scan)
 *
 * @param onNavigate pindah ke salah satu tab utama.
 * @param onOpenQuiz buka Jalur Kuis langsung, melewati layar kamus.
 */
@Composable
fun HomeScreen(
    onNavigate: (BottomTab) -> Unit,
    onOpenQuiz: () -> Unit,
    modifier: Modifier = Modifier,
    progressRepository: LearnProgressRepository = koinInject(),
) {
    val results by progressRepository.observeResults().collectAsStateWithLifecycle(initialValue = emptyList())
    val passedCount = results.count { it.passed }
    val totalLevels = 10

    val context = LocalContext.current
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

        // 1. Kartu Kebiasaan (Streak)
        StreakCard()

        // 2. Kartu Hero Belajar (Progres + 1 Aksi Utama)
        LearningCard(
            passedCount = passedCount,
            totalLevels = totalLevels,
            onOpenQuiz = onOpenQuiz,
            onOpenDictionary = { onNavigate(BottomTab.LEARN) },
        )

        // 3. Kartu Alat Bantu Taktil (Sepasang Slab Terjemah & Scan)
        TranslateCard(
            onOpenTranslate = { onNavigate(BottomTab.TRANSLATE) },
            onOpenScan = { onNavigate(BottomTab.SCAN) },
        )

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
                            style = Ds.text.skillLabel,
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

/**
 * Kartu Streak:
 * Bersih dan tidak bertumpuk kotak di dalam kotak. Ikon api bernapas dengan
 * denyut lembut 2 detik, dipadankan dengan baris 7 hari yang terbuka dan jelas.
 */
@Composable
private fun StreakCard() {
    val streakColor = Ds.colors.streak
    val animationsEnabled = dsAnimationsEnabled()
    val flameScale = if (animationsEnabled) {
        rememberInfiniteTransition(label = "streakFlame").animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "streakIdle",
        ).value
    } else {
        1f
    }

    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    BraillyIcons.Streak,
                    contentDescription = null,
                    tint = streakColor,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(flameScale),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "3 Hari Beruntun",
                        style = Ds.text.sectionHeader,
                        color = streakColor,
                    )
                    Text(
                        "Pertahankan kebiasaan belajarmu hari ini!",
                        style = Ds.text.helper,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Baris 7 hari pelacak kebiasaan tanpa wadah kotak ganda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val days = listOf("S", "S", "R", "K", "J", "S", "M")
                val streakCount = 3
                days.forEachIndexed { i, day ->
                    val isDone = i < streakCount
                    val isToday = i == streakCount - 1
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDone) streakColor else Ds.colors.neutralFill
                                )
                                .then(
                                    if (isToday) {
                                        Modifier.border(2.dp, streakColor.ledge(), CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isDone) {
                                Icon(
                                    BraillyIcons.Streak,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Ds.colors.neutralBorder),
                                )
                            }
                        }
                        Text(
                            text = day,
                            style = Ds.text.meta.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isDone) streakColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu Hero Belajar:
 * Berfokus pada kemajuan pembelajaran. Menampilkan progres level kuis secara
 * gamblang, satu tombol slab 3D yang tegas untuk melanjutkan kuis, dan tautan
 * sekunder ramah untuk membuka kamus tanpa berdesakan.
 */
@Composable
private fun LearningCard(
    passedCount: Int,
    totalLevels: Int,
    onOpenQuiz: () -> Unit,
    onOpenDictionary: () -> Unit,
) {
    BraillyCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        BraillyIcons.Trophy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Belajar Braille",
                        style = Ds.text.sectionHeader,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (passedCount > 0) "$passedCount dari $totalLevels level tuntas"
                        else "Mulai perjalanan belajar braille",
                        style = Ds.text.helper,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Bilah progres Duolingo
            val progress = if (totalLevels > 0) passedCount.toFloat() / totalLevels else 0f
            DsProgressBar(progress = progress)

            // Tombol utama satu aksi yang tegas
            PrimaryButton(
                text = if (passedCount > 0) "Lanjutkan Kuis" else "Mulai Kuis",
                onClick = onOpenQuiz,
                leadingIcon = BraillyIcons.Play,
                modifier = Modifier.fillMaxWidth(),
            )

            // Tautan sekunder yang halus dan tidak bersaing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDictionary),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Ingin hafalkan huruf dulu? ",
                    style = Ds.text.helper,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Buka Kamus →",
                    style = Ds.text.skillLabel.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/**
 * Kartu Alat Bantu Taktil (Terjemah & Scan):
 * Berbeda dari kartu belajar yang vertikal-sentris, di sini fitur alat kerja
 * disajikan sebagai sepasang kartu slab berdampingan yang responsif terhadap
 * sentuhan (menurun 4dp saat ditekan lengkap dengan haptic & audio click).
 */
@Composable
private fun TranslateCard(
    onOpenTranslate: () -> Unit,
    onOpenScan: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolCard(
            title = "Terjemah",
            subtitle = "Teks ke braille",
            icon = BraillyIcons.Translate,
            iconTint = Ds.colors.info,
            iconBg = Ds.colors.selectedContainer,
            onClick = onOpenTranslate,
            modifier = Modifier.weight(1f),
        )
        ToolCard(
            title = "Scan Braille",
            subtitle = "Pindai via kamera",
            icon = BraillyIcons.Camera,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBg = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenScan,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Kartu slab alat interaktif 3D:
 * Memiliki depth ledge 4dp, border 2dp, dan reaksi tekan mekanis Duolingo.
 */
@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val faceOffset by animateDpAsState(
        targetValue = if (pressed) DsDim.ledge else 0.dp,
        animationSpec = if (pressed) tween(80) else spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "toolCardSlab",
    )
    val haptics = LocalHapticFeedback.current
    val sounds = LocalDsSounds.current
    val shape = RoundedCornerShape(DsDim.radiusCard)

    Box(
        modifier = modifier
            .height(130.dp)
            .clearAndSetSemantics { contentDescription = "$title, $subtitle" },
    ) {
        // Ledge bawah 4dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = DsDim.ledge)
                .clip(shape)
                .background(c.neutralLedge),
        )
        // Muka kartu yang turun saat ditekan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset(y = faceOffset)
                .padding(bottom = DsDim.ledge)
                .clip(shape)
                .background(cs.surface)
                .border(DsDim.borderWidth, c.neutralBorder, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sounds.play(DsSound.Press)
                    onClick()
                }
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(DsDim.radiusTile))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = Ds.text.skillLabel,
                    color = cs.onSurface,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    style = Ds.text.meta,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
