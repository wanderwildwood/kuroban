package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseAboutTest {
    private val data = AboutData(
        title = "Sample 0.1.0",
        sections = listOf(
            AboutSection(null, "Counts the days."),
            AboutSection("Privacy", "Nothing leaves the phone."),
            AboutSection(null, "No ads."),
            AboutSection("How to contact us", "Write to email@wanderthe.dev."),
            AboutSection("Terms", "GPL-3.0-only."),
        ),
        sourceUrl = "https://github.com/wanderwildwood/sample",
        links = listOf(AboutLinkData("example.org/work", "https://example.org/work")),
    )

    private val licences = LicencesData(
        appName = "Sample",
        sourceUrl = "https://github.com/wanderwildwood/sample",
        licenceLine = "Sample is free software: GPL-3.0-only.",
        components = listOf(LicenceComponent("Okio", "Square", "Apache-2.0", "https://square.github.io/okio/")),
        texts = listOf(LicenceText("gpl", "GPL (full text)", 0, "GPL one.\n\nGPL two."), LicenceText("apache", "Apache (full text)", 0, "Apache one.")),
    )

    private fun drawers(rows: List<AboutRow>) = rows.filterIsInstance<AboutRow.Drawer>().map { it.drawer }

    @Test fun rowsAreInStyleOrder() {
        val rows = aboutRows(data)
        assertEquals(AboutRow.Intro(0, "Counts the days."), rows[0])
        assertEquals(AboutRow.Link("github.com/wanderwildwood/sample", "https://github.com/wanderwildwood/sample"), rows[1])
        assertEquals(AboutRow.Llama, rows[2])
        assertEquals(listOf("Privacy", "How to contact us", "Terms"), drawers(rows.subList(3, 6)).map { it.heading })
        assertEquals(AboutRow.Licences, rows[6])
        assertEquals("Links", (rows[7] as AboutRow.Drawer).drawer.heading)
        assertEquals(8, rows.size)
    }

    @Test fun theShortDescriptionIsPlainTextNotADrawer() {
        val two = data.copy(sections = listOf(AboutSection(null, "Counts the days."), AboutSection(null, "Offline.")) + data.sections.drop(1))
        assertEquals(listOf("Counts the days.", "Offline."), aboutIntro(two))
        val d = aboutDrawers(two)
        assertTrue(d.none { it.heading == "What it does" })
        assertTrue(d.flatMap { it.bodies }.none { it == "Counts the days." || it == "Offline." })
        val items = aboutItems(aboutRows(two), d.map { it.key }.toSet(), "Licenses") { emptyList() }
        val intro = items.take(2)
        assertEquals(listOf("intro.0", "intro.1"), intro.map { it.key })
        assertEquals(listOf("Counts the days.", "Offline."), intro.map { (it as AboutItem.Text).text })
        // the same keys whatever is open
        assertEquals(intro, aboutItems(aboutRows(two), emptySet(), "Licenses") { error("closed") }.take(2))
    }

    @Test fun headedWordsStayInTheirDrawers() {
        val d = aboutDrawers(data)
        assertEquals(listOf("Nothing leaves the phone.", "No ads."), d[0].bodies)
        assertEquals(data.sections.drop(1).map { it.body }, d.flatMap { it.bodies })
    }

    @Test fun contactGoesWhereTheAddressIs() {
        val d = aboutDrawers(data)
        assertEquals(ABOUT_CONTACT_EMAIL, d.single { it.heading == "How to contact us" }.contact)
        assertEquals(1, d.count { it.contact != null })
        val bare = aboutDrawers(data.copy(sections = listOf(AboutSection("Terms", "GPL-3.0-only.")), links = emptyList()))
        assertEquals(listOf("Terms", "Contact"), bare.map { it.heading })
        assertEquals(ABOUT_CONTACT_EMAIL, bare.last().contact)
    }

    @Test fun theAddressParagraphIsTheContactRow() {
        val items = aboutItems(aboutRows(data), setOf("d1"), "Licenses") { emptyList() }
        val contact = items.filterIsInstance<AboutItem.Contact>().single()
        assertEquals("d1.contact", contact.key)
        assertEquals(ABOUT_CONTACT_EMAIL, contact.address)
        assertEquals("Write to email@wanderthe.dev.", contact.label)
        assertTrue(items.filterIsInstance<AboutItem.Text>().none { ABOUT_CONTACT_EMAIL in it.text })
        // a paragraph before the address one stays text, in its place
        val two = data.copy(sections = listOf(AboutSection("How to contact us", "We read everything."), AboutSection(null, "Write to email@wanderthe.dev.")))
        val rows = aboutItems(aboutRows(two), setOf("d0"), "Licenses") { emptyList() }.dropWhile { it.key != "d0" }
        assertEquals(listOf("d0", "d0.0", "d0.contact"), rows.take(3).map { it.key })
        assertEquals("Write to email@wanderthe.dev.", (rows[2] as AboutItem.Contact).label)
    }

    @Test fun withNoAddressParagraphTheWriteToRowStays() {
        val none = data.copy(sections = listOf(AboutSection("How to contact us", "Use the forum.")), links = emptyList())
        val items = aboutItems(aboutRows(none), setOf("d0", "contact"), "Licenses") { emptyList() }
        assertEquals(listOf("d0", "d0.0", ABOUT_LICENCES_KEY, "contact", "contact.contact"), items.dropWhile { it.key != "d0" }.map { it.key })
        assertNull((items.last() as AboutItem.Contact).label)
    }

    @Test fun drawerNamesComeFromTheLabels() {
        val labels = AboutLabels("Close", "Licenses", "Feed", { it }, { it }, { it }, intro = "In short", contact = "Write", links = "Pages")
        val names = drawers(aboutRows(data.copy(sections = listOf(AboutSection(null, "x"))), labels)).map { it.heading }
        assertEquals(listOf("Pages", "Write"), names)
    }

    @Test fun allClosedShowsTheTopPlainThenHeads() {
        val items = aboutItems(aboutRows(data), emptySet(), "Licenses") { error("closed") }
        assertEquals(8, items.size)
        assertEquals(AboutItem.Text("intro.0", "Counts the days."), items[0])
        assertTrue(items[1] is AboutItem.Link && items[2] == AboutItem.Llama)
        assertTrue(items.drop(3).all { it is AboutItem.Head && !it.open })
    }

    @Test fun anOpenDrawerShowsItsRowsOneByOne() {
        val rows = aboutRows(data)
        val items = aboutItems(rows, setOf("d0", "d1", "d2", "links"), "Licenses") { emptyList() }
        val keys = items.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(listOf("d0.0", "d0.1"), keys.filter { it.startsWith("d0.") })
        assertTrue(items.any { it is AboutItem.Contact && it.key == "d1.contact" })
        assertTrue(items.any { it is AboutItem.Link && it.url == "https://example.org/work" })
    }

    @Test fun licensesOpenInline() {
        var read = 0
        val closed = aboutLicenceItems(licences, emptySet(), { read++; it.content!! }, { "$it needs none" })
        assertEquals(0, read)
        assertEquals(listOf("lic.line", "lic/app", "lic/lib:Okio"), closed.map { it.key })
        val lib = aboutLicenceItems(licences, setOf("lic/lib:Okio"), { read++; it.content!! }, { "$it needs none" })
        assertEquals(1, read)
        assertEquals(listOf("Apache-2.0", "Apache (full text)", "Apache one."), lib.filterIsInstance<AboutItem.Text>().drop(1).map { it.text })
        assertTrue(lib.any { it is AboutItem.Link && it.url == "https://square.github.io/okio/" })
        val app = aboutLicenceItems(licences, setOf("lic/app"), { it.content!! }, { it })
        assertEquals(listOf("GPL one.", "GPL two."), app.filterIsInstance<AboutItem.Text>().filter { it.key.startsWith("lic/app.t0.") }.map { it.text })
        val items = aboutItems(aboutRows(data), setOf(ABOUT_LICENCES_KEY), "Licenses") { closed }
        assertEquals(closed, items.subList(7, 7 + closed.size))
    }

    @Test fun drawerLabelsDefaultToEnglish() {
        val labels = AboutLabels("Close", "Licenses", "Feed the llamas", { it }, { it }, { it })
        assertEquals("Open Privacy", labels.openDrawer("Privacy"))
        assertEquals("Close Privacy", labels.closeDrawer("Privacy"))
        assertNull(aboutDrawers(data).first().contact)
    }

    @Test fun mailUriCarriesTheSubject() {
        assertEquals("mailto:email@wanderthe.dev?subject=Cycle%200.3.6", aboutMailUri(ABOUT_CONTACT_EMAIL, "Cycle 0.3.6"))
        assertEquals("mailto:a@example.org?subject=Dream%20Log%20%26%20more", aboutMailUri("a@example.org", "Dream Log & more"))
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
