package com.wanderwildwood.kuroban.game

import java.io.File

/**
 * What was picked on the new-game screen last time, so the next game opens on it.
 *
 * Someone who plays the person opposite plays them again, and a handicap that suits two
 * players goes on suiting them. Only the four choices are kept: a position set up by hand
 * belongs to its game, not to the next one.
 */
class NewGameChoices(private val file: File) {

    fun load(): GameConfig = try {
        file.takeIf { it.isFile }?.readText()?.let(::parseChoices) ?: GameConfig()
    } catch (_: Exception) {
        GameConfig()
    }

    fun save(choices: GameConfig) {
        try {
            file.writeText(formatChoices(choices))
        } catch (_: Exception) {
            // Forgetting a choice costs a tap next time; nothing worth stopping for.
        }
    }
}

/** The handicaps the new-game screen offers. */
val HANDICAPS = listOf(0, 2, 3, 4, 5)

internal fun formatChoices(choices: GameConfig): String = listOf(
    choices.opponent.name,
    choices.difficulty.name,
    choices.humanColor.name,
    choices.handicap.toString(),
).joinToString(" ")

/** The choices written by [formatChoices], or null when the text is not that. */
internal fun parseChoices(text: String): GameConfig? {
    val parts = text.trim().split(" ")
    if (parts.size != 4) return null
    val opponent = Opponent.entries.firstOrNull { it.name == parts[0] } ?: return null
    val difficulty = Difficulty.entries.firstOrNull { it.name == parts[1] } ?: return null
    val colour = Stone.entries.firstOrNull { it.name == parts[2] } ?: return null
    val handicap = parts[3].toIntOrNull()?.takeIf { it in HANDICAPS } ?: return null
    return GameConfig(opponent = opponent, difficulty = difficulty, humanColor = colour, handicap = handicap)
}

/**
 * Where GNU Go's `fixed_handicap` puts Black's stones on a 9x9 board: the 3-3 points,
 * lower left and upper right first, then upper left, then lower right, and tengen for five.
 */
fun handicapPoints(handicap: Int): List<Point> {
    if (handicap < 2) return emptyList()
    val corners = listOf(Point(2, 6), Point(6, 2), Point(2, 2), Point(6, 6))
    return corners.take(minOf(handicap, 4)) + if (handicap == 5) listOf(Point(4, 4)) else emptyList()
}
