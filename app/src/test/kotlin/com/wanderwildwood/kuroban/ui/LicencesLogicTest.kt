package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicencesLogicTest {
    private fun c(name: String, licence: String) = LicenceComponent(name, "someone", licence, "https://example.org/$name")
    private fun t(id: String) = LicenceText(id, id, 0)

    @Test fun familiesFromLicenceNames() {
        assertEquals("gpl", licenceFamily("GPL-3.0-or-later"))
        assertEquals("gpl", licenceFamily("GPL-3.0-only"))
        assertEquals("apache", licenceFamily("Apache-2.0"))
        assertEquals("mpl", licenceFamily("MPL-2.0"))
        assertEquals("mit", licenceFamily("MIT"))
        assertEquals("bsd", licenceFamily("BSD-3-Clause"))
        assertEquals("ofl", licenceFamily("OFL-1.1"))
        assertEquals("cc-by", licenceFamily("CC BY 4.0"))
        assertEquals("cc0", licenceFamily("CC0-1.0"))
        assertEquals("pd", licenceFamily("Public domain (PD-old-70-expired)"))
        assertEquals("gpl2-cpe", licenceFamily("GPL-2.0 with Classpath Exception"))
        assertEquals("unicode", licenceFamily("Unicode-3.0"))
        assertEquals("lgpl", licenceFamily("LGPL-2.1"))
        assertEquals(null, licenceFamily("Proprietary"))
    }

    @Test fun combinedLicencesNameEveryFamily() {
        assertEquals(listOf("apache", "bsd"), licenceFamilies("Apache-2.0 and BSD"))
        assertEquals(listOf("mit"), licenceFamilies("MIT"))
    }

    @Test fun textsAreOrderedGplFirstAndOnePerId() {
        val ordered = orderedTexts(listOf(t("ofl"), t("mit"), t("gpl"), t("apache"), t("mit")))
        assertEquals(listOf("gpl", "apache", "mit", "ofl"), ordered.map { it.id })
    }

    @Test fun componentsGroupByLicence() {
        val g = groupByLicence(listOf(c("a", "MIT"), c("b", "Apache-2.0"), c("c", "Apache-2.0 and BSD"), c("d", "Proprietary")))
        assertEquals(listOf("apache", "mit", "bsd", ""), g.keys.toList())
        assertEquals(listOf("b", "c"), g["apache"]!!.map { it.name })
        assertEquals(listOf("c"), g["bsd"]!!.map { it.name })
        assertEquals(listOf("d"), g[""]!!.map { it.name })
    }

    @Test fun problemsAreFoundAndAGoodSetHasNone() {
        val good = LicencesData("App", "https://github.com/x/y", "GPL-3.0-only", listOf(c("a", "MIT"), c("b", "CC0-1.0")), listOf(t("gpl"), t("mit")))
        assertTrue(licencesProblems(good).isEmpty())
        val missing = good.copy(texts = listOf(t("gpl")))
        assertEquals(listOf("no full text for mit"), licencesProblems(missing))
        val extra = good.copy(texts = good.texts + t("ofl"))
        assertEquals(listOf("text ofl is not used by anything"), licencesProblems(extra))
        assertTrue(licencesProblems(good.copy(components = listOf(c("a", "MIT"), c("a", "MIT")))).any { "twice" in it })
    }

    @Test fun componentLineReadsNameAuthorLicence() {
        assertEquals("a, someone: MIT", componentLine(c("a", "MIT")))
    }

    @Test fun paragraphsKeepEveryWord() {
        assertEquals(listOf("One two three", "Four five"), licenceParagraphs("One two\n three\n\n\nFour\r\nfive"))
    }
}
