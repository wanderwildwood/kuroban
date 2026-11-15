package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.Canvas
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
    // As the board shows them: a black stone black, a white stone white, in light and in dark.
    // No patch of paper behind it (a white square on the dark page). Dark mode turns the page
    // over, ink for paper, so there the two drawings change places: the black stone is the
    // paper-filled disc, shown black, with the rim that parts it from the dark page, and the
    // white stone the solid ink disc, shown white.
    val rimmed = (stone != Stone.BLACK) != DarkMode.on
    Canvas(modifier = Modifier.size(size)) {
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
