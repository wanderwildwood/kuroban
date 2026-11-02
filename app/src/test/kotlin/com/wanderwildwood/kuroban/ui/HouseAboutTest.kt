package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseAboutTest {
    private val data = AboutData(
        title = "Sample 0.1.0",
        sections = listOf(AboutSection("Privacy", "Nothing leaves the phone."), AboutSection(null, "GPL-3.0-only.")),
        sourceUrl = "https://github.com/wanderwildwood/sample",
        links = listOf(AboutLinkData("example.org/work", "https://example.org/work")),
    )

    @Test fun rowsAreInStyleOrder() {
        val rows = aboutRows(data)
        assertEquals(7, rows.size)
        assertTrue(rows[0] is AboutRow.Title)
        assertTrue(rows[1] is AboutRow.Section && rows[2] is AboutRow.Section)
        assertEquals(AboutRow.Licences, rows[3])
        assertEquals(AboutRow.Link("example.org/work", "https://example.org/work"), rows[4])
        assertEquals(AboutRow.Link("github.com/wanderwildwood/sample", "https://github.com/wanderwildwood/sample"), rows[5])
        assertEquals(AboutRow.Llama, rows.last())
    }

    @Test fun addressDropsSchemeAndSlash() {
        assertEquals("wanderthe.dev", aboutAddress("https://wanderthe.dev"))
        assertEquals("square.link/u/AGu8oT10", aboutAddress(ABOUT_DONATE_URL))
        assertEquals("example.org/a", aboutAddress("https://example.org/a/"))
    }

    @Test fun goodDataHasNoProblems() = assertEquals(emptyList<String>(), aboutProblems(data))

    @Test fun problemsAreFound() {
        val bad = data.copy(
            title = " ",
            sections = listOf(AboutSection("Empty", "")),
            sourceUrl = "http://example.org/x",
            links = listOf(AboutLinkData("a", "http://a"), AboutLinkData("a", "https://a")),
        )
        val problems = aboutProblems(bad)
        assertEquals(5, problems.size)
        assertTrue(problems.any { it.contains("title") })
        assertTrue(problems.any { it.contains("not https") })
        assertTrue(problems.any { it.contains("twice") })
    }
}
