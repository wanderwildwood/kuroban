package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The list every screen uses: Mudita's list look, with its scrollbar (a chevron at each end that
 * turns a page, a track that jumps to the place pressed, a thumb that shows where the page is), but
 * a page turns on WHOLE ROWS. MMD's own `LazyColumnMMD` moves four rows a turn whatever their
 * heights, so tall rows ran off the foot of a page (Sky's Today lost its credits that way). Here
 * (3 Nov 2026: pages end on whole rows; the scrollbar stays):
 *
 * - a turn forward brings the first row that did not fit to the top of the page;
 * - a row that does not fit at the foot is not drawn (nor pressable) until the next page, and
 *   (16 Nov 2026) is not there for a screen reader either;
 * - a blank tail after the last row lets that row come to the top, so the last page also starts on
 *   a row and is never shifted back to end flush, mid-row at its top;
 * - a turn back goes one page back and lands on a row.
 *
 * A row taller than the whole page is read a page at a time within it.
 *
 * [showCutRow] is for lists of tall rows (an inbox, conversations, articles, notes, books; decided
 * 5 Nov 2026): the row the foot cuts through is drawn and can be pressed, so a page has no blank
 * band at its foot, and a turn forward still brings that row to the top, whole, so nothing is
 * skipped. There is no blank tail: the last page ends flush and may open part-way down a row the
 * page before already showed. A turn back goes one page back and lands on a row, as it does
 * without it. Off by default; every other list keeps whole-row pages.
 *
 * The bar is drawn as MMD's `VerticalScrollbar` draws it (MMD 1.0.2, `LazyMMD.kt`, Apache-2.0,
 * Copyright 2025 The Android Open Source Project, modified by Mudita Sp. z o.o.), with its own
 * chevron drawables; only where a turn lands is ours, and (15 Nov 2026) the arrow at an end the
 * list cannot turn past, drawn as a plain outline instead of MMD's dotted one (a grey dither on
 * e-ink). A screen reader calls the arrows "Previous page" and "Next page" (the app's strings
 * `page_arrow_previous` and `page_arrow_next` where it has them).
 *
 * Called as `LazyColumnMMD` was. [isScrollbarVisible] is on by default; a list that must not show
 * one passes false. [folio] and [rowStep] are kept so every existing call compiles.
 *
 * [barClearBelow] is for a screen with a floating button in the corner under the bar (11 Nov 2026):
 * the bar ends that far above the foot of the list, so the button keeps its corner and the lower
 * chevron stands over it instead of under it, where a press meant for the next page pressed the
 * button. One 56 dp button on its 16 dp margin is 72 dp. The rows are not moved.
 *
 * Whether the bar is needed is decided at the rows' full width (16 Nov 2026). The bar takes width
 * from the rows, so rows that wrap onto more lines beside it can keep needing it: Wallet's Settings,
 * shrunk by a dialog's keyboard, got the bar, and when the keyboard closed its narrower rows still
 * ran onto a second page, so the bar and the narrower text stayed. Now, when the list grows (taller
 * or wider) while the bar is up, the rows are laid out once without it, and the bar comes back only
 * if they still run past one page.
 *
 * A list that gets shorter under its page (15 Nov 2026) comes back to its last real page. Level's
 * Settings: turned on to "Forget calibration", then forgotten, the row went and the page was left
 * on the blank tail alone, with no scrollbar and nothing a swipe could turn. The page that ends
 * on the last row is shown instead, as a turn back from the end would show it. Only then: a list
 * that grows, or a page that still shows a row, is left where it is, and a turn under way is let
 * finish first.
 *
 * [onPullAtTop], when given, is called when a swipe down starts on the first page of the rows: the
 * list asks for them again. It asks for nothing otherwise, and does not take the swipe from the
 * list. Saying what the pull set going is the caller's, in words.
 *
 * Do not read state derived from the list's layout inside [content]: every layout then rebuilds
 * the items and asks for another, a loop that keeps a core busy while idle. (The list itself reads
 * it only while drawing and in the bar, outside the items.)
 */
