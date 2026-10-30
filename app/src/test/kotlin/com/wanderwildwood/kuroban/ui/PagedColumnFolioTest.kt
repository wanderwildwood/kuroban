package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PagedColumnFolioTest {

    @Test
    fun rowsThatFitMakeOnePage() {
        val starts = pageStarts(listOf(100, 100, 100), 400)
        assertEquals(1, starts.size)
    }

    @Test
    fun noRowsIsOnePage() {
        assertEquals(1, pageStarts(emptyList(), 400).size)
        assertEquals(1, pageStarts(listOf(50), 0).size)
    }

    @Test
    fun rowsPerPageFromMeasuredHeights() {
        // 400 px pages and 90 px rows: four whole rows a page.
        val starts = pageStarts(List(10) { 90 }, 400)
        assertEquals(listOf(0, 4, 8), starts.map { it.row })
    }

    @Test
    fun lastPartialPageCounts() {
        // Nine rows at four a page: the ninth is a page of its own.
        assertEquals(3, pageStarts(List(9) { 100 }, 400).size)
        // Exactly eight fill two pages and no more.
        assertEquals(2, pageStarts(List(8) { 100 }, 400).size)
    }

    @Test
    fun unevenRowsEndAPageWhereTheNextDoesNotFit() {
        val starts = pageStarts(listOf(150, 150, 150, 50, 50), 400)
        assertEquals(listOf(0, 2), starts.map { it.row })
    }

    @Test
    fun aRowTallerThanThePageTakesSeveral() {
        val starts = pageStarts(listOf(100, 900, 100), 400)
        // Page 1: row 0; then row 1 in three goes (0, 400, 800 of 900), then row 2 follows the rest.
        assertEquals(listOf(0 to 0, 1 to 0, 1 to 400, 1 to 800), starts.map { it.row to it.into })
    }

    @Test
    fun currentPageFromTheTopRow() {
        val starts = pageStarts(List(10) { 90 }, 400)
        assertEquals(1, pageOf(starts, 0, 0))
        assertEquals(1, pageOf(starts, 3, 0))
        assertEquals(2, pageOf(starts, 4, 0))
        assertEquals(3, pageOf(starts, 9, 0))
        val tall = pageStarts(listOf(900), 400)
        assertEquals(2, pageOf(tall, 0, 450))
    }

    @Test
    fun goToPageClamps() {
        assertEquals(1, clampPage(0, 6))
        assertEquals(4, clampPage(4, 6))
        assertEquals(6, clampPage(99, 6))
        assertEquals(1, clampPage(5, 0))
    }

    @Test
    fun unseenRowsCountAsTheMedian() {
        val h = RowHeights()
        h.put(0, 100)
        h.put(1, 200)
        h.put(2, 900)
        // One tall row does not drag the guess up the way an average would.
        assertEquals(listOf(100, 200, 900, 200, 200), h.list(5))
        assertEquals(listOf(0, 0), RowHeights().list(2))
    }
}
