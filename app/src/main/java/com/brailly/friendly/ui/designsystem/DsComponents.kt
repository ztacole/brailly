package com.brailly.friendly.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Komponen bergaya slab 3D.
 *
 * Kedalaman datang dari bidang ledge 4dp di bawah muka elemen, bukan dari
 * bayangan: menekan tombol menurunkan mukanya sampai menempel ke ledge.
 * Tidak ada elevation di mana pun.
 *
 * Ikon diterima sebagai [ImageVector] karena seluruh ikon aplikasi ini sudah
 * tersedia sebagai ImageVector di `BraillyIcons`, sehingga tidak ada pustaka
 * ikon tambahan yang dibutuhkan.
 */

// ───────────────────────── Tombol slab ─────────────────────────

enum class DsButtonVariant { Primary, Neutral, Destructive }

@Composable
fun DsButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DsButtonVariant = DsButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val (face, ledge, content) = when {
        !enabled -> Triple(c.neutralFill, c.neutralLedge, cs.onSurfaceVariant)
        variant == DsButtonVariant.Primary -> Triple(cs.primary, cs.primary.ledge(), cs.onPrimary)
        variant == DsButtonVariant.Neutral -> Triple(cs.surface, c.neutralLedge, cs.onSurface)
        else -> Triple(cs.error, cs.error.ledge(), cs.onError)
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val faceOffset by animateDpAsState(
        targetValue = if (pressed && enabled) DsDim.ledge else 0.dp,
        animationSpec = if (pressed) tween(80) else spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "slab",
    )
    val haptics = LocalHapticFeedback.current
    val sounds = LocalDsSounds.current
    val shape = RoundedCornerShape(DsDim.radiusButton)

    Box(
        modifier
            .height(DsDim.buttonFace + DsDim.ledge)
            // Label ditulis kapital, tetapi yang dibacakan pembaca layar adalah
            // teks aslinya. Sebagian mesin suara melafalkan kata berhuruf besar
            // sebagai singkatan, huruf demi huruf.
            .clearAndSetSemantics { contentDescription = title },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(DsDim.buttonFace)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(ledge),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(DsDim.buttonFace)
                .offset(y = faceOffset)
                .clip(shape)
                .background(face)
                .then(
                    if (variant == DsButtonVariant.Neutral && enabled) {
                        Modifier.border(DsDim.borderWidth, c.neutralBorder, shape)
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = interaction,
                    indication = null,
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sounds.play(DsSound.Press)
                    onClick()
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                Box(Modifier.size(8.dp))
            }
            Text(
                title.uppercase(),
                style = Ds.text.button,
                color = content,
                maxLines = 1,
                // Label kapital berbobot berat mudah melebihi lebarnya pada
                // skala huruf besar. Dipotong dengan elipsis, bukan dibiarkan
                // terpangkas di tengah huruf.
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Tombol ikon slab berukuran 48dp, tetap memenuhi sasaran sentuh minimum.
 * Muka tombolnya penuh 48dp dan ledge-nya ditambahkan di bawahnya, bukan
 * dipotong dari dalamnya.
 */
@Composable
fun DsIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    face: Color = MaterialTheme.colorScheme.surface,
    ledge: Color? = null,
    bordered: Boolean = true,
) {
    val c = Ds.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val faceOffset by animateDpAsState(
        targetValue = if (pressed && enabled) DsDim.ledge else 0.dp,
        animationSpec = if (pressed) tween(80) else spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "iconSlab",
    )
    val haptics = LocalHapticFeedback.current
    val sounds = LocalDsSounds.current
    val shape = RoundedCornerShape(DsDim.radiusTile)
    val resolvedLedge = when {
        !enabled -> c.neutralLedge
        ledge != null -> ledge
        else -> c.neutralLedge
    }
    val resolvedFace = if (enabled) face else c.neutralFill
    val resolvedTint = if (enabled) tint else tint.copy(alpha = 0.38f)

    Box(modifier.size(DsDim.iconButton, DsDim.iconButton + DsDim.ledge)) {
        Box(
            Modifier
                .size(DsDim.iconButton)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(resolvedLedge),
        )
        Box(
            Modifier
                .size(DsDim.iconButton)
                .offset(y = faceOffset)
                .clip(shape)
                .background(resolvedFace)
                .then(
                    if (bordered) {
                        Modifier.border(DsDim.borderWidth, c.neutralBorder, shape)
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = interaction,
                    indication = null,
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sounds.play(DsSound.Press)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = resolvedTint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ───────────────────────── Wadah ─────────────────────────

/**
 * Lapisan slab serbaguna: ledge 4dp, border 2dp, tanpa bayangan.
 *
 * Dipakai ulang oleh kartu, tile jawaban, dan panel. [faceColor] menentukan
 * muka, [ledgeColor] bidang bawahnya, dan [borderColor] garis tepinya.
 */
@Composable
fun DsSlabSurface(
    modifier: Modifier = Modifier,
    faceColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = Ds.colors.neutralBorder,
    ledgeColor: Color = Ds.colors.neutralLedge,
    radius: Dp = DsDim.radiusCard,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            // Ruang untuk ledge disisakan di bawah, supaya tinggi total elemen
            // tetap terbaca oleh tata letak induknya.
            .padding(bottom = DsDim.ledge)
            .drawBehind {
                drawRoundRect(
                    color = ledgeColor,
                    topLeft = Offset(0f, DsDim.ledge.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(radius.toPx()),
                )
            }
            .clip(shape)
            .background(faceColor)
            .border(DsDim.borderWidth, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
    ) {
        content()
    }
}

// ───────────────────────── Progres ─────────────────────────

@Composable
fun DsProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val c = Ds.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "progress",
    )
    val pill = RoundedCornerShape(50)
    Box(
        modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(pill)
            .background(c.neutralFill),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(pill)
                .background(MaterialTheme.colorScheme.primary),
        ) {
            // Garis sorot tipis di bagian atas isian: inilah yang memberi
            // kesan isian menonjol tanpa memakai bayangan.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp, start = 8.dp, end = 8.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(pill)
                    .background(c.progressFill.copy(alpha = 0.6f)),
            )
        }
    }
}

// ───────────────────────── Tile jawaban ─────────────────────────

enum class DsTileState { Idle, Selected, Correct, Wrong }

/**
 * Tile pilihan jawaban.
 *
 * Keadaan benar dan salah tidak pernah ditandai warna saja: yang salah ikut
 * bergetar dan yang benar ikut memberi getaran haptik, dan lembar umpan balik
 * di bawahnya menambahkan glyph centang atau silang.
 */
@Composable
fun DsAnswerTile(
    state: DsTileState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors

    val fill = when (state) {
        DsTileState.Idle -> cs.surface
        DsTileState.Selected -> c.selectedContainer
        DsTileState.Correct -> c.successContainer
        DsTileState.Wrong -> c.wrongContainer
    }
    val border = when (state) {
        DsTileState.Idle -> c.neutralBorder
        DsTileState.Selected -> c.selectedBorder
        DsTileState.Correct -> c.success
        DsTileState.Wrong -> cs.error
    }
    val ledge = if (state == DsTileState.Idle) c.neutralLedge else border.ledge()

    val shake = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val sounds = LocalDsSounds.current
    LaunchedEffect(state) {
        when (state) {
            DsTileState.Wrong -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sounds.play(DsSound.Wrong)
                repeat(4) { shake.animateTo(if (it % 2 == 0) 6f else -6f, tween(50)) }
                shake.animateTo(0f, tween(50))
            }
            DsTileState.Correct -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sounds.play(DsSound.Correct)
            }
            else -> Unit
        }
    }

    DsSlabSurface(
        modifier = modifier
            .absoluteOffset(x = shake.value.dp)
            .then(
                // Disalin ke variabel lokal: di dalam blok semantics, nama
                // `contentDescription` menunjuk properti penerimanya, bukan
                // parameter fungsi ini.
                contentDescription?.let { label ->
                    Modifier.semantics { this.contentDescription = label }
                } ?: Modifier,
            ),
        faceColor = fill,
        borderColor = border,
        ledgeColor = ledge,
        radius = DsDim.radiusTile,
        contentPadding = 14.dp,
        onClick = onTap,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
    }
}

/** Warna teks yang sepadan dengan isian [DsAnswerTile]. */
@Composable
fun dsTileTextColor(state: DsTileState): Color = when (state) {
    DsTileState.Idle, DsTileState.Selected -> MaterialTheme.colorScheme.onSurface
    DsTileState.Correct -> Ds.colors.success
    DsTileState.Wrong -> MaterialTheme.colorScheme.error
}

// ───────────────────────── Node jalur belajar ─────────────────────────

enum class DsNodeState { Locked, Available, Current, Completed }

/**
 * Node slab pada jalur belajar.
 *
 * Node yang sedang berjalan dibuat lebih besar dan dikelilingi cincin berdenyut
 * supaya titik lanjut pengguna terlihat tanpa harus membaca label.
 */
@Composable
fun DsSlabNode(
    state: DsNodeState,
    shape: Shape,
    face: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    animate: Boolean = true,
    content: @Composable () -> Unit,
) {
    val c = Ds.colors
    val enabled = state != DsNodeState.Locked
    val ledge = if (enabled) face.ledge() else c.neutralLedge

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "node",
    )
    val haptics = LocalHapticFeedback.current
    val sounds = LocalDsSounds.current

    val ring = if (state == DsNodeState.Current && animate) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                tween(900, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
            label = "ring",
        ).value
    } else {
        1f
    }

    Box(modifier.size(size + DsDim.ledge), contentAlignment = Alignment.Center) {
        if (state == DsNodeState.Current) {
            Box(
                Modifier
                    .size(size)
                    .scale(ring)
                    .clip(CircleShape)
                    .border(4.dp, c.progressFill, CircleShape),
            )
        }
        Box(
            Modifier
                .size(size)
                .offset(y = DsDim.ledge)
                .clip(shape)
                .background(ledge),
        )
        Box(
            Modifier
                .size(size)
                .scale(pressScale)
                .clip(shape)
                .background(if (enabled) face else c.neutralFill)
                .then(
                    if (enabled) {
                        Modifier.clickable(interactionSource = interaction, indication = null) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sounds.play(DsSound.Press)
                            onClick()
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

// ───────────────────────── HUD ─────────────────────────

@Composable
fun DsHudChip(
    icon: ImageVector,
    tint: Color,
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
) {
    val label = contentDescription
    val scale = if (animate) {
        rememberInfiniteTransition(label = "hud").animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                tween(2000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
            label = "idle",
        ).value
    } else {
        1f
    }

    Row(
        modifier
            .defaultMinSize(minHeight = DsDim.iconButton)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clearAndSetSemantics { this.contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .scale(scale),
        )
        Text(value, style = Ds.text.hudNumber, color = tint, maxLines = 1)
    }
}

// ───────────────────────── Kotak teks ─────────────────────────

/**
 * Kotak teks bergaya slab: radius 12dp, border 2dp, tanpa isian maupun garis
 * bawah ala Material.
 *
 * Dibangun dari [BasicTextField] alih-alih `OutlinedTextField` karena ketebalan
 * garis tepi bawaan Material tidak dapat disetel lewat parameter, dan nilainya
 * 1dp saat tidak terfokus. Labelnya diletakkan di atas kotak, bukan mengapung
 * di garis tepi, sehingga tetap terbaca setelah kotaknya terisi.
 */
@Composable
fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    isError: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val border = when {
        isError -> cs.error
        focused -> c.selectedBorder
        else -> c.neutralBorder
    }
    val shape = RoundedCornerShape(DsDim.radiusTile)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(label, style = Ds.text.labelUpper, color = cs.onSurfaceVariant)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cs.surface)
                .border(DsDim.borderWidth, border, shape)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = Ds.text.body.copy(color = cs.onSurface),
                cursorBrush = SolidColor(cs.primary),
                singleLine = singleLine,
                minLines = minLines,
                interactionSource = interaction,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                placeholder,
                                style = Ds.text.body,
                                color = cs.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

// ───────────────────────── Umpan balik & perayaan ─────────────────────────

/**
 * Lembar umpan balik jawaban, muncul dari bawah soal.
 *
 * Isiannya memakai [DsColors.successContainer] atau [DsColors.wrongContainer],
 * dengan glyph dan judul berwarna senada, lalu satu tombol slab untuk lanjut.
 */
@Composable
fun DsFeedbackSheet(
    correct: Boolean,
    title: String,
    glyph: ImageVector,
    modifier: Modifier = Modifier,
    detail: @Composable (() -> Unit)? = null,
    action: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val accent = if (correct) c.success else cs.error
    val shape = RoundedCornerShape(
        topStart = DsDim.radiusSheet,
        topEnd = DsDim.radiusSheet,
        bottomStart = DsDim.radiusTile,
        bottomEnd = DsDim.radiusTile,
    )

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (correct) c.successContainer else c.wrongContainer)
            .border(DsDim.borderWidth, accent, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Glyph wajib ada: benar dan salah tidak boleh dibedakan hanya
            // lewat warna.
            Icon(glyph, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            Text(title, style = Ds.text.sectionHeader, color = accent)
        }
        detail?.invoke()
        action()
    }
}

/** Hujan konfeti singkat memakai warna tema. */
@Composable
fun DsConfetti(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val c = Ds.colors
    val palette = listOf(cs.primary, c.streak, c.reward, c.info, cs.error, c.premium)
    val pieces = remember(palette) {
        List(14) {
            ConfettiPiece(
                x = Random.nextFloat(),
                color = palette.random(),
                delayMillis = Random.nextInt(0, 300),
                spin = Random.nextFloat() * 720f - 360f,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(CONFETTI_MILLIS)) }

    Canvas(modifier.fillMaxSize()) {
        pieces.forEach { p ->
            val t = ((progress.value * CONFETTI_MILLIS - p.delayMillis) / CONFETTI_MILLIS)
                .coerceIn(0f, 1f)
            val y = t * (size.height + 40f) - 20f
            rotate(p.spin * t, pivot = Offset(p.x * size.width, y)) {
                drawRoundRect(
                    color = p.color,
                    topLeft = Offset(p.x * size.width - 4f, y),
                    size = Size(8.dp.toPx(), 14.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

private data class ConfettiPiece(
    val x: Float,
    val color: Color,
    val delayMillis: Int,
    val spin: Float,
)

private const val CONFETTI_MILLIS = 1200