@Composable
fun PagedColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    isScrollbarVisible: Boolean = true,
    onPullAtTop: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") rowStep: (() -> Float?)? = null,
    @Suppress("UNUSED_PARAMETER") folio: Boolean = false,
    showCutRow: Boolean = false,
    barClearBelow: Dp = 0.dp,
    content: LazyListScope.() -> Unit,
) {
    // The gesture below is set up once for the list; read the caller's latest action, not the
    // one it was handed first.
    val pull by rememberUpdatedState(onPullAtTop)
    val scope = rememberCoroutineScope()
    // In a dialog the list sits inside the dialog's own padding, which leaves the scrollbar floating
    // well in from the rim. The dialog says how much room it has ([LocalListBleed]); the list runs out
    // into it, so the bar sits at the edge, and takes the same amount off its own end so the rows
    // stay exactly where they were.
    val direction = LocalLayoutDirection.current
    val bleed = if (isScrollbarVisible) LocalListBleed.current else 0.dp
    val padding = if (bleed > 0.dp) {
        PaddingValues(
            start = contentPadding.calculateStartPadding(direction),
            top = contentPadding.calculateTopPadding(),
            end = contentPadding.calculateEndPadding(direction) + bleed,
            bottom = contentPadding.calculateBottomPadding(),
        )
    } else {
        contentPadding
    }
    val widen = if (bleed > 0.dp) {
        Modifier.layout { measurable, constraints ->
            val max = if (constraints.hasBoundedWidth) constraints.maxWidth + bleed.roundToPx() else constraints.maxWidth
            val placeable = measurable.measure(
                constraints.copy(minWidth = if (constraints.hasBoundedWidth) max else constraints.minWidth, maxWidth = max),
            )
            // The parent trims our width back to what it offered and centres what is left over, which would
            // slide the whole list left by half the bleed; put it back so the rows stay and only the end moves.
            layout(placeable.width, placeable.height) { placeable.place(bleed.roundToPx() / 2, 0) }
        }
    } else {
        Modifier
    }
    val cutRow by rememberUpdatedState(showCutRow)
    val shown by remember(state) { derivedStateOf { state.layoutInfo.totalItemsCount > if (cutRow) 0 else 1 } }
    val pages by remember(state) { derivedStateOf { state.layoutInfo.view(cutRow).pages } }
    // The rows went from under the page: back to the last real one (see above).
    val lost by remember(state) { derivedStateOf { stranded(state.layoutInfo.view(cutRow)) } }
    LaunchedEffect(state, lost) {
        if (!lost) return@LaunchedEffect
        snapshotFlow { state.isScrollInProgress }.first { !it }
        if (stranded(state.layoutInfo.view(cutRow))) state.resnap(cutRow)
    }
    // The bar at full width (see above): while [recheckAt] holds the list's width from before, the
    // bar is left out until the rows have been laid out wider than that, without it.
    var recheckAt by remember(state) { mutableStateOf(-1) }
    val bar = pages && isScrollbarVisible && recheckAt < 0
    val barNow by rememberUpdatedState(bar)
    LaunchedEffect(state) {
        var wide = 0
        var tall = 0
        snapshotFlow { state.layoutInfo.viewportSize }.collect { now ->
            if (recheckAt >= 0) {
                if (now.width > recheckAt) recheckAt = -1
            } else if (recheckBar(barNow, wide, tall, now.width, now.height)) {
                recheckAt = now.width
            }
            wide = now.width
            tall = now.height
        }
    }
    Row(
        modifier = (if (onPullAtTop == null) modifier else modifier.pullAtTop(state) { pull?.invoke(); Unit })
            .then(widen)
            // As MMD does: nothing is drawn until the rows are there, so the screen does not flash.
            .alpha(if (shown) 1f else 0f),
    ) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val tail = maxHeight
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state) {
                        var turned = false
                        detectVerticalDragGestures(onDragStart = { turned = false }) { _, drag ->
                            if (!turned && drag != 0f) {
                                turned = true
                                scope.launch { if (drag < 0f) state.turnForward(cutRow) else state.turnBack(cutRow) }
                            }
                        }
                    }
                    // A row hidden at the foot takes no press either: what cannot be seen is not there.
                    .pointerInput(state) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val foot = state.layoutInfo.view(cutRow).foot
                            if (foot != null && down.position.y >= foot) down.consume()
                        }
                    }
                    .drawWithContent {
                        // Read here, while drawing, so a page turn redraws without composing anything.
                        val foot = state.layoutInfo.view(cutRow).foot
                        if (foot == null) drawContent() else clipRect(bottom = foot.toFloat()) { this@drawWithContent.drawContent() }
                    },
                state = state,
                contentPadding = padding,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                // The list turns its own pages.
                userScrollEnabled = false,
            ) {
                FootScope(this) { row -> FootRow(state, showCutRow, horizontalAlignment, row) }.content()
                // With the cut row shown there is no tail: the last page ends flush.
                // The tail has no key of its own (8 Nov 2026): a list keeps its place on the key of its
                // first row, so a list that opened empty (its rows still loading) and so on the tail alone
                // would stay on the tail when the rows came in above it, a blank page. Known by its type.
                if (!showCutRow) item(contentType = TAIL) { Spacer(Modifier.height(tail)) }
            }
        }
        if (bar) {
            PageBar(
                state = state,
                cutRow = showCutRow,
                clearBelow = barClearBelow,
                onBack = { scope.launch { state.turnBack(showCutRow) } },
                onForward = { scope.launch { state.turnForward(showCutRow) } },
                onFirst = { scope.launch { state.scrollToItem(0) } },
                onLast = { scope.launch { state.scrollToItem(state.lastPageStart(showCutRow)) } },
                onJump = { index -> scope.launch { state.scrollToItem(index) } },
            )
        }
    }
}

