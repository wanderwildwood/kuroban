package com.wanderwildwood.kuroban.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NewGameChoicesTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the last choices come back`() {
        val choices = NewGameChoices(folder.newFile())
        val picked = GameConfig(
            opponent = Opponent.HUMAN,
            difficulty = Difficulty.EASY,
            humanColor = Stone.WHITE,
            handicap = 3,
        )
        choices.save(picked)
        assertEquals(picked, choices.load())
    }

    @Test
    fun `nothing saved yet opens on the defaults`() {
        assertEquals(GameConfig(), NewGameChoices(File(folder.root, "absent.txt")).load())
    }

    @Test
    fun `a file that is not choices is ignored`() {
        assertNull(parseChoices(""))
        assertNull(parseChoices("HUMAN EASY WHITE"))
        assertNull(parseChoices("HUMAN EASY WHITE 1"))
        assertNull(parseChoices("ROBOT EASY WHITE 0"))
        val file = folder.newFile().apply { writeText("garbage") }
        assertEquals(GameConfig(), NewGameChoices(file).load())
    }

    @Test
    fun `handicap stones go where GNU Go puts them`() {
        assertEquals(emptyList<Point>(), handicapPoints(0))
        assertEquals(listOf(Point(2, 6), Point(6, 2)), handicapPoints(2))
        assertEquals(listOf(Point(2, 6), Point(6, 2), Point(2, 2)), handicapPoints(3))
        assertEquals(4, handicapPoints(4).toSet().size)
        assertEquals(Point(4, 4), handicapPoints(5).last())
        assertEquals(5, handicapPoints(5).toSet().size)
    }
}
