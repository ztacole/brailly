package com.brailly.friendly.ui.screen.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleMap
import com.brailly.friendly.core.braille.BrailleToken
import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.core.learn.VideoLesson
import com.brailly.friendly.core.learn.VideoLessonCatalog
import com.brailly.friendly.data.learn.LearnProgressRepository
import com.brailly.friendly.data.learn.LevelResultEntity
import com.brailly.friendly.data.learn.VideoProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ------------------------------------------------------------------- kamus
data class AlphabetEntry(val letter: Char, val cell: BrailleCell)

/** Tab kamus pada menu Belajar. */
enum class DictionaryTab(val label: String) {
    ALPHABET("Alfabet"),
    NUMBER("Angka"),
    SYMBOL("Simbol"),
}

// ------------------------------------------------------------- kuis pathway
/** Arah soal: tebak braille dari teks, atau tebak teks dari braille. */
enum class QuizDirection { TEXT_TO_BRAILLE, BRAILLE_TO_TEXT }

/** Bentuk jawaban yang diminta. */
enum class QuizFormat { MULTIPLE_CHOICE, FILL_BLANK }

/**
 * Satu soal kuis pathway.
 *
 * @param answerText jawaban teks yang benar (huruf/kata/kalimat).
 * @param tokens representasi braille dari [answerText] (dipakai saat menampilkan
 *   sel braille, baik sebagai soal maupun sebagai pilihan jawaban).
 * @param options pilihan untuk soal pilihan ganda (teks). Kosong untuk isian.
 */
data class QuizQuestion(
    val direction: QuizDirection,
    val format: QuizFormat,
    val answerText: String,
    val tokens: List<BrailleToken>,
    val options: List<String> = emptyList(),
) {
    val isBrailleShown: Boolean get() = direction == QuizDirection.BRAILLE_TO_TEXT
}

/**
 * Status satu level pada pathway.
 *
 * @param blockedByVideo true bila level sebelumnya sudah lulus tetapi materi
 *   video unitnya belum tuntas ditonton. Dipakai UI untuk membedakan "belum
 *   sampai di sini" dari "tonton materinya dulu".
 */
data class LevelStatus(
    val index: Int,          // 0-based
    val number: Int,         // 1-based untuk tampilan
    val title: String,
    val unlocked: Boolean,
    val completed: Boolean,
    val bestScore: Int,      // skor terbaik (0..questionsPerLevel)
    val blockedByVideo: Boolean = false,
)

/**
 * Satu node pada Jalur Kuis. Pathway terdiri atas dua jenis node yang
 * berselang-seling: materi video pembuka unit, lalu level-level kuis unit itu.
 */
sealed interface PathStep {
    val unlocked: Boolean
    val completed: Boolean

    data class Video(
        val lesson: VideoLesson,
        override val unlocked: Boolean,
        override val completed: Boolean,
    ) : PathStep

    data class Quiz(val level: LevelStatus) : PathStep {
        override val unlocked: Boolean get() = level.unlocked
        override val completed: Boolean get() = level.completed
    }
}

/** Sesi bermain satu level. */
data class QuizSession(
    val levelIndex: Int,
    val levelNumber: Int,
    val questions: List<QuizQuestion>,
    val current: Int = 0,
    val score: Int = 0,
    val answered: Int = 0,
    val lastCorrect: Boolean? = null,
    val lastAnswer: String = "",
    val finished: Boolean = false,
) {
    val question: QuizQuestion get() = questions[current]
    val total: Int get() = questions.size
}

/**
 * Sesi menonton satu materi video.
 *
 * [watchedSeconds] adalah akumulasi detik yang benar-benar diputar, sehingga
 * nilainya tidak melonjak saat pengguna menggeser bilah waktu.
 */