/**
 * The caller's rows, each handed through [wrap] (16 Nov 2026), so the list can take a row that is
 * not drawn at the foot of the page out of what a screen reader is told. Sticky headers are passed
 * on as they are.
 */
@OptIn(ExperimentalFoundationApi::class)
private class FootScope(
    private val real: LazyListScope,
    private val wrap: @Composable (@Composable () -> Unit) -> Unit,
) : LazyListScope by real {
    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        val row = wrap
        real.item(key, contentType) { val scope = this; row { scope.content() } }
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        val row = wrap
        real.items(count, key, contentType) { index -> val scope = this; row { scope.itemContent(index) } }
    }
}

/**
 * One row of the list. A row that starts at or below the foot is not drawn and takes no press (see
 * [PagedColumn]); here it also says nothing of itself, so a screen reader does not find a row the
 * page does not show. Where the row stands is noted as it is placed and only "hidden or not" is
 * read in composition, so a page turn composes a row again only when that changes.
 */
@Composable
private fun FootRow(state: LazyListState, cutRow: Boolean, align: Alignment.Horizontal, content: @Composable () -> Unit) {
    var top by remember { mutableStateOf(Int.MIN_VALUE) }
    val shows by rememberUpdatedState(cutRow)
    val hidden by remember(state) {
        derivedStateOf {
            val foot = state.layoutInfo.view(shows).foot
            foot != null && top >= foot
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { top = it.positionInParent().y.roundToInt() }
            .then(if (hidden) Modifier.clearAndSetSemantics { } else Modifier),
        horizontalAlignment = align,
    ) { content() }
}

/**
 * How far a list inside a dialog may run out into the dialog's own padding. The dialog sets it
 * (see its `EInkDialog`); everywhere else it is zero and a list is just its own width.
 */
val LocalListBleed = compositionLocalOf { 0.dp }

private const val TAIL = "paged-column-tail"

/** One laid-out row: its index, and its top and bottom in pixels down the list's viewport. */
internal data class RowSpan(val index: Int, val top: Int, val bottom: Int)

/**
 * A page as laid out: its [rows] (the tail left out), the [height] rows may fill, and how many
 * rows the list has, [count]. [showsCut]: the row cut at the foot is drawn (see [PagedColumn]).
 */
internal data class PageView(val rows: List<RowSpan>, val height: Int, val count: Int, val showsCut: Boolean = false) {
    /** The first row that runs past the foot, if any (a row taller than the page included). */
    private val cut: RowSpan? get() = rows.firstOrNull { it.bottom > height }

    /** Where drawing stops: the top of a row cut at the foot, or null to draw it all. */
    val foot: Int?
        get() {
            if (showsCut) return null
            val c = cut ?: return null
            // A row taller than the page, alone on it, is shown as far as it goes.
            return if (c == rows.first() || c.top <= 0) null else c.top
        }

    /** Whether the rows take more than one page. */
    val pages: Boolean
        get() = rows.isNotEmpty() && (rows.first().index > 0 || rows.first().top < 0 || cut != null || rows.last().index < count - 1)

    /** Whether this is the first page. */
    val atStart: Boolean get() = rows.isEmpty() || (rows.first().index == 0 && rows.first().top >= 0)

    /** Whether this is the last page: the last row is on it, whole. */
    val atEnd: Boolean get() = rows.isEmpty() || (cut == null && rows.last().index >= count - 1)

    /** The rows shown whole. */
    val whole: List<RowSpan> get() = rows.filter { it.top >= 0 && it.bottom <= height }
}

/** What a turn forward does. */
internal sealed interface Turn {
    /** Bring this row to the top. */
    data class To(val index: Int) : Turn
    /** Move this many pixels, within a row taller than the page. */
    data class By(val px: Int) : Turn
    data object None : Turn
}

/** Where a turn forward from [page] lands. */
internal fun forward(page: PageView): Turn {
    if (page.rows.isEmpty() || page.atEnd) return Turn.None
    val cut = page.rows.firstOrNull { it.bottom > page.height }
        ?: return Turn.To(page.rows.last().index + 1)
    // The row that does not fit is the one the page opens with: it is taller than a page, so the
    // next page is the next part of it. "Opens with", not "starts at the top": a list with
    // padding above its first row has that row start below 0, and it still must be read through.
    if (cut.index == page.rows.first().index) return Turn.By(minOf(page.height, cut.bottom - page.height).coerceAtLeast(1))
    return Turn.To(cut.index)
}

/**
 * After a turn back of one page height, [page] may open part-way into a row; that row was shown
 * whole on the page before, so start on the next one. Null to stay: a row taller than a page, or
 * the very top.
 */
internal fun backSnap(page: PageView): Int? {
    val first = page.rows.firstOrNull() ?: return null
    if (first.top >= 0) return null
    if (first.bottom - first.top > page.height) return null
    return page.rows.getOrNull(1)?.index
}

/**
 * Whether [page] is past the end of a list that got shorter under it: no row with anything in it
 * is on the page (only the blank tail, or rows of no height), the list has rows, and the page is
 * not the first. A turn never lands there (the last page opens on the last row), so only a list
 * that shrank does. Never with the cut row shown: that list has no tail and ends flush by itself.
 */
internal fun stranded(page: PageView): Boolean =
    !page.showsCut && page.count > 0 && page.height > 0 &&
        page.rows.none { it.index == 0 } && page.rows.none { it.bottom > it.top }

/**
 * Whether the bar must be decided again at the rows' full width (see [PagedColumn]): it is shown
 * ([barShown]) and the list has grown, taller or wider, from a size it had been laid out at. A list
 * that shrinks, or one with no bar, keeps what it has.
 */
internal fun recheckBar(barShown: Boolean, wasWidth: Int, wasHeight: Int, width: Int, height: Int): Boolean =
    barShown && wasWidth > 0 && wasHeight > 0 && (width > wasWidth || height > wasHeight)

/**
 * The row a tap at [y] on the scrollbar ([height] pixels) jumps to, with [visible] rows whole on
 * the page and [count] rows in all: the slider's middle comes to the tap. Null with no rows.
 */
internal fun jumpTarget(y: Float, height: Int, visible: Int, count: Int): Int? {
    if (count <= 0 || height <= 0) return null
    val slider = height * visible / count
    return ((y - slider / 2) / height * count).toInt().coerceIn(0, count - 1)
}

/** The scrollbar's thumb: its [height] and [offset] in pixels on a bar [barHeight] pixels tall, and whether it is [filled] (drawn). */
internal data class Thumb(val height: Float, val offset: Float, val filled: Boolean)

/**
 * Where the thumb sits. Normally it counts rows: [visible] whole rows of [PageView.count], the page
 * starting at row [first]. A list that is one row taller than the page (a reader-style screen, read
 * a page at a time) has nothing to count, so the thumb is measured in pixels: the page's share of
 * the row's height, placed by how far the row has been scrolled (the row's top is negative then).
 */
internal fun thumb(barHeight: Float, page: PageView, visible: Int, first: Int): Thumb {
    val tall = page.rows.singleOrNull()?.takeIf { page.count == 1 && it.bottom - it.top > page.height }
    val tallLength = tall?.let { (it.bottom - it.top).toFloat() }
    val height = (
        if (tallLength != null) barHeight * page.height / tallLength
        else barHeight * visible / page.count
    ).coerceAtLeast(max(barHeight * 0.05f, 16f))
    val maxOffset = (barHeight - height).coerceAtLeast(0f)
    val denominator = (page.count - visible).coerceAtLeast(1)
    val offset = (
        if (tall != null && tallLength != null) -tall.top / (tallLength - page.height) * maxOffset
        else first.toFloat() / denominator * maxOffset
    ).coerceIn(0f, maxOffset)
    return Thumb(height, offset, filled = tall != null || visible < page.count)
}

private fun LazyListLayoutInfo.view(cutRow: Boolean = false): PageView {
    val items = visibleItemsInfo.filter { it.contentType != TAIL }
    val rows = items.map { RowSpan(it.index, it.offset - viewportStartOffset, it.offset - viewportStartOffset + it.size) }
    // Without the cut row the list ends in the tail, which is not a row.
    val count = (totalItemsCount - if (cutRow) 0 else 1).coerceAtLeast(0)
    return PageView(rows, viewportSize.height - afterContentPadding, count, cutRow)
}

private suspend fun LazyListState.turnForward(cutRow: Boolean = false) {
    when (val t = forward(layoutInfo.view(cutRow))) {
        is Turn.To -> scrollToItem(t.index)
        is Turn.By -> scrollBy(t.px.toFloat())
        Turn.None -> Unit
    }
}

private suspend fun LazyListState.turnBack(cutRow: Boolean = false) {
    val page = layoutInfo.view(cutRow)
    if (page.atStart) return
    scrollBy(-page.height.toFloat())
    backSnap(layoutInfo.view(cutRow))?.let { scrollToItem(it) }
}

/**
 * Back to the last real page from past the end ([stranded]): the tail's top on the foot, so the
 * last row ends the page, then onto a whole row at the top, as a turn back does.
 */
private suspend fun LazyListState.resnap(cutRow: Boolean = false) {
    scrollToItem((layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    val info = layoutInfo
    val tail = info.visibleItemsInfo.lastOrNull { it.contentType == TAIL } ?: return
    val back = info.view(cutRow).height - (tail.offset - info.viewportStartOffset)
    if (back > 0) scrollBy(-back.toFloat())
    backSnap(layoutInfo.view(cutRow))?.let { scrollToItem(it) }
}

/**
 * The last row, which opens the last page: the tail under it leaves it at the top. With the cut
 * row shown there is no tail, and the list stops the last row flush with the foot.
 */
private fun LazyListState.lastPageStart(cutRow: Boolean = false): Int =
    (layoutInfo.totalItemsCount - if (cutRow) 1 else 2).coerceAtLeast(0)

/** MMD's scrollbar, as it draws it; see [PagedColumn]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageBar(
    state: LazyListState,
    cutRow: Boolean,
    clearBelow: Dp,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
    onJump: (Int) -> Unit,
) {
    val page = state.layoutInfo.view(cutRow)
    if (page.count == 0) return
    val previous = pageLabel("page_arrow_previous", "Previous page")
    val next = pageLabel("page_arrow_next", "Next page")
    val containerColor = MaterialTheme.colorScheme.onPrimary
    val sliderColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxHeight().padding(start = 8.dp, end = 8.dp, bottom = clearBelow),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = if (page.atStart) rememberVectorPainter(ChevronOutlineUp) else painterResource(com.mudita.mmd.R.drawable.chevron_filled_up),
            contentDescription = previous,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(24.dp)
                .combinedClickable(role = Role.Button, onClick = onBack, onLongClick = onFirst),
        )
        val whole = page.whole
        val visible = whole.size.coerceAtLeast(1)
        val first = whole.firstOrNull()?.index ?: page.rows.firstOrNull()?.index ?: 0
        // The tap handler is set up once; it reads the page as it is now, not as it was when the
        // bar was first drawn (a turn changes how many rows are whole on the page, not the count).
        val shownNow by rememberUpdatedState(visible)
        val countNow by rememberUpdatedState(page.count)
        val jump by rememberUpdatedState(onJump)
        Canvas(
            modifier = Modifier
                .width(8.dp)
                .weight(1f)
                .border(width = 1.dp, color = sliderColor, shape = RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        jumpTarget(offset.y, size.height, shownNow, countNow)?.let { jump(it) }
                    }
                },
        ) {
            val thumb = thumb(size.height, page, visible, first)
            val corner = CornerRadius(size.width / 2, size.width / 2)
            drawRoundRect(color = containerColor, topLeft = Offset.Zero, size = Size(size.width, size.height), cornerRadius = corner)
            drawRoundRect(
                color = if (thumb.filled) sliderColor else containerColor,
                topLeft = Offset(0f, thumb.offset),
                size = Size(size.width, thumb.height),
                cornerRadius = corner,
            )
        }
        Icon(
            painter = if (page.atEnd) rememberVectorPainter(ChevronOutlineDown) else painterResource(com.mudita.mmd.R.drawable.chevron_filled_down),
            contentDescription = next,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(24.dp)
                .combinedClickable(role = Role.Button, onClick = onForward, onLongClick = onLast),
        )
    }
}

/**
 * The words a screen reader says for a page arrow: the app's own string of that name (its
 * strings.xml, translated where the app is), or English when the app has none. Looked up by name
 * because this file is shared and cannot see an app's R; the name is a string in the code, so R8's
 * resource shrinker (default, non-strict) keeps the string. `copy.sh` adds both strings.
 */
@Composable
private fun pageLabel(name: String, english: String): String {
    val context = LocalContext.current
    return remember(name) {
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        if (id == 0) english else context.getString(id)
    }
}

/**
 * The arrow at an end the list cannot turn past (15 Nov 2026): MMD's filled chevron drawn as an
 * outline in the ink color, the same shape, size and place. MMD's own end chevron is dotted, which
 * e-ink shows as a grey dither.
 */
private val ChevronOutlineUp: ImageVector = chevronOutline(
    "ChevronOutlineUp",
    "M13.347,6.001C12.621,5.215 11.379,5.215 10.653,6.001L3.84,13.382C2.756,14.556 3.589,16.458 5.187,16.458H18.813C20.411,16.458 21.244,14.556 20.16,13.382L13.347,6.001Z",
)
private val ChevronOutlineDown: ImageVector = chevronOutline(
    "ChevronOutlineDown",
    "M10.653,17.999C11.379,18.785 12.621,18.785 13.347,17.999L20.16,10.618C21.244,9.444 20.411,7.542 18.813,7.542L5.187,7.542C3.589,7.542 2.756,9.444 3.84,10.618L10.653,17.999Z",
)

private fun chevronOutline(name: String, path: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(pathData = addPathNodes(path), stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f)
        .build()

/**
 * Calls [onPull] when a swipe that began with the list on its first page ends further down than
 * it began. It watches the touches before the list sees them and takes none of them, so the list
 * scrolls as it would anyway.
 */
private fun Modifier.pullAtTop(state: LazyListState, onPull: () -> Unit): Modifier = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val atTop = state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
        var last = down.position.y
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            last = change.position.y
            if (!change.pressed) break
        }
        if (swipeTurn(last - down.position.y, atTop, canPull = true) == SwipeTurn.Pull) onPull()
    }
}

/** What one swipe does to the list. */
internal enum class SwipeTurn { Forward, Back, Pull, None }

/**
 * What a swipe of [drag] pixels does: up turns a page on, down turns one back, and down on the
 * first page, [atTop], asks for the rows again if the list [canPull], or else does nothing.
 */
internal fun swipeTurn(drag: Float, atTop: Boolean, canPull: Boolean): SwipeTurn = when {
    drag < 0f -> SwipeTurn.Forward
    drag == 0f -> SwipeTurn.None
    !atTop -> SwipeTurn.Back
    canPull -> SwipeTurn.Pull
    else -> SwipeTurn.None
}
