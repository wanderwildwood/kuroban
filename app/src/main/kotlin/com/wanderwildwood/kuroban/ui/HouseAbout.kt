package com.wanderwildwood.kuroban.ui


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD

/*
 * The one About dialog (STYLE.md, "About"), the same in every app. Edit it in
 * /opt/projects/shared-about, then run copy.sh for each app; never edit an app's copy.
 *
 * An `i` at the right of the Settings top bar opens it. The name and version head the dialog.
 * Under them, in this order (4 Nov 2026): the short description (the app's unheaded sections
 * before any heading, plain text), the source address, the llama, then the drawers: the app's own
 * headed sections, Licenses (the app's line and one small drawer per library, its license text
 * inline), Links (if any), Contact (only when no app section gives the address). A drawer is its
 * heading alone, no + or − mark, pressed anywhere to open or close it; all start closed and
 * several may be open. In the drawer whose words give the address, that paragraph IS the contact
 * row, pressed to write; no separate "Write to" line. Reading text is labelSmall (16 sp), the
 * name bodyLarge. Under the list ONE button, "Close". System Back closes the dialog.
 *
 * Every row of an open drawer is its own row of the list, so pages still turn on whole rows.
 *
 * Needs the app's EInkDialog, HouseButton, PagedColumn (and LocalListBleed), openLink (LinkOpener)
 * and the shared Licenses data (rememberLicencesData, rememberLicencesLabels, licenceRows,
 * licencePage, licenceParagraphs); no R, no strings: the app passes data, labels and the drawable id.
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
 * [openDrawer] and [closeDrawer] are what a screen reader says for a closed or an open drawer, the
 * heading passed in ("Open %1$s", "Close %1$s"). [contact] and [links] head the drawers the
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

/** The key of the Licenses drawer. */
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

/** Plain sentences for what is wrong with an app's data (use it in the app's test); empty is fine. */
fun aboutProblems(data: AboutData): List<String> = buildList {
    if (data.title.isBlank()) add("The title is empty.")
    data.sections.forEachIndexed { i, s -> if (s.body.isBlank()) add("Section ${i + 1} has no text.") }
    (data.links.map { it.url } + data.sourceUrl).forEach { if (!it.startsWith("https://")) add("The link $it is not https.") }
    val shown = data.links.map { it.label } + aboutAddress(data.sourceUrl)
    shown.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { add("The link $it is listed twice.") }
}

/**
 * The About. [licencesData] and [licencesLabels] fill the Licenses drawer; by default the app's own
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
    val room = LocalConfiguration.current.screenHeightDp.dp * 0.62f
    val list = rememberLazyListState()
    var open by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var opened by remember { mutableStateOf<String?>(null) }
    val rows = remember(data, labels) { aboutRows(data, labels) }
    val entries = remember(rows, open, licencesData) {
        aboutItems(rows, open, labels.licences) {
            aboutLicenceItems(
                licencesData,
                open,
                readText = { t -> t.content ?: context.resources.openRawResource(t.rawResId).bufferedReader().use { it.readText() } },
                noText = licencesLabels.noText,
            )
        }
    }
    // An opened drawer at the foot of a page whose first line does not fit there: bring the drawer
    // to the top of the page, so the press is answered on this page. Otherwise the page stays.
    LaunchedEffect(opened) {
        val key = opened ?: return@LaunchedEffect
        withFrameNanos { }
        val i = entries.indexOfFirst { it.key == key }
        val info = list.layoutInfo
        val foot = info.viewportSize.height - info.afterContentPadding
        val head = info.visibleItemsInfo.firstOrNull { it.index == i }
        val next = info.visibleItemsInfo.firstOrNull { it.index == i + 1 }
        if (head != null && head.offset - info.viewportStartOffset > 0 &&
            (next == null || next.offset - info.viewportStartOffset + next.size > foot)
        ) {
            list.scrollToItem(i)
        }
        opened = null
    }
    val toggle: (String) -> Unit = { key ->
        if (key in open) {
            open = open - key
        } else {
            open = open + key
            opened = key
        }
    }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            text = data.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        PagedColumn(modifier = Modifier.fillMaxWidth().height(room), state = list) {
            items(entries, key = { it.key }) { item ->
                when (item) {
                    is AboutItem.Head -> AboutHead(item, labels) { toggle(item.key) }
                    is AboutItem.Text -> TextMMD(
                        text = item.text,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (item.bold) FontWeight.Bold else null,
                        modifier = Modifier.fillMaxWidth().padding(start = INDENT * item.depth, top = 4.dp, bottom = 4.dp),
                    )
                    is AboutItem.Link -> AboutLine(item.label, depth = item.depth) {
                        openLink(context, item.url, labels.noBrowser(aboutAddress(item.url)))
                    }
                    is AboutItem.Contact -> AboutLine(item.label ?: labels.writeTo(item.address), depth = item.depth) {
                        openMail(context, item.address, data.title, labels.noMailApp(item.address))
                    }
                    AboutItem.Llama -> AboutLlama(labels, llamaRes)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        HouseButton(label = labels.close, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
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

/**
 * A drawer's row: the heading (and a second line, for a library) and no mark (4 Nov 2026: no + / −;
 * an open drawer shows by its text beneath), as tall as a thumb, pressed anywhere to open or close.
 * No ripple: on e-ink the press is the text appearing.
 */
