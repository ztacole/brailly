package com.brailly.friendly.ui.screen.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleMap
import com.brailly.friendly.core.braille.BrailleToken
import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.data.learn.LearnProgressRepository
import com.brailly.friendly.data.learn.LevelResultEntity
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

/** Status satu level pada pathway. */
data class LevelStatus(
    val index: Int,          // 0-based
    val number: Int,         // 1-based untuk tampilan
    val title: String,
    val unlocked: Boolean,
    val completed: Boolean,
    val bestScore: Int,      // skor terbaik (0..questionsPerLevel)
)

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

data class LearnUiState(
    val query: String = "",
    val tab: DictionaryTab = DictionaryTab.ALPHABET,
    val alphabet: List<AlphabetEntry> = emptyList(),
    val numbers: List<BrailleMap.DictionaryEntry> = emptyList(),
    val symbols: List<BrailleMap.DictionaryEntry> = emptyList(),
    val levels: List<LevelStatus> = emptyList(),
    val session: QuizSession? = null,
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

    private val _state = MutableStateFlow(
        LearnUiState(
            alphabet = allAlphabet,
            numbers = BrailleMap.numberDictionary,
            symbols = BrailleMap.symbolDictionary + BrailleMap.indicatorDictionary,
            levels = buildLevels(),
        )
    )
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    init {
        // Amati hasil tersimpan: perbarui status level setiap kali Room berubah.
        viewModelScope.launch {
            repository.observeResults().collect { results ->
                applyResults(results)
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
        // Pertahankan sesi aktif (bila ada) sambil menyegarkan daftar level.
        _state.value = _state.value.copy(levels = buildLevels())
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
    private fun buildLevels(): List<LevelStatus> =
        (0 until LEVEL_COUNT).map { i ->
            val unlocked = i == 0 || completed[i - 1]
            LevelStatus(
                index = i,
                number = i + 1,
                title = LEVEL_TITLES[i],
                unlocked = unlocked,
                completed = completed[i],
                bestScore = bestScores[i],
            )
        }

    fun startLevel(index: Int) {
        val unlocked = index == 0 || completed[index - 1]
        if (!unlocked) return
        _state.value = _state.value.copy(
            session = QuizSession(
                levelIndex = index,
                levelNumber = index + 1,
                questions = generateQuestions(index),
            )
        )
    }

    fun exitQuiz() {
        _state.value = _state.value.copy(session = null, levels = buildLevels())
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
            _state.value = _state.value.copy(
                session = session.copy(finished = true),
                levels = buildLevels(),
            )
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
        return (0 until QUESTIONS_PER_LEVEL).map { i ->
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
