package com.wanderwildwood.kuroban.ui


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.mudita.mmd.components.text.TextMMD

/*
 * The one About dialog (STYLE.md, "About"), the same in every app. Edit it in
 * /opt/projects/shared-about, then run copy.sh for each app; never edit an app's copy.
 *
 * An `i` at the right of the Settings top bar opens it. The name and version head the dialog.
 * Under them, in this order (4 Nov 2026): the short description (the app's unheaded sections
 * before any heading, plain text), the source address, the llama, then one row per heading: the
 * app's own headed sections, Licenses, Links (if any), Contact (only when no app section gives the
 * address). A heading's row is the heading alone, no mark, pressed anywhere.
 *
 * Each heading opens a FULL-SCREEN PAGE (15 Nov 2026, InkApps review: the legal texts were read
 * in a narrow dialog, in drawers that opened in place). The page is its own window over the
 * dialog, the whole screen: the heading, the text in the house PagedColumn (whole-row pages, the
 * bar at the screen's edge), and ONE button, "Close". Licenses is a page listing the app's line
 * and one row per library; a library opens a page of its own (its author, license, address and
 * the full text paragraph by paragraph). Close or system Back goes one step back: a library to
 * Licenses, a page to the dialog, the dialog away. In the page whose words give the address, that
 * paragraph IS the contact row, pressed to write; no separate "Write to" line.
 *
 * Reading text is 18 sp on a 26 sp line, here and on every page (15 Nov 2026, the same review:
 * about 18 sp for body text; it was labelSmall, 16 sp). Headings are never smaller: a row's
 * heading is the same 18 sp in bold, the name and a page's heading bodyLarge (20 sp).
 *
 * Needs the app's EInkDialog, HouseButton, PagedColumn (and LocalListBleed), DarkWindow (DarkMode),
 * openLink (LinkOpener) and the shared Licenses data (rememberLicencesData, rememberLicencesLabels,
 * licenceRows, licencePage, licenceParagraphs); no R, no strings: the app passes data, labels and
 * the drawable id.
 */

const val ABOUT_WEBSITE_URL = "https://wanderthe.dev"
const val ABOUT_DONATE_URL = "https://square.link/u/AGu8oT10"

/** The one contact address, the same in every app, its PRIVACY.md and its in-app privacy text. */
const val ABOUT_CONTACT_EMAIL = "email@wanderthe.dev"

/**
 * One block of the app's own words: a heading (or none) and its text. A headed section is a
 * drawer; a section with no heading is the next paragraph of the drawer above it, and ones before
 * any heading are the short description, shown plainly above the source, the llama and the drawers.
 */
data class AboutSection(val heading: String?, val body: String)

/** A link row: [label] is what is shown, [url] must be https. */
data class AboutLinkData(val label: String, val url: String)

/**
 * What an app says about itself. [title] is the name and version in one line ("Level 0.1.2").
 * [sections] come in the order STYLE.md gives. [links] are extra addresses (a bundled work's page),
 * shown in a Links drawer.
 */
data class AboutData(
    val title: String,
    val sections: List<AboutSection>,
    val sourceUrl: String,
    val links: List<AboutLinkData> = emptyList(),
)

/**
 * The words the dialog itself needs. [noBrowser] turns a shown address into the plain sentence.
 * [writeTo] makes the contact row ("Write to %1$s"); [noMailApp] is what is said, address and all,
 * when the phone has no email app to take it ("No email app is installed. The address is %1$s.").
 * [openDrawer] is what a screen reader says for a heading's row, the heading passed in
 * ("Open %1$s"); [closeDrawer] is no longer used (15 Nov 2026: a heading opens a page, closed by
 * its Close button) and stays so call sites still compile. [contact] and [links] head the rows the
 * dialog makes itself. All are English unless the app passes its own. [intro] is no longer used
 * (4 Nov 2026: the short description is plain text, not a "What it does" drawer); it stays so
 * older call sites still compile.
 */
data class AboutLabels(
    val close: String,
    val licences: String,
    val feedTheLlamas: String,
    val noBrowser: (String) -> String,
    val writeTo: (String) -> String,
    val noMailApp: (String) -> String,
    val openDrawer: (String) -> String = { "Open $it" },
    val closeDrawer: (String) -> String = { "Close $it" },
    val intro: String = "What it does",
    val contact: String = "Contact",
    val links: String = "Links",
)

