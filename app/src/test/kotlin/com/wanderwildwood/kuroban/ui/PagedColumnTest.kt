package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedColumnTest {
    private fun page(height: Int, count: Int, vararg rows: Triple<Int, Int, Int>) =
        PageView(rows.map { RowSpan(it.first, it.second, it.third) }, height, count)

    /** The same page in a list of tall rows that shows the row cut at the foot. */
    private fun cutPage(height: Int, count: Int, vararg rows: Triple<Int, Int, Int>) =
        PageView(rows.map { RowSpan(it.first, it.second, it.third) }, height, count, showsCut = true)

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
    fun aTallFirstRowBelowTopPaddingIsStillReadAPageAtATime() {
        // Padding above the list puts the first row's top at 36, not 0.
        assertEquals(Turn.By(700), forward(page(700, 2, Triple(0, 36, 1636))))
        // A short row above a tall one: the tall one is cut, not first, so the page turns to it.
        assertEquals(Turn.To(1), forward(page(700, 3, Triple(0, 36, 200), Triple(1, 200, 1900))))
    }

    @Test
    fun aTurnBackLandsOnARow() {
        assertEquals(3, backSnap(page(700, 9, Triple(2, -60, 120), Triple(3, 120, 300))))
        assertNull(backSnap(page(700, 9, Triple(0, 0, 120), Triple(1, 120, 300))))
        assertNull(backSnap(page(700, 9, Triple(2, -900, 100), Triple(3, 100, 300))))
    }

    @Test
    fun theDefaultPaintsOutTheCutRow() {
        val p = page(700, 20, Triple(0, 0, 240), Triple(1, 240, 480), Triple(2, 480, 720))
        assertFalse(p.showsCut)
        assertEquals(480, p.foot)
    }

    @Test
    fun aCutRowShownAtTheFootIsDrawnAndOpensTheNextPage() {
        // An inbox: three messages whole, the fourth cut through by the foot.
        val p = cutPage(700, 20, Triple(0, 0, 200), Triple(1, 200, 400), Triple(2, 400, 600), Triple(3, 600, 800))
        assertNull(p.foot)
        assertEquals(Turn.To(3), forward(p))
        assertFalse(p.atEnd)
        assertTrue(p.pages)
        assertEquals(listOf(0, 1, 2), p.whole.map { it.index })
    }

    @Test
    fun aCutRowShownTurnsAPageOnAndSkipsNothing() {
        // Each turn opens on the row the last page cut, so every row is whole on some page.
        val heights = listOf(180, 260, 200, 150, 300, 220, 240, 190)
        val tops = heights.runningFold(0) { a, h -> a + h }
        val height = 700
        var start = 0
        val seenWhole = mutableSetOf<Int>()
        repeat(heights.size) {
            if (start >= heights.size) return@repeat
            val rows = (start until heights.size).map { RowSpan(it, tops[it] - tops[start], tops[it + 1] - tops[start]) }
                .filter { it.top < height }
            val p = PageView(rows, height, heights.size, showsCut = true)
            seenWhole += p.whole.map { it.index }
            start = when (val t = forward(p)) {
                is Turn.To -> t.index.also { assertTrue(it > start) }
                else -> heights.size
            }
        }
        assertEquals((heights.indices).toSet(), seenWhole)
    }

    @Test
    fun theLastPageWithTheCutRowShownEndsFlushAndTurnsNoFurther() {
        // No blank tail: the list stops with the last row on the foot, part-way into a row at the top.
        val p = cutPage(700, 8, Triple(5, -100, 140), Triple(6, 140, 460), Triple(7, 460, 700))
        assertNull(p.foot)
        assertTrue(p.atEnd)
        assertEquals(Turn.None, forward(p))
        // A turn back from it still lands on a row.
        assertEquals(3, backSnap(cutPage(700, 8, Triple(2, -80, 100), Triple(3, 100, 400), Triple(4, 400, 760))))
    }

    @Test
    fun aRowTallerThanThePageIsStillReadAPageAtATimeWithTheCutRowShown() {
        val p = cutPage(700, 3, Triple(1, 0, 1600))
        assertEquals(Turn.By(700), forward(p))
        assertNull(p.foot)
    }

    @Test
    fun aListThatGotShorterUnderThePageIsStranded() {
        // Level's Settings: on the page that opened with "Forget calibration", which then went.
        // Only the blank tail is left (no rows), or the row is still there with no height.
        assertTrue(stranded(page(700, 8)))
        assertTrue(stranded(page(700, 8, Triple(7, 0, 0))))
        // Not the first page, nor a page that still shows a row, nor an empty list.
        assertFalse(stranded(page(700, 8, Triple(7, 0, 240))))
        assertFalse(stranded(page(700, 8, Triple(6, -40, 0), Triple(7, 0, 120))))
        assertFalse(stranded(page(700, 8, Triple(0, 0, 0), Triple(1, 0, 0))))
        assertFalse(stranded(page(700, 0)))
        // With the cut row shown the list ends flush by itself.
        assertFalse(stranded(cutPage(700, 8)))
    }

    @Test
    fun theLastRealPageIsNotStrandedAndTurnsNoFurther() {
        // Where the list comes back to: the last row on the foot, a whole row at the top.
        val p = page(700, 8, Triple(4, 0, 160), Triple(5, 160, 400), Triple(6, 400, 420), Triple(7, 420, 700))
        assertFalse(stranded(p))
        assertTrue(p.atEnd)
        assertEquals(Turn.None, forward(p))
        assertTrue(p.pages)
    }
}
