package com.wanderwildwood.kuroban.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoTest {
    private val vsComputer = GameConfig(opponent = Opponent.COMPUTER, humanColor = Stone.BLACK)

    @Test
    fun takingBackOnYourTurnTakesTheReplyToo() {
        val s = GameState(vsComputer, phase = Phase.PLAYING, toMove = Stone.BLACK, movesPlayed = 4)
        assertEquals(2, s.movesPerUndo)
    }

    @Test
    fun aGameEndedOnYourMoveTakesBackOnlyThatMove() {
        // You passed after its pass, or it resigned in reply: the computer is to move.
        val s = GameState(vsComputer, phase = Phase.FINISHED, toMove = Stone.WHITE, movesPlayed = 5)
        assertEquals(1, s.movesPerUndo)
        assertTrue(s.canUndo)
    }

    @Test
    fun twoPlayersTakeBackOneAtATime() {
        val s = GameState(GameConfig(opponent = Opponent.HUMAN), phase = Phase.PLAYING, toMove = Stone.WHITE, movesPlayed = 3)
        assertEquals(1, s.movesPerUndo)
    }
}
