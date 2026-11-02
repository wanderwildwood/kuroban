package com.wanderwildwood.kuroban.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
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
        // MMD's list turns a fixed FOUR ROWS per page whatever their heights, so the rows are made one
        // height and the list exactly four of them tall: every page is whole rows, the last ends flush.
        BoxWithConstraints(pageModifier) {
            val density = LocalDensity.current
            val body = MaterialTheme.typography.bodyMedium
            val small = MaterialTheme.typography.bodySmall
            val plan = with(density) {
                licenceListPlan(
                    availablePx = maxHeight.roundToPx(),
                    verticalPadPx = ROW_PAD.roundToPx(),
                    titleLinePx = lineHeightPx(body),
                    subtitleLinePx = lineHeightPx(small),
                )
            }
            val rowHeight = with(density) { plan.rowPx.toDp() }
            PagedColumn(modifier = Modifier.fillMaxWidth().height(with(density) { plan.listPx.toDp() }), state = listState) {
                licenceRows(data).forEach { r -> item(key = r.key) { LicenceRowView(r, rowHeight, plan.titleLines) { open = r.key } } }
            }
        }
    }
}

/** One text line's height in px: the style's line height, or 1.3 times its size where it has none. */
private fun Density.lineHeightPx(style: TextStyle): Int {
    val lh = if (style.lineHeight.isSp) style.lineHeight.toPx() else style.fontSize.toPx() * 1.3f
    return lh.toInt().coerceAtLeast(1)
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

/**
 * One list row: the name (at most [titleLines] lines, then an ellipsis; the library's page shows it
 * in full), and under it the author (or, for the app, its licence) on one line. Exactly [height]
 * tall, so a page of the list is whole rows.
 */
@Composable
private fun LicenceRowView(row: LicenceRow, height: Dp, titleLines: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClick = onClick)
            .padding(vertical = ROW_PAD),
        verticalArrangement = Arrangement.Center,
    ) {
        TextMMD(text = row.title, style = MaterialTheme.typography.bodyMedium, maxLines = titleLines, overflow = TextOverflow.Ellipsis)
        if (row.subtitle.isNotEmpty()) TextMMD(text = row.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    // The rows of the page, in order, each with how it is drawn and (for measuring) its text and style.
    val rows = buildList {
        add(PageRow(PageRowKind.Title, page.title))
        page.author?.let { add(PageRow(PageRowKind.Body, it)) }
        add(PageRow(PageRowKind.Body, page.licence))
        page.url?.let { u -> add(PageRow(PageRowKind.Link, if (isApp) labels.sourceHeading else u.removePrefix("https://"), u)) }
        page.note?.let { add(PageRow(PageRowKind.Body, it)) }
        if (page.texts.isEmpty()) add(PageRow(PageRowKind.Body, labels.noText(page.licence)))
        page.texts.forEachIndexed { ti, t ->
            if (page.texts.size > 1) add(PageRow(PageRowKind.Heading, t.title))
            loaded.getOrNull(ti).orEmpty().forEach { add(PageRow(PageRowKind.Body, it)) }
        }
    }
    LicencesFrame(modifier, title = null, back = labels.back, onBack = onBack) { pageModifier ->
        // MMD's list turns a fixed FOUR ITEMS per swipe or chevron, whatever their heights. So each PAGE
        // is one item as tall as the list, holding the rows that fit it (measured here, `licencePageStartsByHeight`),
        // followed by three empty items that make up the four: a turn lands on the next page, no row is cut
        // at the foot or skipped. Rows are measured at the width the list gives its items.
        BoxWithConstraints(pageModifier) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val body = MaterialTheme.typography.bodyMedium
            val large = MaterialTheme.typography.bodyLarge
            val viewportPx = with(density) { maxHeight.roundToPx() }
            val gapPx = with(density) { PAGE_GAP.roundToPx() }
            var widthPx by remember { mutableIntStateOf(0) }
            val plan = remember(rows, widthPx, viewportPx, body, large) {
                if (widthPx <= 0) null else with(density) {
                    val limit = viewportPx - 2 // two px of slack so rounding in the real layout can never push a last line over the foot
                    fun styleOf(r: PageRow) = when (r.kind) {
                        PageRowKind.Title -> large.copy(fontWeight = FontWeight.Medium)
                        PageRowKind.Heading -> large.copy(fontWeight = FontWeight.Bold)
                        else -> body
                    }
                    fun heightOf(r: PageRow, laidOut: Int) = when (r.kind) {
                        PageRowKind.Link -> maxOf(laidOut, LINK_MIN.roundToPx())
                        PageRowKind.Heading -> laidOut + HEADING_TOP.roundToPx() + HEADING_BOTTOM.roundToPx()
                        else -> laidOut
                    }
                    // A paragraph taller than a whole page (a small screen) is cut at line ends into pieces that each fit one.
                    val pieces = ArrayList<PageRow>()
                    val heights = ArrayList<Int>()
                    for (r in rows) {
                        val layout = measurer.measure(r.text, styleOf(r), constraints = Constraints(maxWidth = widthPx))
                        if (layout.size.height <= limit || layout.lineCount < 2) {
                            pieces += r; heights += heightOf(r, layout.size.height)
                        } else {
                            val lineH = (layout.size.height / layout.lineCount).coerceAtLeast(1)
                            val per = (limit / lineH).coerceAtLeast(1)
                            var line = 0
                            while (line < layout.lineCount) {
                                val last = minOf(line + per, layout.lineCount) - 1
                                val piece = r.copy(text = r.text.substring(layout.getLineStart(line), layout.getLineEnd(last)).trim())
                                pieces += piece
                                heights += heightOf(piece, measurer.measure(piece.text, styleOf(piece), constraints = Constraints(maxWidth = widthPx)).size.height)
                                line = last + 1
                            }
                        }
                    }
                    PagePlan(pieces, licencePageStartsByHeight(heights, gapPx, limit, pieces.map { it.kind == PageRowKind.Heading }))
                }
            }
            val pageHeight = maxHeight
            PagedColumn(modifier = Modifier.fillMaxWidth().height(pageHeight)) {
                if (plan == null) {
                    // First a probe, two pages tall so the list shows its scrollbar and the width it will have is the one measured.
                    items(2, key = { "probe-$it" }) { Box(Modifier.fillMaxWidth().height(pageHeight).onSizeChanged { if (it.width != widthPx) widthPx = it.width }) }
                } else {
                    plan.starts.forEachIndexed { pi, start ->
                        val end = plan.starts.getOrNull(pi + 1) ?: plan.rows.size
                        item(key = "page-$pi") {
                            Column(
                                Modifier.fillMaxWidth().height(pageHeight).onSizeChanged { if (it.width != widthPx) widthPx = it.width },
                                verticalArrangement = Arrangement.spacedBy(PAGE_GAP),
                            ) {
                                for (k in start until end) {
                                    val r = plan.rows[k]
                                    when (r.kind) {
                                        PageRowKind.Title -> TextMMD(text = r.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        PageRowKind.Body -> LicencesBody(r.text)
                                        PageRowKind.Heading -> LicencesHeading(r.text)
                                        PageRowKind.Link -> LicencesLinkRow(r.text) { go(r.url.orEmpty()) }
                                    }
                                }
                            }
                        }
                        // the other items of this page's four (see above)
                        items(LICENCE_ROWS_PER_PAGE - 1, key = { "gap-$pi-$it" }) { Spacer(Modifier.height(0.dp)) }
                    }
                }
            }
        }
    }
}

private enum class PageRowKind { Title, Body, Heading, Link }

private class PagePlan(val rows: List<PageRow>, val starts: List<Int>)

private data class PageRow(val kind: PageRowKind, val text: String, val url: String? = null)

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
            .heightIn(min = LINK_MIN)
            .clickable(onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/** What a title line and the Back button with its gap take, which the paged area gives up. */
private val TITLE_LINE: Dp = 28.dp
private val BACK_ROOM: Dp = 60.dp

/** Padding above and below a list row's text. */
private val ROW_PAD: Dp = 6.dp

/** The gap between rows of a text page, a heading's padding and a link row's least height (as drawn below). */
private val PAGE_GAP: Dp = 8.dp
private val HEADING_TOP: Dp = 10.dp
private val HEADING_BOTTOM: Dp = 2.dp
private val LINK_MIN: Dp = 48.dp
