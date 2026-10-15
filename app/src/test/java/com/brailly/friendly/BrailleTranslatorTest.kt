package com.brailly.friendly

import com.brailly.friendly.core.braille.BrailleCell
import com.brailly.friendly.core.braille.BrailleMap
import com.brailly.friendly.core.braille.BrailleToken
import com.brailly.friendly.core.braille.BrailleTranslator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailleTranslatorTest {

    private val translator = BrailleTranslator()

    @Test
    fun letterA_mapsToDot1() {
        val tokens = translator.textToBraille("a")
        assertEquals(1, tokens.size)
        val cell = (tokens[0] as BrailleToken.Cell).cell
        assertEquals(setOf(1), cell.dots)
    }

    @Test
    fun unicode_forC_isBrailleC() {
        // 'c' = dots 1,4 -> Unicode U+2809 (⠉)
        assertEquals("\u2809", translator.textToUnicode("c"))
    }

    @Test
    fun capitalLetter_addsCapitalSign() {
        val tokens = translator.textToBraille("A")
        // Token pertama = tanda kapital (dot 6), lalu huruf a
        assertEquals(2, tokens.size)
        assertEquals(BrailleMap.CAPITAL_SIGN.dots, (tokens[0] as BrailleToken.Cell).cell.dots)
        assertEquals(setOf(1), (tokens[1] as BrailleToken.Cell).cell.dots)
    }

    @Test
    fun digits_prefixedWithNumberSign() {
        val tokens = translator.textToBraille("1")
        assertEquals(2, tokens.size)
        assertEquals(BrailleMap.NUMBER_SIGN.dots, (tokens[0] as BrailleToken.Cell).cell.dots)
        // Angka 1 memakai pola huruf 'a' (dot 1)
        assertEquals(setOf(1), (tokens[1] as BrailleToken.Cell).cell.dots)
    }

    @Test
    fun space_producesSpaceToken() {
        val tokens = translator.textToBraille("a b")
        assertTrue(tokens.any { it is BrailleToken.Space })
    }

    @Test
    fun roundTrip_wordHalo() {
        val cells = listOf('h', 'a', 'l', 'o').map { BrailleMap.letters[it]!! }
        assertEquals("halo", translator.brailleToText(cells))
    }

    @Test
    fun brailleToText_decodesCapitalAndNumber() {
        val cells = listOf(
            BrailleMap.CAPITAL_SIGN,
            BrailleMap.letters['b']!!,
            BrailleCell.EMPTY, // spasi
            BrailleMap.NUMBER_SIGN,
            BrailleMap.letters['a']!!, // -> 1
        )
        assertEquals("B 1", translator.brailleToText(cells))
    }
}
