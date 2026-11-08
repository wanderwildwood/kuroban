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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * The list every screen uses: Mudita's list look, with its scrollbar (a chevron at each end that
 * turns a page, a track that jumps to the place pressed, a thumb that shows where the page is), but
 * a page turns on WHOLE ROWS. MMD's own `LazyColumnMMD` moves four rows a turn whatever their
 * heights, so tall rows ran off the foot of a page (Sky's Today lost its credits that way). Here
 * (3 Nov 2026: pages end on whole rows; the scrollbar stays):
 *
 * - a turn forward brings the first row that did not fit to the top of the page;
 * - a row that does not fit at the foot is not drawn (nor pressable) until the next page;
 * - a blank tail after the last row lets that row come to the top, so the last page also starts on
 *   a row and is never shifted back to end flush, mid-row at its top;
 * - a turn back goes one page back and lands on a row.
 *
 * A row taller than the whole page is read a page at a time within it.
 *
 * The bar is drawn as MMD's `VerticalScrollbar` draws it (MMD 1.0.2, `LazyMMD.kt`, Apache-2.0,
 * Copyright 2025 The Android Open Source Project, modified by Mudita Sp. z o.o.), with its own
 * chevron drawables; only where a turn lands is ours.
 *
 * Called as `LazyColumnMMD` was. [isScrollbarVisible] is on by default; a list that must not show
 * one passes false. [folio] and [rowStep] are kept so every existing call compiles.
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
    val shown by remember(state) { derivedStateOf { state.layoutInfo.totalItemsCount > 1 } }
    val pages by remember(state) { derivedStateOf { state.layoutInfo.view().pages } }
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
                                scope.launch { if (drag < 0f) state.turnForward() else state.turnBack() }
                            }
                        }
                    }
                    // A row hidden at the foot takes no press either: what cannot be seen is not there.
                    .pointerInput(state) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val foot = state.layoutInfo.view().foot
                            if (foot != null && down.position.y >= foot) down.consume()
                        }
                    }
                    .drawWithContent {
                        // Read here, while drawing, so a page turn redraws without composing anything.
                        val foot = state.layoutInfo.view().foot
                        if (foot == null) drawContent() else clipRect(bottom = foot.toFloat()) { this@drawWithContent.drawContent() }
                    },
                state = state,
                contentPadding = padding,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                // The list turns its own pages.
                userScrollEnabled = false,
            ) {
                content()
                // The tail has no key of its own (8 Nov 2026): a list keeps its place on the key of its
                // first row, so a list that opened empty (its rows still loading) and so on the tail alone
                // would stay on the tail when the rows came in above it, a blank page. Known by its type.
                item(contentType = TAIL) { Spacer(Modifier.height(tail)) }
            }
        }
        if (pages && isScrollbarVisible) {
            PageBar(
                state = state,
                onBack = { scope.launch { state.turnBack() } },
                onForward = { scope.launch { state.turnForward() } },
                onFirst = { scope.launch { state.scrollToItem(0) } },
                onLast = { scope.launch { state.scrollToItem(state.lastPageStart()) } },
                onJump = { index -> scope.launch { state.scrollToItem(index) } },
            )
        }
    }
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
 * rows the list has, [count].
 */
internal data class PageView(val rows: List<RowSpan>, val height: Int, val count: Int) {
    /** The first row that runs past the foot, if any (a row taller than the page included). */
    private val cut: RowSpan? get() = rows.firstOrNull { it.bottom > height }

    /** Where drawing stops: the top of a row cut at the foot, or null to draw it all. */
    val foot: Int?
        get() {
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
    // next page is the next part of it.
    // "Opens with", not "starts at the top": a list with padding above its first row has that
    // row start below 0, and it still must be read through.
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

private fun LazyListLayoutInfo.view(): PageView {
    val items = visibleItemsInfo.filter { it.contentType != TAIL }
    val rows = items.map { RowSpan(it.index, it.offset - viewportStartOffset, it.offset - viewportStartOffset + it.size) }
    val count = (totalItemsCount - 1).coerceAtLeast(0)
    return PageView(rows, viewportSize.height - afterContentPadding, count)
}

private suspend fun LazyListState.turnForward() {
    when (val t = forward(layoutInfo.view())) {
        is Turn.To -> scrollToItem(t.index)
        is Turn.By -> scrollBy(t.px.toFloat())
        Turn.None -> Unit
    }
}

private suspend fun LazyListState.turnBack() {
    val page = layoutInfo.view()
    if (page.atStart) return
    scrollBy(-page.height.toFloat())
    backSnap(layoutInfo.view())?.let { scrollToItem(it) }
}

/** The last row, which opens the last page: the tail under it leaves it at the top. */
private fun LazyListState.lastPageStart(): Int = (layoutInfo.totalItemsCount - 2).coerceAtLeast(0)

/** MMD's scrollbar, as it draws it; see [PagedColumn]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageBar(
    state: LazyListState,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
    onJump: (Int) -> Unit,
) {
    val page = state.layoutInfo.view()
    if (page.count == 0) return
    val containerColor = MaterialTheme.colorScheme.onPrimary
    val sliderColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(if (page.atStart) com.mudita.mmd.R.drawable.chevron_dotted_up else com.mudita.mmd.R.drawable.chevron_filled_up),
            contentDescription = null,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(24.dp)
                .combinedClickable(onClick = onBack, onLongClick = onFirst),
        )
        val whole = page.whole
        val visible = whole.size.coerceAtLeast(1)
        val first = whole.firstOrNull()?.index ?: page.rows.firstOrNull()?.index ?: 0
        Canvas(
            modifier = Modifier
                .width(8.dp)
                .weight(1f)
                .border(width = 1.dp, color = sliderColor, shape = RoundedCornerShape(4.dp))
                .pointerInput(page.count) {
                    detectTapGestures { offset ->
                        val slider = size.height * visible / page.count
                        val target = ((offset.y - slider / 2) / size.height * page.count).toInt().coerceIn(0, page.count - 1)
                        onJump(target)
                    }
                },
        ) {
            val sliderHeight = (size.height * visible / page.count).coerceAtLeast(max(size.height * 0.05f, 16f))
            val maxOffset = (size.height - sliderHeight).coerceAtLeast(0f)
            val denominator = (page.count - visible).coerceAtLeast(1)
            val sliderOffset = (first.toFloat() / denominator * maxOffset).coerceIn(0f, maxOffset)
            val corner = CornerRadius(size.width / 2, size.width / 2)
            drawRoundRect(color = containerColor, topLeft = Offset.Zero, size = Size(size.width, size.height), cornerRadius = corner)
            drawRoundRect(
                color = if (visible >= page.count) containerColor else sliderColor,
                topLeft = Offset(0f, sliderOffset),
                size = Size(size.width, sliderHeight),
                cornerRadius = corner,
            )
        }
        Icon(
            painter = painterResource(if (page.atEnd) com.mudita.mmd.R.drawable.chevron_dotted_down else com.mudita.mmd.R.drawable.chevron_filled_down),
            contentDescription = null,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(24.dp)
                .combinedClickable(onClick = onForward, onLongClick = onLast),
        )
    }
}

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
