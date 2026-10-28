package com.brailly.friendly.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.brailly.friendly.R

/**
 * Hierarki tipografi design system: dua suara huruf.
 *
 * - **[Fredoka]** adalah suara emosi: judul, angka besar, angka HUD.
 * - **[VarelaRound]** adalah suara antarmuka: tombol, tile, label, teks isi.
 *
 * Keduanya berlisensi SIL Open Font License 1.1 dan diambil dari repositori
 * resmi `google/fonts`. Teks lisensinya disertakan di `licenses/` pada akar
 * proyek, sebagaimana diwajibkan OFL saat berkas hurufnya didistribusikan
 * ulang. Berkas huruf adalah sumber daya, bukan dependensi, jadi penambahannya
 * tidak menyentuh Gradle.
 *
 * ```
 * app/src/main/res/font/fredoka_variable.ttf       (Fredoka, variable)
 * app/src/main/res/font/varela_round_regular.ttf   (Varela Round, Regular)
 * ```
 */

/**
 * Satu contoh bobot dari berkas variable Fredoka.
 *
 * Nilai `variationSettings` wajib diisi. Tanpanya seluruh bobot akan digambar
 * memakai contoh bawaan berkas, yang pada Fredoka adalah Light 300, sehingga
 * judul yang seharusnya tebal justru tampil paling tipis. Pengaturan variasi
 * berlaku sejak API 26, dan minSdk aplikasi ini 26.
 */
private fun fredokaInstance(weight: FontWeight): Font = Font(
    resId = R.font.fredoka_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/**
 * Suara emosi.
 *
 * Fredoka hanya tersedia sebagai variable font dengan sumbu bobot 300..700,
 * jadi hanya rentang itu yang didaftarkan. Permintaan bobot di atas 700 akan
 * dipetakan ke Bold, yang memang bobot terberat yang dimiliki huruf ini.
 *
 * Nama berkasnya di repositori asal adalah `Fredoka[wdth,wght].ttf`. Nama itu
 * diubah menjadi `fredoka_variable.ttf` karena nama sumber daya Android hanya
 * menerima huruf kecil, angka, dan garis bawah.
 */
val Fredoka: FontFamily = FontFamily(
    fredokaInstance(FontWeight.Normal),
    fredokaInstance(FontWeight.Medium),
    fredokaInstance(FontWeight.SemiBold),
    fredokaInstance(FontWeight.Bold),
)

/**
 * Suara antarmuka.
 *
 * Varela Round hanya diterbitkan dalam satu bobot, Regular 400. Peran yang
 * meminta Bold atau Black karena itu ditebalkan secara sintetis oleh sistem:
 * garis hurufnya dilebarkan, bukan diganti dengan potongan huruf tebal yang
 * sebenarnya. Hasilnya terbaca, tetapi tidak sepadat huruf tebal asli.
 *
 * Bila label tombol terasa kurang berbobot di perangkat, cukup ubah satu baris
 * pada [DsDefaultText] menjadi `ui = Fredoka`: Fredoka punya bobot 300..700
 * yang asli dan sama-sama berwajah membulat, sehingga keduanya serasi.
 */
val VarelaRound: FontFamily = FontFamily(
    Font(R.font.varela_round_regular, FontWeight.Normal),
)

@Immutable
class DsTextRoles(
    /** Judul berskala wordmark. */
    val display: TextStyle,
    /** Angka besar: skor akhir, hitungan streak. */
    val heroNumber: TextStyle,
    val screenTitle: TextStyle,
    /** Pertanyaan latihan. */
    val question: TextStyle,
    val hudNumber: TextStyle,
    val sectionHeader: TextStyle,
    val skillLabel: TextStyle,
    val button: TextStyle,
    val tile: TextStyle,
    val body: TextStyle,
    val helper: TextStyle,
    val meta: TextStyle,
    val tab: TextStyle,
    val labelUpper: TextStyle,
    /**
     * Unicode braille (U+2800..U+28FF).
     *
     * Satu-satunya peran yang menetapkan keluarga hurufnya sendiri, dan tidak
     * ikut berganti saat huruf merek diubah. Monospace di sini bukan pilihan
     * gaya: huruf berlebar proporsional membuat deretan sel braille tidak
     * sejajar, sehingga pola titiknya sulit dibandingkan.
     */
    val brailleUnicode: TextStyle,
)

/** Ukuran mengikuti brief satu-ke-satu (pt → sp). */
fun dsTextRoles(display: FontFamily, ui: FontFamily) = DsTextRoles(
    display = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp,
    ),
    heroNumber = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold,
        fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = (-1.0).sp,
    ),
    screenTitle = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp,
    ),
    question = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp,
    ),
    hudNumber = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp,
    ),
    sectionHeader = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Black,
        fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp,
    ),
    skillLabel = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Black,
        fontSize = 16.sp, lineHeight = 19.sp,
    ),
    button = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Black,
        fontSize = 17.sp, lineHeight = 17.sp, letterSpacing = 0.4.sp,
    ),
    tile = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 20.sp,
    ),
    body = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    helper = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp,
    ),
    meta = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 16.sp,
    ),
    tab = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 11.sp, letterSpacing = 0.2.sp,
    ),
    labelUpper = TextStyle(
        fontFamily = ui, fontWeight = FontWeight.Black,
        fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 1.0.sp,
    ),
    brailleUnicode = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = 2.0.sp,
    ),
)

val DsDefaultText: DsTextRoles = dsTextRoles(display = Fredoka, ui = VarelaRound)

/**
 * Terapkan hierarki yang sama pada komponen Material yang tidak di-restyle,
 * misalnya `AlertDialog`, `FilterChip`, dan `SegmentedButton`.
 *
 * Slot yang tidak dipetakan hierarki ini dibiarkan memakai nilai milik
 * aplikasi, sehingga tidak ada peran yang kehilangan definisinya.
 */
fun Typography.withDuolingoFonts(): Typography = copy(
    displayLarge = DsDefaultText.heroNumber,
    headlineLarge = DsDefaultText.screenTitle,
    headlineMedium = DsDefaultText.question,
    titleLarge = DsDefaultText.sectionHeader,
    titleMedium = DsDefaultText.skillLabel,
    bodyLarge = DsDefaultText.body,
    bodyMedium = DsDefaultText.body,
    labelLarge = DsDefaultText.button,
    labelMedium = DsDefaultText.meta,
    labelSmall = DsDefaultText.tab,
)
