package com.brailly.friendly.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

/**
 * Aksi "salin" dan "bagikan" untuk teks hasil terjemahan maupun pemindaian.
 *
 * Keduanya dikumpulkan di sini supaya hasil scan, riwayat, dan layar terjemah
 * berperilaku sama: satu tempat yang menentukan label papan klip, judul
 * pemilih aplikasi, dan kapan umpan balik ditampilkan.
 */

/** Label papan klip, ikut terlihat di riwayat clipboard sebagian peluncur. */
private const val CLIP_LABEL = "Brailly"

/**
 * Salin [text] ke papan klip.
 *
 * Teks kosong diabaikan tanpa umpan balik: menyalin "tidak ada apa-apa" lalu
 * mengabarkan keberhasilan hanya menyesatkan. Mengembalikan true bila ada yang
 * benar-benar disalin, sehingga pemanggil bisa menyesuaikan tampilannya.
 */
fun Context.copyToClipboard(
    text: String,
    confirmation: String = "Teks disalin",
): Boolean {
    if (text.isBlank()) return false
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, text))

    // Android 13 ke atas sudah memunculkan konfirmasi penyalinannya sendiri.
    // Menambahkan Toast di situ membuat pengguna melihat dua pemberitahuan
    // untuk satu tindakan, dan pembaca layar membacakan keduanya.
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(this, confirmation, Toast.LENGTH_SHORT).show()
    }
    return true
}

/**
 * Bagikan [text] lewat pemilih aplikasi bawaan sistem.
 *
 * Mengembalikan true bila pemilih berhasil dibuka.
 */
fun Context.shareText(
    text: String,
    chooserTitle: String = "Bagikan",
): Boolean {
    if (text.isBlank()) return false
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return try {
        startActivity(Intent.createChooser(intent, chooserTitle))
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "Tidak ada aplikasi untuk membagikan teks", Toast.LENGTH_SHORT)
            .show()
        false
    }
}
