package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wanderwildwood.kuroban.game.Stone

/**
 * A stone, small, for use in a line of text.
 *
 * Drawn rather than typed: the ● and ○ characters land at different sizes and baselines
 * depending on which font ends up serving them, and a pair that should read as a matched
 * set instead reads as a mistake.
 */
@Composable
fun StoneGlyph(stone: Stone, size: Dp = 13.dp) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    // Drawn as the board draws it, solid black and rimmed white, and like the board it keeps
    // its colors in dark mode, on a patch of the board's paper (unseen on the light page).
    val rimmed = stone != Stone.BLACK
    Canvas(modifier = Modifier.size(size).keepColors().background(paper)) {
        val rim = 1.5.dp.toPx()
        val radius = this.size.minDimension / 2f - rim / 2f
        if (!rimmed) {
            drawCircle(ink, radius = radius)
        } else {
            drawCircle(paper, radius = radius)
            drawCircle(ink, radius = radius, style = Stroke(rim))
        }
    }
}
