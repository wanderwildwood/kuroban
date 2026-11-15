package com.wanderwildwood.kuroban.game

import org.junit.Assert.assertEquals
import org.junit.Test

/** The one sentence a player reads at the end of a game, so what it says had better be right. */
class ResultTest {

    @Test
    fun `a scored win is read out the way a person would say it`() {
        assertEquals(Outcome.WinBy(Stone.BLACK, "7.5"), outcome("B+7.5"))
        assertEquals(Outcome.WinBy(Stone.WHITE, "12.5"), outcome("W+12.5"))
        assertEquals(Outcome.WinBy(Stone.BLACK, "0.5"), outcome(" b+0.5 "))
    }

    @Test
    fun `a resignation names the other player as the winner`() {
        assertEquals(Outcome.Resignation(Stone.WHITE), outcome("", resignedBy = Stone.BLACK))
        assertEquals(Outcome.Resignation(Stone.BLACK), outcome("", resignedBy = Stone.WHITE))
    }

    @Test
    fun `a resignation ignores whatever the score happened to be`() {
        assertEquals(Outcome.Resignation(Stone.WHITE), outcome("B+40.5", resignedBy = Stone.BLACK))
    }

    @Test
    fun `a drawn game says so`() {
        assertEquals(Outcome.Draw, outcome("0"))
        assertEquals(Outcome.Draw, outcome(""))
    }

    @Test
    fun `a resignation written as a score reads as one`() {
        assertEquals(Outcome.Resignation(Stone.BLACK), outcome("B+R"))
        assertEquals(Outcome.Resignation(Stone.WHITE), outcome("W+r"))
    }

    @Test
    fun `anything unrecognised is passed through rather than invented`() {
        assertEquals(Outcome.Unrecognised("?"), outcome("?"))
        assertEquals(Outcome.Unrecognised("unfinished"), outcome("unfinished"))
    }

    @Test
    fun `two passes on an empty board give White the komi and nothing else`() {
        // What GNU Go answers for an empty 9x9 board with a komi of 6.5.
        val score = countedScore("B+17.5", black = emptySet(), white = emptySet(), komi = KOMI)
        assertEquals("W+6.5", score)
        assertEquals(Outcome.WinBy(Stone.WHITE, "6.5"), outcome(score))
    }

    @Test
    fun `an empty board without a komi is a draw`() {
        assertEquals(Outcome.Draw, outcome(countedScore("B+17.5", emptySet(), emptySet(), komi = 0.0)))
    }

    @Test
    fun `a board with a stone on it keeps the engine's count`() {
        val one = setOf(Point(4, 4))
        assertEquals("B+74.5", countedScore("B+74.5", black = one, white = emptySet(), komi = KOMI))
        assertEquals("W+3.5", countedScore("W+3.5", black = one, white = setOf(Point(2, 2)), komi = KOMI))
        assertEquals("W+80.5", countedScore("W+80.5", black = emptySet(), white = one, komi = HANDICAP_KOMI))
    }
}
