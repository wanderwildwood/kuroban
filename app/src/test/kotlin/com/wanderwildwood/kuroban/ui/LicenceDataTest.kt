package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenceDataTest {

    @Test
    fun `every library has a name, a licence and a web address`() {
        assertTrue(LIBRARIES.isNotEmpty())
        LIBRARIES.forEach {
            assertTrue(it.name.isNotBlank() && it.licence.isNotBlank())
            assertTrue(it.link, it.link.startsWith("https://"))
        }
    }

    @Test
    fun `GNU Go is listed and its source is offered`() {
        assertTrue(LIBRARIES.any { it.name.startsWith("GNU Go") })
        assertTrue(GNUGO_SOURCE_URL.startsWith("$SOURCE_URL/"))
        assertTrue(GNUGO_SITE_URL.startsWith("https://www.gnu.org/"))
    }

    @Test
    fun `the source link is the public repository`() {
        assertEquals("https://github.com/wanderwildwood/kuroban", SOURCE_URL)
    }

    @Test
    fun `licence text is cut into paragraphs by blank lines`() {
        assertEquals(listOf("A b", "C"), licenceParagraphs("A\nb\n\n\nC"))
    }
}
