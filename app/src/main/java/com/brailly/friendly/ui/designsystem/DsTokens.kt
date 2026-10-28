package com.brailly.friendly.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.brailly.friendly.ui.theme.BraillyBlue
import com.brailly.friendly.ui.theme.BraillyBlueLight
import com.brailly.friendly.ui.theme.BraillyOrange
import com.brailly.friendly.ui.theme.BraillyOrangeLight
import com.brailly.friendly.ui.theme.SuccessGreen

/**
 * Token desain Brailly: kedalaman "slab" 3D, border tebal, dan kanvas rata.
 *
 * Seluruh warna di sini diturunkan dari `MaterialTheme.colorScheme` dan dari
 * palet yang sudah ada di `ui/theme/Color.kt`. Tidak ada warna baru yang
 * diperkenalkan, dan berkas warna yang lama tidak disentuh.
 *
 * Hierarki tipografinya berada di `DsFonts.kt`.
 */

/** Nada lebih gelap untuk bidang ledge 3D. Berlaku untuk warna apa pun. */
fun Color.ledge(fraction: Float = 0.2f): Color = lerp(this, Color.Black, fraction)

/**
 * Warna isi yang kontras di atas warna ini.
 *
 * Ambang 0.179 adalah titik silang luminansi tempat kontras terhadap putih dan
 * terhadap hitam bernilai sama. Dipakai untuk glyph di atas muka berwarna merek:
 * `onSecondary` pada tema ini bernilai putih, dan putih di atas BraillyOrange
 * hanya mencapai 2.65:1, di bawah syarat 3:1 untuk glyph besar. Memilih
 * nadanya di sini memperbaiki kontras tanpa menyentuh `Theme.kt`.
 */
fun Color.readableContent(
    onDark: Color = Color.White,
    onLight: Color = Color(0xFF1A1C1E),
): Color = if (luminance() > 0.179f) onLight else onDark

// ───────────────────────── Warna ─────────────────────────

@Immutable
class DsColors(
    val success: Color,
    val successContainer: Color,
    /**
     * Isian untuk keadaan salah.
     *
     * Dipakai alih-alih `colorScheme.errorContainer`: tema aplikasi menetapkan
     * `error` tetapi tidak menetapkan `errorContainer`, sehingga nilainya jatuh
     * ke bawaan Material yang rona merahnya berbeda dari `ErrorRed` milik
     * Brailly. Menurunkannya di sini menjaga pasangan isian dan garis tetap
     * serumpun tanpa perlu mengubah `Theme.kt`.
     */
    val wrongContainer: Color,
    val selectedContainer: Color,
    val selectedBorder: Color,
    val progressFill: Color,
    val streak: Color,
    val reward: Color,
    val info: Color,
    val premium: Color,
    val neutralFill: Color,
    val neutralBorder: Color,
    val neutralLedge: Color,
)

/**
 * Peran yang tidak dimiliki Material, diisi dari palet Brailly yang sudah ada.
 * Null berarti diturunkan dari `colorScheme`.
 */
class DsOverride(
    val success: Color? = null,
    val streak: Color? = null,
    val reward: Color? = null,
    val info: Color? = null,
    val premium: Color? = null,
)

/** Mode terang memakai nada penuh; semuanya sudah ada di `Color.kt`. */
val DsLightOverride = DsOverride(
    success = SuccessGreen,
    streak = BraillyOrange,
    reward = BraillyOrange,
    info = BraillyBlue,
    premium = BraillyOrangeLight,
)

/**
 * Mode gelap memakai nada terang dari palet yang sama. Nada penuh seperti
 * [BraillyOrange] dan [BraillyBlue] tidak mencapai kontras 4.5:1 di atas
 * permukaan gelap bila dipakai sebagai teks.
 */
val DsDarkOverride = DsOverride(
    success = SuccessGreen,
    streak = BraillyOrangeLight,
    reward = BraillyOrangeLight,
    info = BraillyBlueLight,
    premium = BraillyOrangeLight,
)

@Composable
fun dsColors(): DsColors {
    val cs = MaterialTheme.colorScheme
    return remember(cs) {
        val isLight = cs.background.luminance() > 0.5f
        val o = if (isLight) DsLightOverride else DsDarkOverride

        val success = o.success ?: cs.primary
        val info = o.info ?: cs.secondary
        val tintLow = if (isLight) 0.16f else 0.28f

        DsColors(
            success = success,
            successContainer = lerp(cs.surface, success, tintLow),
            wrongContainer = lerp(cs.surface, cs.error, tintLow),
            selectedContainer = lerp(cs.surface, info, tintLow),
            selectedBorder = lerp(cs.surface, info, 0.65f),
            progressFill = lerp(cs.primary, Color.White, 0.25f),
            streak = o.streak ?: cs.tertiary,
            reward = o.reward ?: cs.secondary,
            info = info,
            premium = o.premium ?: cs.tertiary,
            neutralFill = lerp(cs.surface, cs.onSurface, 0.08f),
            neutralBorder = lerp(cs.surface, cs.onSurface, 0.15f),
            neutralLedge = lerp(cs.surface, cs.onSurface, 0.25f),
        )
    }
}

// ───────────────────────── Pengakses ─────────────────────────

object Ds {
    val colors: DsColors @Composable get() = dsColors()

    /**
     * Peran teks tidak bergantung pada `MaterialTheme`, jadi tidak perlu
     * berada dalam komposisi. Lihat `DsFonts.kt`.
     */
    val text: DsTextRoles get() = DsDefaultText
}

// ───────────────────────── Ukuran ─────────────────────────

/** Angka yang dipakai berulang oleh komponen slab. */
object DsDim {
    /** Tinggi bidang ledge yang tersingkap di bawah muka elemen. */
    val ledge = 4.dp

    val borderWidth = 2.dp

    val buttonFace = 56.dp
    val iconButton = 48.dp
    val tileFace = 48.dp

    val radiusButton = 16.dp
    val radiusCard = 16.dp
    val radiusTile = 12.dp
    val radiusSheet = 24.dp

    val screenPadding = 16.dp
    val sectionGap = 24.dp
}