/** A drawer of the app's own words: its [heading], its paragraphs, and any links or contact row it holds. */
data class AboutDrawer(
    val key: String,
    val heading: String,
    val bodies: List<String>,
    val links: List<AboutLinkData> = emptyList(),
    val contact: String? = null,
)

/** The rows, top to bottom, all closed. */
sealed interface AboutRow {
    /** The app's short description, unheaded text before any heading: shown plainly, not a drawer. */
    data class Intro(val index: Int, val text: String) : AboutRow
    data class Drawer(val drawer: AboutDrawer) : AboutRow
    object Licences : AboutRow
    data class Link(val label: String, val url: String) : AboutRow
    object Llama : AboutRow
}

/** One row of the list as drawn, drawers opened: [key] is unique and stable. */
sealed interface AboutItem {
    val key: String

    /** A drawer's own row; [depth] 1 is a drawer inside Licenses, [subtitle] its second line. */
    data class Head(override val key: String, val heading: String, val open: Boolean, val depth: Int = 0, val subtitle: String? = null) : AboutItem
    data class Text(override val key: String, val text: String, val depth: Int = 0, val bold: Boolean = false) : AboutItem
    /** The contact row; [label] is the drawer's own sentence that gives the address, when there is one. */
    data class Contact(override val key: String, val address: String, val depth: Int = 0, val label: String? = null) : AboutItem
    data class Link(override val key: String, val label: String, val url: String, val depth: Int = 0) : AboutItem
    object Llama : AboutItem { override val key = "llama" }
}

/** The address as shown to a person: no scheme, no trailing slash. */
fun aboutAddress(url: String): String = url.removePrefix("https://").trimEnd('/')

/**
 * A mailto: address with the subject filled in (the app's name and version, so a message says which
 * app it is about). Spaces are %20, not +, which mail apps would show as a plus sign.
 */
fun aboutMailUri(address: String, subject: String): String =
    "mailto:$address?subject=" + java.net.URLEncoder.encode(subject, "UTF-8").replace("+", "%20")

/**
 * The app's sections as drawers: a headed section opens one, the unheaded sections after it are
 * its further paragraphs; unheaded ones before any heading are the short description (aboutIntro). The
 * contact row goes into the first drawer whose words give [ABOUT_CONTACT_EMAIL] (the shared "How
 * to contact us"); with none, it gets a drawer of its own, [contact]. Extra links get [links].
 * [intro] is no longer used (the short description is not a drawer); it stays for older callers.
 */
fun aboutDrawers(data: AboutData, intro: String = "What it does", contact: String = "Contact", links: String = "Links"): List<AboutDrawer> {
    val drawers = mutableListOf<AboutDrawer>()
    var heading: String? = null
    val bodies = mutableListOf<String>()
    fun shut() {
        heading?.let { drawers.add(AboutDrawer("d${drawers.size}", it, bodies.toList())) }
        heading = null
        bodies.clear()
    }
    data.sections.forEach { s ->
        if (s.heading != null) {
            shut()
            heading = s.heading
        } else if (heading == null) {
            return@forEach // the short description: aboutIntro, shown above the drawers
        }
        bodies.add(s.body)
    }
    shut()
    val holder = drawers.indexOfFirst { d -> d.bodies.any { ABOUT_CONTACT_EMAIL in it } }
    if (holder >= 0) drawers[holder] = drawers[holder].copy(contact = ABOUT_CONTACT_EMAIL)
    if (data.links.isNotEmpty()) drawers.add(AboutDrawer("links", links, emptyList(), links = data.links))
    if (holder < 0) drawers.add(AboutDrawer("contact", contact, emptyList(), contact = ABOUT_CONTACT_EMAIL))
    return drawers
}

/** The unheaded sections before any heading: the app's short description, shown plainly at the top. */
fun aboutIntro(data: AboutData): List<String> =
    data.sections.takeWhile { it.heading == null }.map { it.body }

/**
 * The short description, the source, the llama, then the app's drawers, Licenses, and the Links
 * and Contact drawers the dialog makes: always this order (4 Nov 2026).
 */
