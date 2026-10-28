package com.brailly.friendly.core.learn

/**
 * Satu materi video pembelajaran yang menjadi gerbang sebuah unit level.
 *
 * Node video muncul di Jalur Kuis tepat sebelum level pertama unitnya, dan
 * seluruh level pada unit tersebut tetap terkunci sampai videonya selesai
 * ditonton.
 *
 * @param index urutan materi, 0-based.
 * @param videoId ID video YouTube (bagian setelah `youtu.be/`).
 * @param title judul node pada pathway.
 * @param summary keterangan singkat isi materi, ditampilkan di layar video.
 * @param firstLevelIndex `levelIndex` level pertama yang dibuka materi ini.
 *   Nilai ini sekaligus menentukan batas unit: sebuah level masuk unit materi
 *   terakhir yang [firstLevelIndex]-nya tidak melebihi indeks level tersebut.
 */
data class VideoLesson(
    val index: Int,
    val videoId: String,
    val title: String,
    val summary: String,
    val firstLevelIndex: Int,
) {
    /** Tautan publik, dipakai bila video perlu dibuka di aplikasi YouTube. */
    val shareUrl: String get() = "https://youtu.be/$videoId"
}

/**
 * Daftar materi video, dikelompokkan jadi tiga unit: huruf, angka, lalu
 * kata/kalimat. Pemetaan ke level mengikuti `LearnViewModel.LEVEL_TITLES`:
 *
 * ```
 * Materi 1  Huruf           -> level 0, 1, 2  (A–J, K–T, U–Z)
 * Materi 2  Angka           -> level 3        (Angka 0–9)
 * Materi 3  Kata & Kalimat  -> level 4..9     (kata, frasa, kalimat)
 * ```
 *
 * PENTING: ketiga `videoId` di bawah masih placeholder dan merujuk ke kanal
 * pihak ketiga (dua video pertama dari kanal "Visually Impaired Designer",
 * yang ketiga dari "In One Lesson"). Keduanya berbahasa Inggris dan tidak
 * menghasilkan pendapatan bagi kanal Brailly. Ganti `videoId` di sini dengan
 * video sendiri sebelum rilis; tidak ada bagian kode lain yang perlu diubah.
 */
object VideoLessonCatalog {

    val lessons: List<VideoLesson> = listOf(
        VideoLesson(
            index = 0,
            videoId = "RdbDEWqkZd0",
            title = "Pengenalan Huruf",
            summary = "Cara enam titik braille menyusun huruf A sampai Z. " +
                "Tonton sampai habis untuk membuka level huruf.",
            firstLevelIndex = 0,
        ),
        VideoLesson(
            index = 1,
            videoId = "hYhKs8L8_vM",
            title = "Pengenalan Angka",
            summary = "Tanda angka dan cara membaca angka 0 sampai 9 dalam braille.",
            firstLevelIndex = 3,
        ),
        VideoLesson(
            index = 2,
            videoId = "sqQ3gdE7ks0",
            title = "Kata & Kalimat",
            summary = "Menggabungkan sel braille menjadi kata, frasa, dan kalimat utuh.",
            firstLevelIndex = 4,
        ),
    )

    /** Materi yang menjadi gerbang bagi [levelIndex]. */
    fun lessonForLevel(levelIndex: Int): VideoLesson =
        lessons.last { it.firstLevelIndex <= levelIndex }

    /** Materi pada urutan [index], atau null bila di luar rentang. */
    fun byIndex(index: Int): VideoLesson? = lessons.getOrNull(index)

    /** Materi yang node-nya harus digambar sebelum [levelIndex], bila ada. */
    fun lessonStartingAt(levelIndex: Int): VideoLesson? =
        lessons.firstOrNull { it.firstLevelIndex == levelIndex }
}
