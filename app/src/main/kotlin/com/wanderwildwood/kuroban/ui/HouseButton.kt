package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

/*
 * The house button: the one press every app draws. The same file in every Compose app of this
 * shop (shared-housebutton/copy.sh); never edit an app's copy.
 *
 * It is MMD's own button, not a new look. ButtonDefaultsMMD already gives 8 dp corners and a 2 dp
 * black border and takes the ripple away; this only raises the height to the 48 dp touch rule,
 * picks filled (on) or outlined (off), and sets the words in bodySmall (16 sp, houseTypography), one line. No
 * strings live here: every label comes from the caller.
 */

/** The shortest a house button is, tall and wide: the 48 dp touch rule. */
internal val HOUSE_MIN: Dp = 48.dp

/** Side padding of a button's words: 14 dp, or 6 dp when [compact] (a row of many). */
internal fun houseSide(compact: Boolean): Dp = if (compact) 6.dp else 14.dp

/** The gap between buttons side by side. */
internal val HOUSE_GAP: Dp = 8.dp

/** The gap between the two buttons of a dialog footer (what the folio dialog already uses). */
internal val DIALOG_GAP: Dp = 10.dp

/** [selected] with [item] added if it was missing, taken out if it was there. */
internal fun <T> toggled(selected: Set<T>, item: T): Set<T> =
    if (item in selected) selected - item else selected + item

/** [selected] if it points at one of [count] options, else -1 (nothing chosen). */
internal fun validChoice(selected: Int, count: Int): Int = if (selected in 0 until count) selected else -1

/**
 * One press. [on] is filled black with white words (a chosen choice, a switch that is on, the
 * primary press); off is white with a black rim. At least 48 dp tall and wide whatever the words.
 * [note] is a second, smaller line under the label. [maxLines] lets a long label (a message to
 * dismiss, a word you typed yourself) wrap instead of being cut; the usual label is one line.
 */
@Composable
fun HouseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
    note: String? = null,
    maxLines: Int = 1,
) {
    val m = modifier.heightIn(min = HOUSE_MIN).widthIn(min = HOUSE_MIN)
    val pad = PaddingValues(horizontal = houseSide(compact), vertical = 8.dp)
    val words: @Composable RowScope.() -> Unit = {
        if (note == null) {
            TextMMD(text = label, style = MaterialTheme.typography.bodySmall, maxLines = maxLines, textAlign = TextAlign.Center)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextMMD(text = label, style = MaterialTheme.typography.bodySmall, maxLines = maxLines, textAlign = TextAlign.Center)
                TextMMD(text = note, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
    if (on) ButtonMMD(onClick = onClick, modifier = m, enabled = enabled, contentPadding = pad, content = words)
    else OutlinedButtonMMD(onClick = onClick, modifier = m, enabled = enabled, contentPadding = pad, content = words)
}

/** Pick one: the options side by side, equal width, the chosen one filled. */
@Composable
fun HouseChoiceRow(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val chosen = validChoice(selected, options.size)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HOUSE_GAP)) {
        options.forEachIndexed { i, label ->
            HouseButton(label, { onSelect(i) }, Modifier.weight(1f).semantics { this.selected = i == chosen }, on = i == chosen, compact = compact)
        }
    }
}

/** Pick any number: like [HouseChoiceRow], each press adds or removes its index from [selected]. */
@Composable
fun HouseMultiChoiceRow(
    options: List<String>,
    selected: Set<Int>,
    onChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HOUSE_GAP)) {
        options.forEachIndexed { i, label ->
            HouseButton(label, { onChange(toggled(selected, i)) }, Modifier.weight(1f).semantics { this.selected = i in selected }, on = i in selected, compact = compact)
        }
    }
}

/**
 * The foot of an [EInkDialog]: [cancel] and [confirm] side by side, equal width, 48 dp tall, 10 dp
 * apart, both outlined as the page-number dialog draws them. [confirmOn] fills the confirm
 * button (for the one press that goes ahead); [confirmEnabled] greys it until there is
 * something to confirm. With [onCancel] null the footer is a single full-width [confirm]
 * (a dialog that only says "Close").
 */
@Composable
fun DialogButtons(
    cancel: String,
    confirm: String,
    onCancel: (() -> Unit)?,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmOn: Boolean = false,
    confirmEnabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth()) {
        if (onCancel != null) {
            HouseButton(cancel, onCancel, Modifier.weight(1f))
            Spacer(Modifier.width(DIALOG_GAP))
        }
        HouseButton(confirm, onConfirm, Modifier.weight(1f), on = confirmOn, enabled = confirmEnabled)
    }
}