fun aboutRows(data: AboutData, labels: AboutLabels? = null): List<AboutRow> {
    val drawers = if (labels == null) aboutDrawers(data) else aboutDrawers(data, labels.intro, labels.contact, labels.links)
    val own = drawers.filter { it.key.startsWith("d") }
    return buildList {
        aboutIntro(data).forEachIndexed { i, t -> add(AboutRow.Intro(i, t)) }
        add(AboutRow.Link(aboutAddress(data.sourceUrl), data.sourceUrl))
        add(AboutRow.Llama)
        own.forEach { add(AboutRow.Drawer(it)) }
        add(AboutRow.Licences)
        (drawers - own.toSet()).forEach { add(AboutRow.Drawer(it)) }
    }
}

/** The key of the Licenses row and of its page. */
const val ABOUT_LICENCES_KEY = "licences"

/**
 * The rows as the list draws them: each drawer's own row, and, for the ones in [open], a row per
 * paragraph, link and contact line. An open Licenses drawer takes [licenceItems] under it.
 */
fun aboutItems(rows: List<AboutRow>, open: Set<String>, licencesHeading: String, licenceItems: () -> List<AboutItem>): List<AboutItem> = buildList {
    rows.forEach { row ->
        when (row) {
            is AboutRow.Drawer -> {
                val d = row.drawer
                val isOpen = d.key in open
                add(AboutItem.Head(d.key, d.heading, isOpen))
                if (isOpen) {
                    // The sentence that gives the address IS the contact row, pressed to write: no
                    // second "Write to" line saying the same (4 Nov 2026).
                    var said = false
                    d.bodies.forEachIndexed { i, b ->
                        val c = d.contact
                        if (c != null && !said && c in b) {
                            said = true
                            add(AboutItem.Contact("${d.key}.contact", c, label = b))
                        } else {
                            add(AboutItem.Text("${d.key}.$i", b))
                        }
                    }
                    d.links.forEachIndexed { i, l -> add(AboutItem.Link("${d.key}.link$i", l.label, l.url)) }
                    d.contact?.let { if (!said) add(AboutItem.Contact("${d.key}.contact", it)) }
                }
            }
            AboutRow.Licences -> {
                val isOpen = ABOUT_LICENCES_KEY in open
                add(AboutItem.Head(ABOUT_LICENCES_KEY, licencesHeading, isOpen))
                if (isOpen) addAll(licenceItems())
            }
            is AboutRow.Intro -> add(AboutItem.Text("intro.${row.index}", row.text))
            is AboutRow.Link -> add(AboutItem.Link("source", row.label, row.url))
            AboutRow.Llama -> add(AboutItem.Llama)
        }
    }
}

/**
 * Inside the Licenses drawer: the app's own license line, then one small drawer per row of the
 * shared list (the app first, then each library). An open one shows its license, any note, its
 * address, and its full text(s) paragraph by paragraph, read by [readText] only when opened.
 */
fun aboutLicenceItems(
    data: LicencesData,
    open: Set<String>,
    readText: (LicenceText) -> String,
    noText: (String) -> String,
): List<AboutItem> = buildList {
    add(AboutItem.Text("lic.line", data.licenceLine, depth = 1))
    licenceRows(data).forEach { r ->
        val key = "lic/${r.key}"
        val isOpen = key in open
        add(AboutItem.Head(key, r.title, isOpen, depth = 1, subtitle = r.subtitle.ifBlank { null }))
        if (!isOpen) return@forEach
        val page = licencePage(data, r.key) ?: return@forEach
        add(AboutItem.Text("$key.licence", page.licence, depth = 1))
        page.note?.let { add(AboutItem.Text("$key.note", it, depth = 1)) }
        page.url?.let { add(AboutItem.Link("$key.url", aboutAddress(it), it, depth = 1)) }
        if (page.texts.isEmpty()) add(AboutItem.Text("$key.none", noText(page.licence), depth = 1))
        page.texts.forEachIndexed { ti, t ->
            add(AboutItem.Text("$key.t$ti", t.title, depth = 1, bold = true))
            licenceParagraphs(readText(t)).forEachIndexed { pi, p -> add(AboutItem.Text("$key.t$ti.$pi", p, depth = 1)) }
        }
    }
}

/**
 * What a heading opens: a full-screen page. [heading] heads it, [subtitle] is a library's second
 * line (its author), [items] are its rows, none of them a page's own heading row except, on the
 * Licenses page, the rows of the libraries.
 */
data class AboutPage(val key: String, val heading: String, val subtitle: String?, val items: List<AboutItem>)

