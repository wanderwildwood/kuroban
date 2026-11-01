package com.wanderwildwood.kuroban.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Licences screen: a short paged page (source link, the app's licence, Built with, and a
 * "Full texts" list with one row per licence), and behind each of those rows the full text on a
 * paged screen of its own. [modifier] gives the size (a dialog passes a fixed height, a screen
 * fillMaxSize). [showBackButton] adds a Back button under the short page; the full-text screen
 * always has one, and the system Back returns from it to the short page.
 */
@Composable
fun LicencesScreen(
    data: LicencesData,
    labels: LicencesLabels,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    showTitle: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val text = data.texts.firstOrNull { it.id == open }
    BackHandler(enabled = text != null) { open = null }
    if (text != null) {
        LicenceTextScreen(text, labels, onBack = { open = null }, modifier = modifier)
        return
    }
    val context = LocalContext.current
    val go = { url: String -> openLink(context, url, labels.noBrowser(url.removePrefix("https://"))) }
    LicencesFrame(modifier, title = if (showTitle) labels.title else null, back = if (showBackButton) labels.back else null, onBack = onBack) { pageModifier ->
        PagedColumn(modifier = pageModifier) {
            // A heading never ends a page alone: it shares a row with what follows it.
            item(key = "source") {
                Column {
                    LicencesHeading(labels.sourceHeading)
                    LicencesLinkRow(data.sourceUrl.removePrefix("https://")) { go(data.sourceUrl) }
                }
            }
            item(key = "licence") {
                Column {
                    LicencesHeading(labels.licenceHeading)
                    LicencesBody(data.licenceLine)
                }
            }
            headed("built", labels.builtWithHeading, data.components) { c -> LicencesLinkRow(componentLine(c)) { go(c.url) } }
            data.extraGroups.forEach { g ->
                item(key = "g-${g.heading}") {
                    Column {
                        LicencesHeading(g.heading)
                        g.note?.let { LicencesBody(it) }
                    }
                }
                g.components.forEach { c -> item(key = "gc-${c.name}") { LicencesLinkRow(componentLine(c)) { go(c.url) } } }
            }
            headed("texts", labels.fullTextsHeading, orderedTexts(data.texts)) { t -> LicencesLinkRow(t.title, underline = false) { open = t.id } }
        }
    }
}

/**
 * Title, the paged area and the Back button in a given space. The paged area gets an exact height
 * (what is left after the title and button), because a PagedColumn measures its rows against a
 * height it is handed, not one it takes from a weight.
 */
@Composable
private fun LicencesFrame(modifier: Modifier, title: String?, back: String?, onBack: () -> Unit, titleLines: Int = 1, page: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(modifier) {
        val titleRoom = if (title != null) TITLE_LINE * titleLines + 10.dp else 0.dp
        val backRoom = if (back != null) BACK_ROOM else 0.dp
        val pageHeight = (maxHeight - titleRoom - backRoom).coerceAtLeast(96.dp)
        Column {
            if (title != null) {
                TextMMD(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.height(TITLE_LINE * titleLines))
                Spacer(Modifier.height(10.dp))
            }
            page(Modifier.fillMaxWidth().height(pageHeight))
            if (back != null) LicencesBackButton(back, onBack)
        }
    }
}

/** Items for [rows] where the first shares its row with [heading], so the heading is never left alone at a page's foot. */
private fun <T> LazyListScope.headed(key: String, heading: String, rows: List<T>, row: @Composable (T) -> Unit) {
    rows.forEachIndexed { i, r ->
        item(key = "$key-$i") {
            if (i == 0) Column { LicencesHeading(heading); row(r) } else row(r)
        }
    }
}

/** "Name, author: licence", the one line a Built-with row shows. */
fun componentLine(c: LicenceComponent): String = "${c.name}, ${c.author}: ${c.licence}"

@Composable
private fun LicenceTextScreen(text: LicenceText, labels: LicencesLabels, onBack: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val paragraphs by produceState(emptyList<String>(), text.rawResId) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                licenceParagraphs(context.resources.openRawResource(text.rawResId).bufferedReader().use { it.readText() })
            }.getOrDefault(emptyList())
        }
    }
    LicencesFrame(modifier, title = text.title, back = labels.back, onBack = onBack, titleLines = 2) { pageModifier ->
        PagedColumn(modifier = pageModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            paragraphs.forEachIndexed { i, p -> item(key = "p-$i") { LicencesBody(p) } }
        }
    }
}

@Composable
private fun LicencesBackButton(label: String, onClick: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    OutlinedButtonMMD(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LicencesHeading(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun LicencesBody(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodyMedium)
}

/** A 48 dp row; underlined when it opens an address in a browser. */
@Composable
private fun LicencesLinkRow(text: String, underline: Boolean = true, onClick: () -> Unit) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textDecoration = if (underline) TextDecoration.Underline else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/** What a title line and the Back button with its gap take, which the paged area gives up. */
private val TITLE_LINE: Dp = 28.dp
private val BACK_ROOM: Dp = 60.dp
