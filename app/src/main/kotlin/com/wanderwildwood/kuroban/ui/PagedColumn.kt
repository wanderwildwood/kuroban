package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.lazy.LazyColumnMMD

/**
 * The list every app uses: Mudita's own `LazyColumnMMD` with its scrollbar. The scrollbar has a
 * chevron at each end that turns a page, a track that jumps to the place pressed, and a thumb that
 * shows where in the list the page is, so a long list can be crossed in one press.
 *
 * Called as `LazyColumnMMD` was. [isScrollbarVisible] is on by default; a list that must not show
 * one passes false. [folio] and [rowStep] are kept so every existing call compiles: there is no
 * page number any more (the scrollbar says where you are), and a row taller than the screen is
 * read with the list's own paging.
 *
 * [onPullAtTop], when given, is called when a swipe down starts on the first page of the rows: the
 * list asks for them again. It asks for nothing otherwise, and does not take the swipe from the
 * list. Saying what the pull set going is the caller's, in words.
 *
 * Do not read state derived from the list's layout inside [content]: every layout then rebuilds
 * the items and asks for another, a loop that keeps a core busy while idle.
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
    LazyColumnMMD(
        modifier = (if (onPullAtTop == null) modifier else modifier.pullAtTop(state) { pull?.invoke(); Unit }).then(widen),
        state = state,
        contentPadding = padding,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        isScrollbarVisible = isScrollbarVisible,
        content = content,
    )
}

/**
 * How far a list inside a dialog may run out into the dialog's own padding. The dialog sets it
 * (see its `EInkDialog`); everywhere else it is zero and a list is just its own width.
 */
val LocalListBleed = compositionLocalOf { 0.dp }

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