data class VideoPlaybackState(
    val lesson: VideoLesson,
    val watchedSeconds: Float,
    val durationSeconds: Float,
    val completed: Boolean,
) {
    /** Bagian video yang sudah tertonton, 0f..1f. */
    val watchedRatio: Float
        get() = if (durationSeconds <= 0f) 0f
        else (watchedSeconds / durationSeconds).coerceIn(0f, 1f)

    /** Kemajuan menuju terbukanya gerbang, 0f..1f. */
    val gateProgress: Float
        get() = if (completed) 1f
        else (watchedRatio / LearnViewModel.VIDEO_PASS_RATIO).coerceIn(0f, 1f)

    /** True bila level unit ini sudah boleh dibuka. */
    val requirementMet: Boolean
        get() = completed || (durationSeconds > 0f && watchedRatio >= LearnViewModel.VIDEO_PASS_RATIO)

    /** Sisa detik tontonan sebelum gerbang terbuka. */
    val remainingSeconds: Int
        get() = if (requirementMet || durationSeconds <= 0f) 0
        else (durationSeconds * LearnViewModel.VIDEO_PASS_RATIO - watchedSeconds)
            .coerceAtLeast(0f)
            .toInt()
}

data class LearnUiState(
    val query: String = "",
    val tab: DictionaryTab = DictionaryTab.ALPHABET,
    val alphabet: List<AlphabetEntry> = emptyList(),
    val numbers: List<BrailleMap.DictionaryEntry> = emptyList(),
    val symbols: List<BrailleMap.DictionaryEntry> = emptyList(),
    val levels: List<LevelStatus> = emptyList(),
    val steps: List<PathStep> = emptyList(),
    val session: QuizSession? = null,
    val video: VideoPlaybackState? = null,
)

