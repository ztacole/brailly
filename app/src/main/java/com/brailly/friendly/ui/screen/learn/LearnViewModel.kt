package com.brailly.friendly.ui.screen.learn

import androidx.lifecycle.ViewModel
import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AlphabetEntry(val letter: Char, val cell: BrailleCell)

/** Satu soal kuis: tampilkan pola braille, pilih huruf yang benar. */
data class QuizQuestion(
    val cell: BrailleCell,
    val correct: Char,
    val options: List<Char>,
)

data class LearnUiState(
    val query: String = "",
    val alphabet: List<AlphabetEntry> = emptyList(),
    val quiz: QuizQuestion? = null,
    val score: Int = 0,
    val answered: Int = 0,
    val lastCorrect: Boolean? = null,
)

class LearnViewModel : ViewModel() {

    private val all: List<AlphabetEntry> =
        ('a'..'z').map { AlphabetEntry(it, BrailleMap.letters[it]!!) }

    private val _state = MutableStateFlow(
        LearnUiState(alphabet = all, quiz = generateQuestion())
    )
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    fun onQueryChange(q: String) {
        val filtered = if (q.isBlank()) all
        else all.filter { it.letter.toString().contains(q.trim().lowercase()) }
        _state.value = _state.value.copy(query = q, alphabet = filtered)
    }

    fun answer(choice: Char) {
        val quiz = _state.value.quiz ?: return
        val correct = choice == quiz.correct
        _state.value = _state.value.copy(
            score = _state.value.score + if (correct) 1 else 0,
            answered = _state.value.answered + 1,
            lastCorrect = correct,
        )
    }

    fun nextQuestion() {
        _state.value = _state.value.copy(quiz = generateQuestion(), lastCorrect = null)
    }

    private fun generateQuestion(): QuizQuestion {
        val correct = ('a'..'z').random()
        val distractors = ('a'..'z').filter { it != correct }.shuffled().take(3)
        val options = (distractors + correct).shuffled()
        return QuizQuestion(BrailleMap.letters[correct]!!, correct, options)
    }
}
