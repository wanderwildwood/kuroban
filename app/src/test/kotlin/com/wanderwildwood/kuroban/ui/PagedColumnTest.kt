package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedColumnTest {
    private fun page(height: Int, count: Int, vararg rows: Triple<Int, Int, Int>) =
        PageView(rows.map { RowSpan(it.first, it.second, it.third) }, height, count)

    @Test
    fun aRowCutAtTheFootOpensTheNextPageAndIsNotDrawn() {
        // Today as it was cut: the strip, the air, the sun, and the credits running off the foot.
        val p = page(700, 8, Triple(4, 0, 220), Triple(5, 220, 360), Triple(6, 360, 520), Triple(7, 520, 760))
        assertEquals(Turn.To(7), forward(p))
        assertEquals(520, p.foot)
        assertFalse(p.atEnd)
        assertTrue(p.pages)
    }

    @Test
    fun theLastPageStartsOnItsRowAndTurnsNoFurther() {
        val p = page(700, 8, Triple(7, 0, 240))
        assertEquals(Turn.None, forward(p))
        assertNull(p.foot)
        assertTrue(p.atEnd)
    }

    @Test
    fun aPageThatFitsExactlyTurnsToTheRowAfterIt() {
        val p = page(400, 10, Triple(0, 0, 100), Triple(1, 100, 200), Triple(2, 200, 300), Triple(3, 300, 400))
        assertEquals(Turn.To(4), forward(p))
        assertNull(p.foot)
    }

    @Test
    fun aListThatFitsDoesNotPage() {
        val p = page(700, 3, Triple(0, 0, 100), Triple(1, 100, 200), Triple(2, 200, 300))
        assertFalse(p.pages)
        assertEquals(Turn.None, forward(p))
        assertTrue(p.atStart)
    }

    @Test
    fun aRowTallerThanThePageIsReadAPageAtATime() {
        val p = page(700, 3, Triple(1, 0, 1600))
        assertEquals(Turn.By(700), forward(p))
        assertNull(p.foot)
        val near = page(700, 3, Triple(1, -1400, 200), Triple(2, 200, 900))
        assertEquals(Turn.To(2), forward(near))
    }

    @Test
    fun aTurnBackLandsOnARow() {
        assertEquals(3, backSnap(page(700, 9, Triple(2, -60, 120), Triple(3, 120, 300))))
        assertNull(backSnap(page(700, 9, Triple(0, 0, 120), Triple(1, 120, 300))))
        assertNull(backSnap(page(700, 9, Triple(2, -900, 100), Triple(3, 100, 300))))
    }
}
