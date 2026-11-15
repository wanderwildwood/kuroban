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
        // GNU Go's final_score answered B+17.5 here.
        val score = countedScore(black = emptySet(), white = emptySet(), komi = KOMI)
        assertEquals("W+6.5", score)
        assertEquals(Outcome.WinBy(Stone.WHITE, "6.5"), outcome(score))
    }

    @Test
    fun `an empty board without a komi is a draw`() {
        assertEquals(Outcome.Draw, outcome(countedScore(emptySet(), emptySet(), komi = 0.0)))
    }

    @Test
    fun `one stone owns the whole empty board`() {
        // Black on the centre point, then two passes: GNU Go also said B+17.5 to this.
        // The other 80 points touch only Black, so 80 - 6.5.
        assertEquals("B+73.5", countedScore(black = setOf(Point(4, 4)), white = emptySet(), komi = KOMI))
        assertEquals("W+86.5", countedScore(black = emptySet(), white = setOf(Point(4, 4)), komi = KOMI))
    }

    @Test
    fun `one stone each side leaves the board to nobody`() {
        // Every empty point is reachable from both stones, so it is one shared region.
        val score = countedScore(black = setOf(Point(2, 2)), white = setOf(Point(6, 6)), komi = KOMI)
        assertEquals("W+6.5", score)
    }

    @Test
    fun `handicap stones alone own the board`() {
        // Three handicap stones on the star points, then two passes: 78 empty points, komi 0.5.
        val handicap = setOf(Point(2, 6), Point(6, 2), Point(6, 6))
        assertEquals("B+77.5", countedScore(black = handicap, white = emptySet(), komi = HANDICAP_KOMI))
    }

    @Test
    fun `a wall splits the board and each side counts its own half`() {
        // Black holds columns 0-2 behind a wall on column 3, White columns 5-8 behind one on 4.
        val black = (0 until BOARD_SIZE).map { Point(3, it) }.toSet()
        val white = (0 until BOARD_SIZE).map { Point(4, it) }.toSet()
        // Black 27, White 36 + 6.5.
        assertEquals("W+15.5", countedScore(black = black, white = white, komi = KOMI))
        // Captures count for whoever made them: 4 for Black, none for White.
        assertEquals("W+11.5", countedScore(black = black, white = white, blackCaptures = 4, komi = KOMI))
    }

    @Test
    fun `a dead stone is a prisoner and its point is territory`() {
        val black = (0 until BOARD_SIZE).map { Point(3, it) }.toSet()
        val white = (0 until BOARD_SIZE).map { Point(4, it) }.toSet() + Point(1, 1)
        // White's stone inside Black's side is dead: Black gets its point (27) and the
        // prisoner (1) = 28; White 36 + 6.5 = 42.5.
        assertEquals("W+14.5", countedScore(black, white, dead = setOf(Point(1, 1)), komi = KOMI))
        // Taken as alive it would split nothing off, but it touches Black's region, so that
        // region is shared: Black 0, White 36 + 6.5.
        assertEquals("W+42.5", countedScore(black, white, komi = KOMI))
    }

    @Test
    fun `points beside a seki belong to nobody`() {
        val black = (0 until BOARD_SIZE).map { Point(3, it) }.toSet()
        val white = (0 until BOARD_SIZE).map { Point(4, it) }.toSet()
        // Pretend Black's wall lives in seki: its side is no longer Black's to count.
        assertEquals("W+42.5", countedScore(black, white, seki = black, komi = KOMI))
    }

    @Test
    fun `an exact count without komi is a draw`() {
        val black = (0 until BOARD_SIZE).map { Point(3, it) }.toSet()
        val white = (0 until BOARD_SIZE).map { Point(5, it) }.toSet()
        // 27 each; column 4 touches both and is nobody's.
        assertEquals(Outcome.Draw, outcome(countedScore(black, white, komi = 0.0)))
    }
}
