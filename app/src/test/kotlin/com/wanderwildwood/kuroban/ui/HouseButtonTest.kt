package com.wanderwildwood.kuroban.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseButtonTest {

    @Test
    fun touchRuleIsFortyEightDp() {
        assertEquals(48.dp, HOUSE_MIN)
    }

    @Test
    fun compactIsNarrowerThanPlain() {
        assertEquals(14.dp, houseSide(false))
        assertEquals(6.dp, houseSide(true))
        assertTrue(houseSide(true) < houseSide(false))
    }

    @Test
    fun dialogGapMatchesTheFolioDialog() {
        assertEquals(10.dp, DIALOG_GAP)
    }

    @Test
    fun toggleAddsAMissingItem() {
        assertEquals(setOf(1, 3), toggled(setOf(1), 3))
    }

    @Test
    fun toggleRemovesAPresentItem() {
        assertEquals(setOf(1), toggled(setOf(1, 3), 3))
    }

    @Test
    fun toggleTwiceIsNoChange() {
        val start = setOf(0, 2, 5)
        assertEquals(start, toggled(toggled(start, 4), 4))
    }

    @Test
    fun toggleLeavesTheOriginalAlone() {
        val start = setOf(2)
        toggled(start, 3)
        assertEquals(setOf(2), start)
    }

    @Test
    fun choiceInRangeIsKept() {
        assertEquals(0, validChoice(0, 3))
        assertEquals(2, validChoice(2, 3))
    }

    @Test
    fun choiceOutOfRangeIsNothing() {
        assertEquals(-1, validChoice(-1, 3))
        assertEquals(-1, validChoice(3, 3))
        assertEquals(-1, validChoice(0, 0))
        assertFalse(validChoice(7, 2) >= 0)
    }
}
