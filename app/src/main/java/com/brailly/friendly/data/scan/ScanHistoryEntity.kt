package com.brailly.friendly.data.scan

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Satu hasil pemindaian yang tersimpan di riwayat.
 *
 * Yang disimpan adalah teksnya, bukan fotonya. Gambar halaman jauh lebih besar
 * dan lebih sensitif daripada teks hasilnya, sementara yang dibutuhkan
 * pengguna setelah memindai, yaitu membaca ulang, menyalin, membagikan, dan
 * mendengarkan, semuanya bisa dilakukan dari teks saja.
 *
 * Struktur paragraf sengaja tidak diawetkan. Riwayat menampilkan teks utuh
 * halaman, dan menyimpannya sebagai satu kolom membuat skemanya tetap bisa
 * dimigrasikan otomatis oleh Room.
 *
 * @param text seluruh teks halaman, apa adanya dari pembacaan.
 * @param confidence keyakinan deteksi 0..1 saat pemindaian dilakukan.
 * @param cellCount jumlah sel braille yang terbaca.
 * @param paragraphCount jumlah paragraf yang terbentuk.
 * @param doubtful true bila hasilnya ditandai patut diragukan saat dipindai.
 * @param scannedAt epoch millis waktu pemindaian.
 */
@Entity(
    tableName = "scan_history",
    // Riwayat selalu dibaca terurut dari yang terbaru. Tanpa indeks ini setiap
    // permintaan halaman memaksa SQLite mengurutkan seluruh tabel lebih dulu.
    indices = [Index(value = ["scannedAt"])],
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val confidence: Float,
    val cellCount: Int,
    val paragraphCount: Int,
    val doubtful: Boolean,
    val scannedAt: Long,
)
