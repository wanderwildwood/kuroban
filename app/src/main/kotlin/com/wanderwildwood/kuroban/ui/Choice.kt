package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonDefaultsMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

/*
 * One option of a pick-one set (the new-game choices, the Set up tools).
 *
 * A chosen option used to be a filled black button, the same fill as Play, so a row of
 * choices and the press that starts the game looked alike (Set up's "White" sat beside
 * "Play" as two identical black slabs). Fill now means one thing in this app: the press that
 * goes ahead. A choice stays white; the chosen one is told by its shape instead, the house
 * rule for state on E Ink: a heavy border, a check mark before the words, and the words in
 * bold. Three marks, none of them colour, so it reads in grey and at a glance.
 */

/** The border of a choice: MMD's own outline, made heavy for the chosen one. */
internal fun choiceBorderWidth(chosen: Boolean): Dp = if (chosen) CHOSEN_BORDER else ButtonDefaultsMMD.borderWidth

/** Heavier than MMD's 2 dp outline by enough to see across the room, not so heavy it reads as fill. */
internal val CHOSEN_BORDER: Dp = 3.5.dp

@Composable
internal fun ChoiceButton(
    label: String,
    chosen: Boolean,
    onClick: () -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ButtonDefaultsMMD.contentPadding,
) {
    val border = ButtonDefaultsMMD.outlinedButtonBorder(true).copy(width = choiceBorderWidth(chosen))
    OutlinedButtonMMD(
        onClick = onClick,
        // Said to a screen reader as the border says it to the eye: chosen or not. The tab role is
        // the one Android reports as "selected"; with a button or radio role the choice comes out
        // as "checked" and "selected" stays false.
        modifier = modifier.semantics {
            role = Role.Tab
            selected = chosen
        },
        border = border,
        contentPadding = contentPadding,
    ) {
        if (chosen) {
            CheckMark(Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        TextMMD(
            text = label,
            style = style,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** A drawn tick, so it does not depend on the font having the glyph. Decorative: the row says "selected". */
@Composable
private fun CheckMark(modifier: Modifier) {
    val ink = LocalContentColor.current
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val tick = Path().apply {
            moveTo(w * 0.08f, h * 0.55f)
            lineTo(w * 0.40f, h * 0.85f)
            lineTo(w * 0.94f, h * 0.15f)
        }
        drawPath(tick, ink, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
