package com.wanderwildwood.kuroban.ui


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD

/*
 * The one About dialog (STYLE.md, "About"), the same in every app. Edit it in
 * /opt/projects/shared-about, then run copy.sh for each app; never edit an app's copy.
 *
 * An `i` at the right of the Settings top bar opens it. Inside, in this order and nothing else:
 * the name and version, the app's own sections (what it collects or sends where that is worth a
 * stranger's attention, the licence named in full, anything bundled), the Licences row (the
 * shared Licences screen, in the same dialog), further links, "Write to email@wanderthe.dev", the
 * source address, the llama.
 * Under it ONE button, "Close". System Back closes the dialog; on the Licences page it returns to
 * the About first.
 *
 * Needs the app's EInkDialog, HouseButton, PagedColumn (and LocalListBleed), openLink (LinkOpener)
 * and the shared Licences screen; no R, no strings: the app passes data, labels and the drawable id.
 */

const val ABOUT_WEBSITE_URL = "https://wanderthe.dev"
const val ABOUT_DONATE_URL = "https://square.link/u/AGu8oT10"

/** The one contact address, the same in every app, its PRIVACY.md and its in-app privacy text. */
const val ABOUT_CONTACT_EMAIL = "email@wanderthe.dev"

/** One block of the app's own words: a bold heading (or none) and its text. */
data class AboutSection(val heading: String?, val body: String)

/** A link row: [label] is what is shown, [url] must be https. */
data class AboutLinkData(val label: String, val url: String)

/**
 * What an app says about itself. [title] is the name and version in one line ("Level 0.1.2").
 * [sections] come in the order STYLE.md gives. [links] are extra addresses (a bundled work's page).
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
 */
data class AboutLabels(
    val close: String,
    val licences: String,
    val feedTheLlamas: String,
    val noBrowser: (String) -> String,
    val writeTo: (String) -> String,
    val noMailApp: (String) -> String,
)

/** The rows, top to bottom. */
sealed interface AboutRow {
    data class Title(val text: String) : AboutRow
    data class Section(val section: AboutSection) : AboutRow
    object Licences : AboutRow
    data class Link(val label: String, val url: String) : AboutRow
    data class Contact(val address: String) : AboutRow
    object Llama : AboutRow
}

/** The address as shown to a person: no scheme, no trailing slash. */
fun aboutAddress(url: String): String = url.removePrefix("https://").trimEnd('/')

/**
 * A mailto: address with the subject filled in (the app's name and version, so a message says which
 * app it is about). Spaces are %20, not +, which mail apps would show as a plus sign.
 */
fun aboutMailUri(address: String, subject: String): String =
    "mailto:$address?subject=" + java.net.URLEncoder.encode(subject, "UTF-8").replace("+", "%20")

/** Title, sections, the Licences row, further links, the contact, the source, the llama: always this order. */
fun aboutRows(data: AboutData): List<AboutRow> = buildList {
    add(AboutRow.Title(data.title))
    data.sections.forEach { add(AboutRow.Section(it)) }
    add(AboutRow.Licences)
    data.links.forEach { add(AboutRow.Link(it.label, it.url)) }
    add(AboutRow.Contact(ABOUT_CONTACT_EMAIL))
    add(AboutRow.Link(aboutAddress(data.sourceUrl), data.sourceUrl))
    add(AboutRow.Llama)
}

/** Plain sentences for what is wrong with an app's data (use it in the app's test); empty is fine. */
fun aboutProblems(data: AboutData): List<String> = buildList {
    if (data.title.isBlank()) add("The title is empty.")
    data.sections.forEachIndexed { i, s -> if (s.body.isBlank()) add("Section ${i + 1} has no text.") }
    (data.links.map { it.url } + data.sourceUrl).forEach { if (!it.startsWith("https://")) add("The link $it is not https.") }
    val shown = data.links.map { it.label } + aboutAddress(data.sourceUrl)
    shown.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { add("The link $it is listed twice.") }
}

@Composable
fun HouseAboutDialog(
    data: AboutData,
    labels: AboutLabels,
    llamaRes: Int,
    onDismiss: () -> Unit,
    licences: @Composable (Modifier, () -> Unit) -> Unit,
) {
    val room = LocalConfiguration.current.screenHeightDp.dp * 0.62f
    var showLicences by rememberSaveable { mutableStateOf(false) }
    EInkDialog(onDismiss = { if (showLicences) showLicences = false else onDismiss() }) {
        if (showLicences) {
            licences(Modifier.fillMaxWidth().height(room)) { showLicences = false }
            return@EInkDialog
        }
        PagedColumn(modifier = Modifier.fillMaxWidth().height(room)) {
            aboutRows(data).forEachIndexed { i, row ->
                item(key = "about$i") {
                    when (row) {
                        is AboutRow.Title -> TextMMD(
                            text = row.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        is AboutRow.Section -> AboutPart(row.section)
                        AboutRow.Licences -> AboutLine(labels.licences, bold = true) { showLicences = true }
                        is AboutRow.Link -> {
                            val context = LocalContext.current
                            AboutLine(row.label) { openLink(context, row.url, labels.noBrowser(aboutAddress(row.url))) }
                        }
                        is AboutRow.Contact -> {
                            val context = LocalContext.current
                            AboutLine(labels.writeTo(row.address)) {
                                openMail(context, row.address, data.title, labels.noMailApp(row.address))
                            }
                        }
                        AboutRow.Llama -> AboutLlama(labels, llamaRes)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        HouseButton(label = labels.close, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AboutPart(section: AboutSection) {
    Column(Modifier.padding(top = 14.dp)) {
        if (section.heading != null) {
            TextMMD(text = section.heading, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        TextMMD(text = section.body, style = MaterialTheme.typography.labelSmall)
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
private fun AboutLine(text: String, bold: Boolean = false, onClick: () -> Unit) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.Bold else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
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
