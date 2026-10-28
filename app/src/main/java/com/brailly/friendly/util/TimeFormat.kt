package com.brailly.friendly.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Format waktu untuk daftar riwayat.
 *
 * `java.time` dipakai langsung tanpa desugaring karena minSdk aplikasi ini 26.
 */

private val INDONESIAN = Locale.forLanguageTag("id-ID")

private val TIME_ONLY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", INDONESIAN)

private val FULL_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", INDONESIAN)

/**
 * Ubah epoch millis menjadi keterangan waktu yang singkat.
 *
 * Entri hari ini dan kemarin diberi label relatif, karena itulah yang paling
 * sering dicari pengguna setelah memindai, dan tanggal lengkap untuk hasil
 * beberapa menit lalu lebih banyak menuntut pembacaan daripada memberi
 * informasi. Selebihnya memakai tanggal penuh supaya tetap tidak ambigu.
 */
fun Long.asHistoryTimestamp(zone: ZoneId = ZoneId.systemDefault()): String {
    val moment = Instant.ofEpochMilli(this).atZone(zone)
    val today = LocalDate.now(zone)
    return when (moment.toLocalDate()) {
        today -> "Hari ini, ${TIME_ONLY.format(moment)}"
        today.minusDays(1) -> "Kemarin, ${TIME_ONLY.format(moment)}"
        else -> FULL_DATE.format(moment)
    }
}
