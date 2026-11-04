package com.wanderwildwood.kuroban.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

/*
 * The "tap again" question (STYLE.md, "Asking before something irreversible").
 *
 * The first tap arms a row and changes what it says; a second tap on it does the thing. The row
 * stays armed, with no timer, until the person does something else: a tap or a drag anywhere
 * else, Back, leaving the screen, or the screen going off (the activity pausing). Mudita's review
 * of 4 Nov 2026 found any countdown too short on e-ink, where the panel takes a second or two
 * just to show the question.
 *
 * Put ArmedScope once around the activity's content (inside the theme), and around the body of
 * any dialog that has an armed row of its own. Inside it, every armed row in the window shares
 * one slot, so arming one disarms any other.
 */

/** Which armed row, if any, is live in one [ArmedScope]. */
@Stable
class ArmedScopeState {
    internal var live: ArmedSlot<*>? by mutableStateOf(null)

    /** True while some row in this scope is armed. */
    val isArmed: Boolean get() = live != null

    /** Disarms whichever row is armed. */
    fun disarm() {
        live = null
    }
}

/**
 * One armed question that carries a key: which item of a list is armed, or which of several
 * actions. `null` is disarmed. Read and write [value], or use it as a delegate:
 * `var armedId by rememberArmedKey<Long>()`. It is a [MutableState], so it can stand in for one.
 */
@Stable
class ArmedSlot<T : Any> internal constructor(private val scope: ArmedScopeState?) : MutableState<T?> {
    private var held: T? by mutableStateOf(null)

    override var value: T?
        get() = if (scope == null || scope.live === this) held else null
        set(v) {
            held = v
            if (scope == null) return
            if (v != null) scope.live = this else if (scope.live === this) scope.live = null
        }

    override fun component1(): T? = value
    override fun component2(): (T?) -> Unit = { value = it }

    fun disarm() {
        value = null
    }

    /** First press arms [key]; a press on the same key while it is armed disarms and runs [action]. */
    fun press(key: T, action: () -> Unit) {
        if (value == key) {
            value = null
            action()
        } else {
            value = key
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, v: T?) {
        value = v
    }
}

/**
 * A plain yes/no armed question: `var armed by rememberArmedFlag()`. It is a
 * [MutableState]<Boolean>, so a helper that returned one can return this instead.
 */
@Stable
class ArmedFlag internal constructor(private val slot: ArmedSlot<Unit>) : MutableState<Boolean> {
    override var value: Boolean
        get() = slot.value != null
        set(v) {
            slot.value = if (v) Unit else null
        }

    override fun component1(): Boolean = value
    override fun component2(): (Boolean) -> Unit = { value = it }

    /** The same as [value]. */
    var on: Boolean
        get() = value
        set(v) {
            value = v
        }

    fun disarm() {
        value = false
    }

    /** First press arms; the second disarms and runs [action]. */
    fun press(action: () -> Unit) = slot.press(Unit, action)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, v: Boolean) {
        value = v
    }
}

/** The scope armed rows below join; `null` outside any [ArmedScope]. */
val LocalArmedScope = staticCompositionLocalOf<ArmedScopeState?> { null }

/**
 * Watches every gesture in its window. A gesture that began with a row armed and ended with that
 * same row still armed (it was not the row's own second tap, which disarms it, nor a tap that
 * armed another) disarms it. That is a tap elsewhere, a scroll, a long press, a drag on the row.
 */
@Composable
fun ArmedScope(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { ArmedScopeState() }
    val coroutines = rememberCoroutineScope()
    DisarmWhenPaused { state.disarm() }
    Box(
        modifier.pointerInput(state) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val atDown = state.live
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                } while (event.changes.any { it.pressed })
                // Checked after the gesture's own clicks have run, so the row's second tap
                // (which disarms it) is seen as that and not as "something else".
                if (atDown != null) coroutines.launch {
                    if (disarmAfterGesture(atDown, state.live)) state.live = null
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalArmedScope provides state) { content() }
    }
}

/**
 * An armed question carrying a key; see [ArmedSlot]. [keys] start it afresh (disarmed) when they
 * change, as `remember(keys)` did. [disarmOnBack] false is for a question that Back itself arms
 * ("Discard changes — Back again"): the screen's own BackHandler then answers it.
 */
@Composable
fun <T : Any> rememberArmedKey(vararg keys: Any?, disarmOnBack: Boolean = true): ArmedSlot<T> {
    val scope = LocalArmedScope.current
    val slot = remember(scope, *keys) { ArmedSlot<T>(scope) }
    BackHandler(enabled = disarmOnBack && slot.value != null) { slot.disarm() }
    DisarmWhenPaused { slot.disarm() }
    DisposableEffect(slot) { onDispose { slot.disarm() } }
    return slot
}

/** A yes/no armed question; see [ArmedFlag] and [rememberArmedKey]. */
@Composable
fun rememberArmedFlag(vararg keys: Any?, disarmOnBack: Boolean = true): ArmedFlag {
    val slot = rememberArmedKey<Unit>(*keys, disarmOnBack = disarmOnBack)
    return remember(slot) { ArmedFlag(slot) }
}

/** Runs [disarm] when the activity pauses: the screen went off, or the person left it. */
@Composable
private fun DisarmWhenPaused(disarm: () -> Unit) {
    val owner = LocalView.current.findViewTreeLifecycleOwner()
    val current by rememberUpdatedState(disarm)
    DisposableEffect(owner) {
        val watcher = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) current()
        }
        owner?.lifecycle?.addObserver(watcher)
        onDispose { owner?.lifecycle?.removeObserver(watcher) }
    }
}

/** A gesture that began with [atDown] armed and ended with [now] armed disarms only if they are the same row. */
internal fun disarmAfterGesture(atDown: Any?, now: Any?): Boolean = atDown != null && atDown === now
