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
import androidx.compose.foundation.lazy.rememberLazyListState
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
 * The Licences screen, the way Mudita's own apps do it: a plain list whose first row is the app
 * itself and then one row per library (name, author), alphabetical. A row opens that project's
 * page: its author, licence, a link to its website and the full licence text, paged by whole
 * paragraphs. The app's page carries the GPL text and the Source code link. [modifier] gives the
 * size (a dialog passes a fixed height, a screen fillMaxSize). [showBackButton] adds a Back button
 * under the list; a page always has one, and the system Back returns from it to the list at the
 * place it was left.
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
    val listState = rememberLazyListState()
    val page = open?.let { licencePage(data, it) }
    BackHandler(enabled = page != null) { open = null }
    if (page != null) {
        LicencePageScreen(page, labels, isApp = open == APP_ROW_KEY, onBack = { open = null }, modifier = modifier)
        return
    }
    LicencesFrame(modifier, title = if (showTitle) labels.title else null, back = if (showBackButton) labels.back else null, onBack = onBack) { pageModifier ->
        PagedColumn(modifier = pageModifier, state = listState) {
            licenceRows(data).forEach { r -> item(key = r.key) { LicenceRowView(r) { open = r.key } } }
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

/** One list row: the name, and under it the author (or, for the app, its licence); 48 dp at least. */
@Composable
private fun LicenceRowView(row: LicenceRow, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        TextMMD(text = row.title, style = MaterialTheme.typography.bodyMedium)
        if (row.subtitle.isNotEmpty()) TextMMD(text = row.subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LicencePageScreen(page: LicencePage, labels: LicencesLabels, isApp: Boolean, onBack: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val go = { url: String -> openLink(context, url, labels.noBrowser(url.removePrefix("https://"))) }
    val loaded by produceState(emptyList<List<String>>(), page.title) {
        value = withContext(Dispatchers.IO) {
            page.texts.map { t ->
                runCatching {
                    licenceParagraphs(context.resources.openRawResource(t.rawResId).bufferedReader().use { it.readText() })
                }.getOrDefault(emptyList())
            }
        }
    }
    LicencesFrame(modifier, title = null, back = labels.back, onBack = onBack) { pageModifier ->
        PagedColumn(modifier = pageModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // The name may be long (a library can be several names), so it scrolls with the page rather than sitting in a fixed title.
            item(key = "title") {
                TextMMD(text = page.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            page.author?.let { a -> item(key = "author") { LicencesBody(a) } }
            item(key = "licence") { LicencesBody(page.licence) }
            page.url?.let { u ->
                item(key = "link") { LicencesLinkRow(if (isApp) labels.sourceHeading else u.removePrefix("https://")) { go(u) } }
            }
            page.note?.let { n -> item(key = "note") { LicencesBody(n) } }
            if (page.texts.isEmpty()) {
                item(key = "none") { LicencesBody(labels.noText(page.licence)) }
            }
            page.texts.forEachIndexed { ti, t ->
                if (page.texts.size > 1) item(key = "h-$ti") { LicencesHeading(t.title) }
                loaded.getOrNull(ti).orEmpty().forEachIndexed { i, p -> item(key = "p-$ti-$i") { LicencesBody(p) } }
            }
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