class LearnViewModel(
    private val repository: LearnProgressRepository,
    private val translator: BrailleTranslator = BrailleTranslator(),
) : ViewModel() {

    private val allAlphabet: List<AlphabetEntry> =
        ('a'..'z').map { AlphabetEntry(it, BrailleMap.letters[it]!!) }

    // Progres terpersisten dari Room, diindeks per level.
    private val completed = BooleanArray(LEVEL_COUNT)
    private val bestScores = IntArray(LEVEL_COUNT)

    // Progres menonton materi video, dikunci oleh videoId.
    private val videoWatchedSeconds = mutableMapOf<String, Int>()
    private val videoDurationSeconds = mutableMapOf<String, Int>()
    private val videoCompleted = mutableSetOf<String>()

    // Posisi tick terakhir dari pemutar, dipakai menghitung delta tontonan.
    private var lastTickSecond: Float = NO_TICK
    private var unsavedSeconds: Float = 0f

    private val _state = MutableStateFlow(
        buildLevels().let { levels ->
            LearnUiState(
                alphabet = allAlphabet,
                numbers = BrailleMap.numberDictionary,
                symbols = BrailleMap.symbolDictionary + BrailleMap.indicatorDictionary,
                levels = levels,
                steps = buildSteps(levels),
            )
        }
    )
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    init {
        // Amati hasil tersimpan: perbarui status level setiap kali Room berubah.
        viewModelScope.launch {
            repository.observeResults().collect { results ->
                applyResults(results)
            }
        }
        // Amati progres menonton materi video.
        viewModelScope.launch {
            repository.observeVideoProgress().collect { progress ->
                applyVideoProgress(progress)
            }
        }
    }

    private fun applyResults(results: List<LevelResultEntity>) {
        completed.fill(false)
        bestScores.fill(0)
        results.forEach { r ->
            if (r.levelIndex in 0 until LEVEL_COUNT) {
                completed[r.levelIndex] = r.passed
                bestScores[r.levelIndex] = r.bestScore
            }
        }
        // Pertahankan sesi aktif (bila ada) sambil menyegarkan daftar node.
        refreshPath()
    }

    private fun applyVideoProgress(progress: List<VideoProgressEntity>) {
        videoWatchedSeconds.clear()
        videoDurationSeconds.clear()
        videoCompleted.clear()
        progress.forEach { p ->
            videoWatchedSeconds[p.videoId] = p.watchedSeconds
            if (p.durationSeconds > 0) videoDurationSeconds[p.videoId] = p.durationSeconds
            if (p.completed) videoCompleted += p.videoId
        }
        refreshPath()
    }

    // --------------------------------------------------------------- kamus
    fun onTabChange(tab: DictionaryTab) {
        _state.value = _state.value.copy(tab = tab, query = "", alphabet = allAlphabet)
    }

    fun onQueryChange(q: String) {
        val filtered = if (q.isBlank()) allAlphabet
        else allAlphabet.filter { it.letter.toString().contains(q.trim().lowercase()) }
        _state.value = _state.value.copy(query = q, alphabet = filtered)
    }

    // ------------------------------------------------------------ pathway
    /** Susun ulang daftar level dan node pathway dari cache progres terkini. */
    private fun refreshPath() {
        val levels = buildLevels()
        _state.value = _state.value.copy(levels = levels, steps = buildSteps(levels))
    }

    private fun isVideoWatched(lesson: VideoLesson): Boolean = lesson.videoId in videoCompleted

    /** Materi terbuka begitu level terakhir unit sebelumnya lulus. */
    private fun isVideoUnlocked(lesson: VideoLesson): Boolean =
        lesson.firstLevelIndex == 0 || completed[lesson.firstLevelIndex - 1]

    private fun buildLevels(): List<LevelStatus> =
        (0 until LEVEL_COUNT).map { i ->
            // Level terbuka bila rantai level sudah sampai di sini DAN materi
            // video unitnya sudah tuntas ditonton.
            val chainOpen = i == 0 || completed[i - 1]
            val videoWatched = isVideoWatched(VideoLessonCatalog.lessonForLevel(i))
            LevelStatus(
                index = i,
                number = i + 1,
                title = LEVEL_TITLES[i],
                unlocked = chainOpen && videoWatched,
                completed = completed[i],
                bestScore = bestScores[i],
                blockedByVideo = chainOpen && !videoWatched,
            )
        }

    /** Sisipkan node materi tepat sebelum level pertama tiap unit. */
    private fun buildSteps(levels: List<LevelStatus>): List<PathStep> = buildList {
        levels.forEach { level ->
            VideoLessonCatalog.lessonStartingAt(level.index)?.let { lesson ->
                add(
                    PathStep.Video(
                        lesson = lesson,
                        unlocked = isVideoUnlocked(lesson),
                        completed = isVideoWatched(lesson),
                    )
                )
            }
            add(PathStep.Quiz(level))
        }
    }

    fun startLevel(index: Int) {
        val level = _state.value.levels.getOrNull(index) ?: return
        if (!level.unlocked) return
        _state.value = _state.value.copy(
            session = QuizSession(
                levelIndex = index,
                levelNumber = index + 1,
                questions = generateQuestions(index),
            )
        )
    }

    fun exitQuiz() {
        _state.value = _state.value.copy(session = null)
        refreshPath()
    }

    /** Jawab soal pilihan ganda / isian. */
    fun answer(input: String) {
        val session = _state.value.session ?: return
        if (session.lastCorrect != null || session.finished) return
        val correct = normalize(input) == normalize(session.question.answerText)
        _state.value = _state.value.copy(
            session = session.copy(
                score = session.score + if (correct) 1 else 0,
                answered = session.answered + 1,
                lastCorrect = correct,
                lastAnswer = input,
            )
        )
    }

    /** Lanjut ke soal berikutnya, atau selesaikan level. */
    fun nextQuestion() {
        val session = _state.value.session ?: return
        if (session.lastCorrect == null) return
        if (session.current + 1 >= session.total) {
            // Level selesai: tandai lulus bila skor >= ambang dan simpan ke Room.
            val passed = session.score >= PASS_THRESHOLD
            // Perbarui cache lokal agar UI segera konsisten walau Flow menyusul.
            if (passed) completed[session.levelIndex] = true
            bestScores[session.levelIndex] =
                maxOf(bestScores[session.levelIndex], session.score)
            persistResult(session, passed)
            _state.value = _state.value.copy(session = session.copy(finished = true))
            refreshPath()
        } else {
            _state.value = _state.value.copy(
                session = session.copy(
                    current = session.current + 1,
                    lastCorrect = null,
                    lastAnswer = "",
                )
            )
        }
    }

    private fun persistResult(session: QuizSession, passed: Boolean) {
        viewModelScope.launch {
            repository.recordResult(
                levelIndex = session.levelIndex,
                score = session.score,
                totalQuestions = session.total,
                passed = passed,
            )
        }
    }

    // ------------------------------------------------------ materi video
    /** Buka layar materi video pada urutan [lessonIndex]. */
    fun openVideo(lessonIndex: Int) {
        val lesson = VideoLessonCatalog.byIndex(lessonIndex) ?: return
        if (!isVideoUnlocked(lesson)) return
        lastTickSecond = NO_TICK
        unsavedSeconds = 0f
        _state.value = _state.value.copy(
            video = VideoPlaybackState(
                lesson = lesson,
                watchedSeconds = (videoWatchedSeconds[lesson.videoId] ?: 0).toFloat(),
                durationSeconds = (videoDurationSeconds[lesson.videoId] ?: 0).toFloat(),
                completed = isVideoWatched(lesson),
            )
        )
    }

    /** Tutup layar materi, menyimpan progres tontonan yang belum tercatat. */
    fun closeVideo() {
        if (_state.value.video != null) persistVideoProgress()
        lastTickSecond = NO_TICK
        unsavedSeconds = 0f
        _state.value = _state.value.copy(video = null)
    }

    /**
     * Tutup materi lalu langsung mulai level pertama unitnya. Bila level itu
     * sudah pernah lulus (pengguna sekadar menonton ulang materi), cukup
     * kembali ke pathway tanpa memulai apa pun.
     */
    fun continueFromVideo() {
        val target = _state.value.video?.lesson?.firstLevelIndex ?: return
        closeVideo()
        val level = _state.value.levels.getOrNull(target) ?: return
        if (!level.completed) startLevel(target)
    }

    /** Durasi video dilaporkan pemutar setelah video siap. */
    fun onVideoDuration(duration: Float) {
        val video = _state.value.video ?: return
        if (duration <= 0f || duration == video.durationSeconds) return
        _state.value = _state.value.copy(
            video = video.copy(
                durationSeconds = duration,
                // Progres lama bisa melebihi durasi bila video diganti.
                watchedSeconds = video.watchedSeconds.coerceAtMost(duration),
            )
        )
    }

    /**
     * Posisi pemutar bergerak ke [second].
     *
     * Hanya selisih maju yang wajar yang ditambahkan ke durasi tertonton.
     * Selisih negatif berarti pengguna mundur, dan selisih yang terlalu besar
     * berarti lompatan seek; keduanya diabaikan supaya menggeser bilah waktu
     * ke akhir video tidak membuka gerbang. Ketentuan YouTube melarang
     * menutupi kontrol pemutar, jadi pembatasan ini memang harus dilakukan
     * dari sisi penghitungan, bukan dengan memblokir bilah waktu.
     */
    fun onVideoProgress(second: Float) {
        val video = _state.value.video ?: return
        val previous = lastTickSecond
        lastTickSecond = second
        if (previous == NO_TICK) return
        val delta = second - previous
        if (delta <= 0f || delta > MAX_TICK_DELTA_SECONDS) return

        val duration = video.durationSeconds
        val watched = (video.watchedSeconds + delta)
            .let { if (duration > 0f) it.coerceAtMost(duration) else it }
        val updated = video.copy(watchedSeconds = watched)
        val justMet = !video.completed && updated.requirementMet
        _state.value = _state.value.copy(
            video = if (justMet) updated.copy(completed = true) else updated
        )

        unsavedSeconds += delta
        if (justMet || unsavedSeconds >= SAVE_INTERVAL_SECONDS) {
            unsavedSeconds = 0f
            persistVideoProgress()
        }
    }

    /**
     * Pemutar melaporkan video habis. Status tuntas tidak diberikan di sini,
     * karena pemutar juga melaporkan ENDED setelah pengguna menggeser bilah
     * waktu ke ujung video. Yang menentukan tetap akumulasi durasi tertonton.
     */
    fun onVideoEnded() {
        lastTickSecond = NO_TICK
        if (_state.value.video != null) persistVideoProgress()
    }

    /**
     * Tandai materi tuntas tanpa menonton. Hanya dipakai tombol pintas di build
     * debug supaya level bisa diuji tanpa menonton video penuh setiap kali.
     */
    fun forceCompleteVideo() {
        val video = _state.value.video ?: return
        _state.value = _state.value.copy(video = video.copy(completed = true))
        persistVideoProgress()
    }

    private fun persistVideoProgress() {
        val video = _state.value.video ?: return
        val videoId = video.lesson.videoId
        val watched = video.watchedSeconds.toInt()
        val duration = video.durationSeconds.toInt()
        val isCompleted = video.completed

        // Perbarui cache lokal lebih dulu agar node pathway langsung konsisten
        // walau emisi Flow dari Room menyusul.
        videoWatchedSeconds[videoId] = maxOf(videoWatchedSeconds[videoId] ?: 0, watched)
        if (duration > 0) videoDurationSeconds[videoId] = duration
        if (isCompleted) videoCompleted += videoId
        refreshPath()

        viewModelScope.launch {
            repository.recordVideoProgress(
                videoId = videoId,
                watchedSeconds = watched,
                durationSeconds = duration,
                completed = isCompleted,
            )
        }
    }

    private fun normalize(s: String): String =
        s.trim().lowercase().replace(Regex("\\s+"), " ")

    // -------------------------------------------------- pembuatan soal
    /**
     * Buat 5 soal untuk sebuah level. Tingkat kesulitan naik seiring level:
     * level awal berupa huruf tunggal, level menengah kata, level akhir kalimat.
     * Setiap soal diacak arah (text<->braille) dan formatnya (pilihan/isian).
     */
    private fun generateQuestions(levelIndex: Int): List<QuizQuestion> {
        val pool = contentPool(levelIndex)
        return (0 until QUESTIONS_PER_LEVEL).map {
            val answer = pool.random()
            val direction = QuizDirection.entries.random()
            // Isian lebih mudah untuk huruf tunggal; kalimat panjang pakai pilihan ganda.
            val format = when {
                answer.length > 12 -> QuizFormat.MULTIPLE_CHOICE
                else -> QuizFormat.entries.random()
            }
            val options = if (format == QuizFormat.MULTIPLE_CHOICE) {
                buildOptions(answer, pool)
            } else emptyList()
            QuizQuestion(
                direction = direction,
                format = format,
                answerText = answer,
                tokens = translator.textToBraille(answer),
                options = options,
            )
        }
    }

    private fun buildOptions(answer: String, pool: List<String>): List<String> {
        val distractors = pool.filter { normalize(it) != normalize(answer) }
            .distinctBy { normalize(it) }
            .shuffled()
            .take(3)
        return (distractors + answer).shuffled()
    }

    /** Kumpulan materi soal per level, dari mudah (huruf) ke sulit (kalimat). */
    private fun contentPool(levelIndex: Int): List<String> = when (levelIndex) {
        0 -> listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
        1 -> listOf("k", "l", "m", "n", "o", "p", "q", "r", "s", "t")
        2 -> listOf("u", "v", "w", "x", "y", "z", "a", "e", "i", "o")
        3 -> listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        4 -> listOf("ibu", "api", "roti", "mata", "buku", "nasi", "kaki", "susu")
        5 -> listOf("rumah", "jalan", "pagi", "malam", "hujan", "angin", "pintu", "meja")
        6 -> listOf("sekolah", "belajar", "membaca", "menulis", "bahagia", "keluarga")
        7 -> listOf("saya makan", "dia pergi", "kita main", "ayah kerja", "ibu masak")
        8 -> listOf("saya suka membaca", "hari ini cerah", "kami pergi ke pasar")
        9 -> listOf("belajar braille itu seru", "ayo membaca setiap hari", "braille membuka dunia")
        else -> listOf("a", "b", "c")
    }

    companion object {
        const val LEVEL_COUNT = 10
        const val QUESTIONS_PER_LEVEL = 5

        /** Minimal benar untuk membuka level berikutnya. */
        const val PASS_THRESHOLD = 3

        /** Bagian video yang harus tertonton agar gerbang unit terbuka. */
        const val VIDEO_PASS_RATIO = 0.9f

        /**
         * Batas selisih antar laporan posisi pemutar yang masih dihitung sebagai
         * tontonan. Nilainya longgar supaya buffering atau pemutaran dipercepat
         * tidak ikut terpotong, tetapi jauh lebih kecil daripada lompatan seek.
         */
        private const val MAX_TICK_DELTA_SECONDS = 3f

        /** Simpan ke Room setiap akumulasi tontonan sebanyak ini. */
        private const val SAVE_INTERVAL_SECONDS = 5f

        /** Penanda belum ada laporan posisi dari pemutar. */
        private const val NO_TICK = -1f

        private val LEVEL_TITLES = listOf(
            "Huruf A–J",
            "Huruf K–T",
            "Huruf U–Z",
            "Angka 0–9",
            "Kata Pendek",
            "Kata Sehari-hari",
            "Kata Panjang",
            "Frasa",
            "Kalimat Sederhana",
            "Kalimat Lengkap",
        )
    }
}