@Composable
private fun AboutHead(item: AboutItem.Head, labels: AboutLabels, onToggle: () -> Unit) {
    val said = if (item.open) labels.closeDrawer(item.heading) else labels.openDrawer(item.heading)
    val weight = if (item.depth == 0) FontWeight.Bold else null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = INDENT * item.depth, top = if (item.depth == 0) 2.dp else 6.dp)
            .heightIn(min = 48.dp)
            .clearAndSetSemantics {
                contentDescription = said
                role = Role.Button
                onClick { onToggle(); true }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
    ) {
        Column(Modifier.weight(1f)) {
            TextMMD(text = item.heading, style = MaterialTheme.typography.labelSmall, fontWeight = weight)
            item.subtitle?.let { TextMMD(text = it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/**
 * Opens the phone's email app on a new message to [address], the subject already filled in. With no
 * email app it says so, address and all (the row shows the address in full too, so it can be copied
 * down by hand). No <queries> entry is needed: nothing is looked up first, the press is just caught.
 */
fun openMail(context: Context, address: String, subject: String, noMailApp: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(aboutMailUri(address, subject)))
        .putExtra(Intent.EXTRA_SUBJECT, subject)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, noMailApp, Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, noMailApp, Toast.LENGTH_LONG).show()
    }
}

/** A line that opens something: as tall as a thumb, in words. */
@Composable
private fun AboutLine(text: String, bold: Boolean = false, depth: Int = 0, onClick: () -> Unit) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.labelSmall,
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
 * The foot of the About: the site's address, and the llama with "Feed the llamas", which goes
 * straight to the checkout. The drawing is ink, not an emoji (STYLE.md).
 */
@Composable
private fun AboutLlama(labels: AboutLabels, llamaRes: Int) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        TextMMD(
            text = aboutAddress(ABOUT_WEBSITE_URL),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .clickable { openLink(context, ABOUT_WEBSITE_URL, labels.noBrowser(aboutAddress(ABOUT_WEBSITE_URL))) }
                .heightIn(min = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically),
        )
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { openLink(context, ABOUT_DONATE_URL, labels.noBrowser(aboutAddress(ABOUT_DONATE_URL))) }
                .heightIn(min = 48.dp),
        ) {
            Image(painter = painterResource(llamaRes), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            TextMMD(text = labels.feedTheLlamas, style = MaterialTheme.typography.labelSmall)
        }
    }
}