/**
 * The page for the heading row [key], or null when no row has that key. An app's own section, Links
 * and Contact give their paragraphs, links and contact row; [ABOUT_LICENCES_KEY] gives the app's
 * license line and a row per library; a library's key ("lic/…") gives that library's page.
 * [licenceItems] is [aboutLicenceItems] for a set of open libraries.
 */
fun aboutPage(rows: List<AboutRow>, key: String, licencesHeading: String, licenceItems: (Set<String>) -> List<AboutItem>): AboutPage? {
    if (key == ABOUT_LICENCES_KEY) {
        return if (AboutRow.Licences in rows) AboutPage(key, licencesHeading, null, licenceItems(emptySet())) else null
    }
    val all = if (aboutPageParent(key) != null) licenceItems(setOf(key)) else aboutItems(rows, setOf(key), licencesHeading) { emptyList() }
    val at = all.indexOfFirst { it is AboutItem.Head && it.key == key }
    if (at < 0) return null
    val head = all[at] as AboutItem.Head
    return AboutPage(key, head.heading, head.subtitle, aboutParagraphs(all.drop(at + 1).takeWhile { it !is AboutItem.Head }))
}

/**
 * A text of several paragraphs (blank lines between them) as one row per paragraph, so a page
 * turns on whole paragraphs and not through the middle of a long one. A single paragraph keeps
 * its row and its key.
 */
fun aboutParagraphs(items: List<AboutItem>): List<AboutItem> = items.flatMap { item ->
    if (item !is AboutItem.Text) return@flatMap listOf(item)
    val parts = item.text.split(Regex("\\n[ \\t]*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size <= 1) listOf(item) else parts.mapIndexed { i, p -> item.copy(key = "${item.key}/$i", text = p) }
}

/**
 * Where to cut a paragraph taller than a page: runs of its lines, each at most [room] pixels tall,
 * so every page holds whole lines. (22 Nov 2026: a paragraph taller than the page was turned a
 * page height at a time, by pixels, and a page could open on the bottom half of a line.)
 * [lineBottoms] is each line's bottom, measured from the paragraph's top; [gap] is the space
 * above each run (a later run opens its page with the same gap under the heading as the first).
 * Every run holds at least one line.
 */
fun aboutLineGroups(lineBottoms: List<Float>, gap: Float, room: Float): List<IntRange> {
    val groups = mutableListOf<IntRange>()
    var start = 0
    var top = 0f
    while (start < lineBottoms.size) {
        val limit = room - gap
        var end = start
        while (end + 1 < lineBottoms.size && lineBottoms[end + 1] - top <= limit) end++
        groups.add(start..end)
        top = lineBottoms[end]
        start = end + 1
    }
    return groups
}

/**
 * A page's rows with every paragraph that is taller than the page ([room] pixels, [gap] above a
 * paragraph) cut into runs of whole lines ([aboutLineGroups]), laid out as the page draws them:
 * [style] at [width] pixels. A paragraph that fits keeps its row and its key.
 */
private fun aboutFitPage(items: List<AboutItem>, measurer: TextMeasurer, style: TextStyle, width: Int, room: Float, gap: Float): List<AboutItem> =
    items.flatMap { item ->
        if (item !is AboutItem.Text) return@flatMap listOf(item)
        val layout = measurer.measure(
            item.text,
            style = if (item.bold) style.copy(fontWeight = FontWeight.Bold) else style,
            constraints = Constraints(maxWidth = width),
        )
        if (layout.size.height + gap <= room || layout.lineCount < 2) return@flatMap listOf(item)
        val groups = aboutLineGroups((0 until layout.lineCount).map { layout.getLineBottom(it) }, gap, room)
        groups.mapIndexed { i, lines ->
            val from = layout.getLineStart(lines.first)
            val to = layout.getLineEnd(lines.last)
            item.copy(key = "${item.key}#$i", text = item.text.substring(from, to).trim())
        }
    }

/** Where Close or Back on the page [key] leads: a library's page to Licenses, any other to the dialog (null). */
fun aboutPageParent(key: String): String? = if (key.startsWith("lic/")) ABOUT_LICENCES_KEY else null

/** Plain sentences for what is wrong with an app's data (use it in the app's test); empty is fine. */
fun aboutProblems(data: AboutData): List<String> = buildList {
    if (data.title.isBlank()) add("The title is empty.")
    data.sections.forEachIndexed { i, s -> if (s.body.isBlank()) add("Section ${i + 1} has no text.") }
    (data.links.map { it.url } + data.sourceUrl).forEach { if (!it.startsWith("https://")) add("The link $it is not https.") }
    val shown = data.links.map { it.label } + aboutAddress(data.sourceUrl)
    shown.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { add("The link $it is listed twice.") }
}

/**
 * The About. [licencesData] and [licencesLabels] fill the Licenses page; by default the app's own
 * `rememberLicencesData()` and `rememberLicencesLabels()`.
 */
@Composable
fun HouseAboutDialog(
    data: AboutData,
    labels: AboutLabels,
    llamaRes: Int,
    onDismiss: () -> Unit,
    licencesData: LicencesData = rememberLicencesData(),
    licencesLabels: LicencesLabels = rememberLicencesLabels(),
) {
    val context = LocalContext.current
    // 0.66 of the screen (it was 0.62): at 18 sp the description, the source, the site and the
    // llama still open on one page, and the dialog with its name and Close stays inside the screen.
    val room = LocalConfiguration.current.screenHeightDp.dp * 0.66f
    // The page that is open (a heading row's key), or null: saved, so it is still open after a turn.
    var pageKey by rememberSaveable { mutableStateOf<String?>(null) }
    val rows = remember(data, labels) { aboutRows(data, labels) }
    val entries = remember(rows, labels) { aboutItems(rows, emptySet(), labels.licences) { emptyList() } }
    val page = remember(rows, pageKey, licencesData, labels) {
        pageKey?.let { key ->
            aboutPage(rows, key, labels.licences) { open ->
                aboutLicenceItems(
                    licencesData,
                    open,
                    readText = { t -> t.content ?: context.resources.openRawResource(t.rawResId).bufferedReader().use { it.readText() } },
                    noText = licencesLabels.noText,
                )
            }
        }
    }
    // The Licenses list keeps its place while a library's page is read.
    val licencesList = rememberLazyListState()
    // "No browser" or "no mail app", with the address to type in by hand: it stays until it is
    // closed (16 Nov 2026; it was a Toast, gone before a long address could be read).
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val say: (String) -> Unit = { notice = it }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            text = data.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        // At most [room], and less where the screen has not that much under the name and over Close
        // (turned sideways, 16 Nov 2026: Typewriter's Close was squeezed to about 31 dp): the list
        // gives way and pages, Close keeps its full height.
        PagedColumn(modifier = Modifier.fillMaxWidth().heightIn(max = room).weight(1f, fill = false)) {
            aboutEntries(entries, data, labels, llamaRes, indent = true, onNotice = say) { pageKey = it }
        }
        Spacer(Modifier.height(14.dp))
        HouseButton(label = labels.close, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
    if (page != null) {
        val back = { pageKey = aboutPageParent(page.key) }
        AboutPageWindow(onBack = back) {
            TextMMD(
                text = page.heading,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(start = PAGE_SIDE, end = PAGE_SIDE, top = 16.dp),
            )
            // A full-screen list is not padded as a whole: the text is, so the bar sits at the
            // screen's edge (STYLE.md). Each page starts at its top; Licenses keeps its place.
            key(page.key) {
                // A paragraph taller than the page is cut into runs of whole lines (aboutFitPage), so a
                // turn never opens on half a line. That needs the width the text is really laid out at,
                // which the bar beside it decides: a text row reports it, and the list its height.
                var textWidth by remember { mutableStateOf(0) }
                var listHeight by remember { mutableStateOf(0) }
                val measurer = rememberTextMeasurer()
                val body = aboutBody()
                val gap = with(LocalDensity.current) { PAGE_PARAGRAPH_GAP.toPx() }
                val rows = remember(page.items, textWidth, listHeight, body) {
                    // A pixel to spare: the list's own check is "bottom <= height", and a run that ends
                    // a hair over the foot would be sent whole to the next page with room left above.
                    if (textWidth <= 0 || listHeight <= 0) page.items
                    else aboutFitPage(page.items, measurer, body, textWidth, listHeight - 2f, gap)
                }
                PagedColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).onSizeChanged { listHeight = it.height },
                    state = if (page.key == ABOUT_LICENCES_KEY) licencesList else rememberLazyListState(),
                    contentPadding = PaddingValues(start = PAGE_SIDE, end = 12.dp),
                ) {
                    page.subtitle?.let { line ->
                        item(key = "subtitle") {
                            TextMMD(text = line, style = aboutBody(), modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                        }
                    }
                    aboutEntries(rows, data, labels, llamaRes, indent = false, onNotice = say, onTextWidth = { textWidth = it }) { pageKey = it }
                }
            }
            HouseButton(
                label = labels.close,
                onClick = back,
                modifier = Modifier.fillMaxWidth().padding(start = PAGE_SIDE, end = PAGE_SIDE, top = 8.dp, bottom = 12.dp),
            )
        }
    } else if (pageKey != null) {
        SideEffect { pageKey = null } // a saved key that names no row any more
    }
    notice?.let { text ->
        EInkDialog(onDismiss = { notice = null }) {
            TextMMD(text = text, style = aboutBody(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            HouseButton(label = labels.close, onClick = { notice = null }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * A page's window: the whole screen, over the About dialog, no dimmed backdrop (EInkDialog's
 * reasons), dark in dark mode from its first frame. Back, like the page's Close, calls [onBack].
 */
@Composable
private fun AboutPageWindow(onBack: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        DarkWindow()
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setDimAmount(0f)
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Not inside a dialog's padding: the list does not run out into any.
            CompositionLocalProvider(LocalListBleed provides 0.dp) {
                Column(Modifier.fillMaxSize(), content = content)
            }
        }
    }
}

/** The rows of the dialog's list or of a page's; [indent] sets a library's rows in (never on a page). */
private fun LazyListScope.aboutEntries(
    entries: List<AboutItem>,
    data: AboutData,
    labels: AboutLabels,
    llamaRes: Int,
    indent: Boolean,
    onNotice: (String) -> Unit,
    onTextWidth: ((Int) -> Unit)? = null,
    onOpen: (String) -> Unit,
) {
    // The site's address and the llama are two rows of the list, so a page can end between them.
    // The address is not one of the model's rows: it is the null before the llama.
    val drawn: List<AboutItem?> = entries.flatMap { if (it == AboutItem.Llama) listOf(null, it) else listOf(it) }
    items(drawn, key = { it?.key ?: "site" }) { item ->
        val context = LocalContext.current
        when (item) {
            null -> AboutLine(aboutAddress(ABOUT_WEBSITE_URL)) {
                openLink(context, ABOUT_WEBSITE_URL, labels.noBrowser(aboutAddress(ABOUT_WEBSITE_URL)), onNotice)
            }
            is AboutItem.Head -> AboutHead(if (indent) item else item.copy(depth = 0), labels, bold = !indent || item.depth == 0) { onOpen(item.key) }
            is AboutItem.Text -> TextMMD(
                text = item.text,
                style = aboutBody(),
                fontWeight = if (item.bold) FontWeight.Bold else null,
                // On a page a paragraph is a row. The gap between two is all above the paragraph, none
                // below: a paragraph whose last line just fits the page is not sent to the next one
                // for the sake of empty space under it.
                modifier = Modifier.fillMaxWidth()
                    .then(if (onTextWidth != null) Modifier.onSizeChanged { onTextWidth(it.width) } else Modifier)
                    .padding(
                        start = if (indent) INDENT * item.depth else 0.dp,
                        top = if (indent) 4.dp else PAGE_PARAGRAPH_GAP,
                        bottom = if (indent) 4.dp else 0.dp,
                    ),
            )
            is AboutItem.Link -> AboutLine(item.label, depth = if (indent) item.depth else 0) {
                openLink(context, item.url, labels.noBrowser(aboutAddress(item.url)), onNotice)
            }
            is AboutItem.Contact -> AboutLine(item.label ?: labels.writeTo(item.address), depth = if (indent) item.depth else 0) {
                openMail(context, item.address, data.title, labels.noMailApp(item.address), onNotice)
            }
            AboutItem.Llama -> AboutLlama(labels, llamaRes, onNotice)
        }
    }
}

/**
 * The call as it was before Licenses became a drawer: the trailing Licenses page is no longer
 * used, so an app that has not moved its call site yet still builds and shows the drawers.
 */
@Composable
fun HouseAboutDialog(
    data: AboutData,
    labels: AboutLabels,
    llamaRes: Int,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") licences: @Composable (Modifier, () -> Unit) -> Unit,
) = HouseAboutDialog(data, labels, llamaRes, onDismiss, rememberLicencesData(), rememberLicencesLabels())

private val INDENT = 14.dp

/** A page's side padding: the text's, not the list's (the bar sits at the screen's edge). */
private val PAGE_SIDE = 20.dp

/** The space above a paragraph on a page (none below; see aboutEntries). */
private val PAGE_PARAGRAPH_GAP = 12.dp

/** The About's reading size and its line, in sp (15 Nov 2026, InkApps review: about 18 sp for body text). */
const val ABOUT_BODY_SP: Float = 18f
const val ABOUT_BODY_LINE_SP: Float = 26f

/**
 * The About's reading text, in the dialog and on every page it opens: the theme's bodyMedium
 * (18 sp in MMD's scale and the house one), held at 18 sp if a theme sets it smaller, on a 26 sp
 * line (MMD sets no line height, and Lato's own line is 1.2 times its size, which reads tight in
 * a long legal text and measures as smaller type than it is). The name above stays bodyLarge
 * (20 sp); a heading's row is this size in bold.
 */
@Composable
private fun aboutBody(): TextStyle {
    val s = MaterialTheme.typography.bodyMedium
    val size = if (s.fontSize.isSp && s.fontSize.value >= ABOUT_BODY_SP) s.fontSize else ABOUT_BODY_SP.sp
    val line = if (s.lineHeight.isSp && s.lineHeight.value >= ABOUT_BODY_LINE_SP) s.lineHeight else (ABOUT_BODY_LINE_SP * size.value / ABOUT_BODY_SP).sp
    return s.copy(fontSize = size, lineHeight = line)
}

/**
 * A heading's row: the heading (and a second line, for a library) and no mark, as tall as a thumb,
 * pressed anywhere to open its page. No ripple: on e-ink the press is the page appearing.
 */
@Composable
private fun AboutHead(item: AboutItem.Head, labels: AboutLabels, bold: Boolean, onOpen: () -> Unit) {
    // The second line (a library's author, its copyright holder: Lato's is the font's designer) is
    // said too: the row's own words are replaced by [said], so a line left out here is never heard.
    val said = labels.openDrawer(item.heading) + item.subtitle?.let { ", $it" }.orEmpty()
    val weight = if (bold) FontWeight.Bold else null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = INDENT * item.depth, top = if (item.depth == 0) 2.dp else 6.dp)
            .heightIn(min = 48.dp)
            .clearAndSetSemantics {
                contentDescription = said
                role = Role.Button
                onClick { onOpen(); true }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen),
    ) {
        // A library's two lines get air above and below, so its row is told from the next one.
        Column(Modifier.weight(1f).padding(vertical = if (item.subtitle == null) 0.dp else 6.dp)) {
            TextMMD(text = item.heading, style = aboutBody(), fontWeight = weight)
            item.subtitle?.let { TextMMD(text = it, style = aboutBody()) }
        }
    }
}

/**
 * Opens the phone's email app on a new message to [address], the subject already filled in. With no
 * email app it says so, address and all (the row shows the address in full too, so it can be copied
 * down by hand). No <queries> entry is needed: nothing is looked up first, the press is just caught.
 */
fun openMail(context: Context, address: String, subject: String, noMailApp: String, onNone: ((String) -> Unit)? = null) {
    val say = onNone ?: { text -> Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(aboutMailUri(address, subject)))
        .putExtra(Intent.EXTRA_SUBJECT, subject)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        say(noMailApp)
    } catch (_: SecurityException) {
        say(noMailApp)
    }
}

/** A line that opens something: as tall as a thumb, in words. */
@Composable
private fun AboutLine(text: String, bold: Boolean = false, depth: Int = 0, onClick: () -> Unit) {
    TextMMD(
        text = text,
        style = aboutBody(),
        fontWeight = if (bold) FontWeight.Bold else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = INDENT * depth, top = 6.dp)
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/**
 * The llama with "Feed the llamas", which goes straight to the checkout, on the line under the
 * site's address (15 Nov 2026: at 18 sp the two no longer fit side by side in the dialog, and
 * "Feed the llamas" broke in two). The drawing is ink, not an emoji (STYLE.md).
 */
@Composable
private fun AboutLlama(labels: AboutLabels, llamaRes: Int, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .heightIn(min = 48.dp)
            .clickable { openLink(context, ABOUT_DONATE_URL, labels.noBrowser(aboutAddress(ABOUT_DONATE_URL)), onNotice) },
    ) {
        Image(painter = painterResource(llamaRes), contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        TextMMD(text = labels.feedTheLlamas, style = aboutBody())
    }
}
