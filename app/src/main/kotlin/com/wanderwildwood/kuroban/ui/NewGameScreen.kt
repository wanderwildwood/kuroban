package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.kuroban.R
import com.wanderwildwood.kuroban.game.BOARD_SIZE
import com.wanderwildwood.kuroban.game.Difficulty
import com.wanderwildwood.kuroban.game.HANDICAPS
import com.wanderwildwood.kuroban.game.Point
import com.wanderwildwood.kuroban.game.handicapPoints
import com.wanderwildwood.kuroban.game.GameConfig
import com.wanderwildwood.kuroban.game.Opponent
import com.wanderwildwood.kuroban.game.Setup
import com.wanderwildwood.kuroban.game.Stone

/**
 * New game options, laid out the way Chess+ lays its own out: the opponent choice at the
 * top, and the settings that only mean something against the computer disappearing when
 * you pick two players.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameScreen(
    // Last time's choices: the screen opens on them.
    initial: GameConfig,
    // The four choices as they stand, to be remembered, and the game they start.
    onPlay: (picked: GameConfig, game: GameConfig) -> Unit,
) {
    var opponent by remember { mutableStateOf(initial.opponent) }
    var difficulty by remember { mutableStateOf(initial.difficulty) }
    var humanColor by remember { mutableStateOf(initial.humanColor) }
    var handicap by remember { mutableStateOf(initial.handicap) }
    val picked = GameConfig(opponent = opponent, difficulty = difficulty, humanColor = humanColor, handicap = handicap)
    var aboutOpen by remember { mutableStateOf(false) }
    val gnuGoLabel = stringResource(R.string.new_game_opponent_gnugo)
    val twoPlayersLabel = stringResource(R.string.new_game_opponent_two_players)
    val easyLabel = stringResource(R.string.new_game_difficulty_easy)
    val normalLabel = stringResource(R.string.new_game_difficulty_normal)
    val blackLabel = stringResource(R.string.stone_black)
    val whiteLabel = stringResource(R.string.stone_white)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.app_name)) },
                actions = { InfoButton(onClick = { aboutOpen = true }) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            ChoiceRow(
                label = stringResource(R.string.new_game_opponent),
                options = listOf(Opponent.COMPUTER, Opponent.HUMAN),
                selected = opponent,
                // Named, not "Computer". It is a particular engine with a particular
                // way of playing, it is the reason this app can count a finished game,
                // and its name is the honest answer to "what am I playing against?".
                optionLabel = { if (it == Opponent.COMPUTER) gnuGoLabel else twoPlayersLabel },
                onSelect = { opponent = it },
            )

            if (opponent == Opponent.COMPUTER) {
                Spacer(Modifier.height(22.dp))
                ChoiceRow(
                    label = stringResource(R.string.new_game_difficulty),
                    options = Difficulty.entries.toList(),
                    selected = difficulty,
                    optionLabel = {
                        when (it) {
                            Difficulty.EASY -> easyLabel
                            Difficulty.NORMAL -> normalLabel
                        }
                    },
                    onSelect = { difficulty = it },
                )

                Spacer(Modifier.height(22.dp))
                ChoiceRow(
                    label = stringResource(R.string.new_game_your_stones),
                    options = listOf(Stone.BLACK, Stone.WHITE),
                    selected = humanColor,
                    optionLabel = { if (it == Stone.BLACK) blackLabel else whiteLabel },
                    onSelect = { humanColor = it },
                )
            }

            Spacer(Modifier.height(22.dp))
            ChoiceRow(
                label = stringResource(R.string.new_game_handicap),
                options = HANDICAPS,
                selected = handicap,
                optionLabel = { "$it" },
                onSelect = { handicap = it },
            )
            Spacer(Modifier.height(10.dp))
            TextMMD(
                text = if (handicap == 0) {
                    stringResource(R.string.new_game_black_first)
                } else {
                    stringResource(R.string.new_game_handicap_explained, handicap)
                },
                style = MaterialTheme.typography.labelSmall,
            )

            // The empty middle of this screen was doing nothing; a board fills it and says
            // what the app is without a word of explanation. It is the board the game will
            // start on: empty, or with the handicap stones where GNU Go will put them, so it
            // changes with the choice above it rather than contradicting it. It takes whatever
            // room is going spare - which is most of the screen once two players is picked
            // and the computer settings go away - and bows out entirely when there is none,
            // rather than being wedged in at a size that looks like a mistake.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                val available = minOf(maxWidth, maxHeight)
                if (available >= 88.dp) {
                    StartingBoard(handicap = handicap, size = minOf(available, 200.dp))
                }
            }

            // Two ways to begin, sharing one row. A third labelled row would not fit -
            // four of them and the button already fill this screen on the Kompakt - and
            // setting a position up is a way of starting rather than a setting anyway.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButtonMMD(
                    onClick = {
                        onPlay(
                            picked,
                            GameConfig(
                                opponent = opponent,
                                difficulty = difficulty,
                                humanColor = humanColor,
                                // Placing the stones yourself is what a handicap does for
                                // you, so the two do not both happen.
                                handicap = 0,
                                setup = Setup(toMove = Stone.BLACK),
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                ) {
                    TextMMD(text = stringResource(R.string.new_game_set_up), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                ButtonMMD(
                    onClick = { onPlay(picked, picked) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                ) {
                    TextMMD(text = stringResource(R.string.new_game_play), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }

    if (aboutOpen) {
        AboutDialog(onDismiss = { aboutOpen = false })
    }
}

@Composable
private fun InfoButton(onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    val said = stringResource(R.string.cd_about)
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = said }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // The house Info icon (Material Symbols), 22 dp in a 48 dp box, as in every other app.
        Icon(
            imageVector = Icons.Info,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * The board a game starts on, small: the nine lines, the star points, and Black's handicap
 * stones if there are any, drawn the way the real board draws them.
 */
@Composable
private fun StartingBoard(handicap: Int, size: androidx.compose.ui.unit.Dp) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    val stones = handicapPoints(handicap)
    val said = if (stones.isEmpty()) {
        stringResource(R.string.new_game_board_empty)
    } else {
        stringResource(R.string.new_game_board_handicap, stones.size)
    }

    Canvas(
        modifier = Modifier
            .size(size)
            .keepColors() // its black stones stay black in dark mode
            .background(paper)
            .semantics { contentDescription = said },
    ) {
        val cell = this.size.minDimension / BOARD_SIZE
        val origin = cell / 2f
        val span = (BOARD_SIZE - 1) * cell
        val hairline = 1.dp.toPx()
        val rim = 2.dp.toPx()
        val radius = cell * 0.44f

        fun at(point: Point) = Offset(origin + point.col * cell, origin + point.row * cell)

        for (i in 0 until BOARD_SIZE) {
            val line = origin + i * cell
            drawLine(ink, Offset(origin, line), Offset(origin + span, line), hairline)
            drawLine(ink, Offset(line, origin), Offset(line, origin + span), hairline)
        }
        drawRect(ink, topLeft = Offset(origin, origin), size = Size(span, span), style = Stroke(rim))
        for (star in listOf(Point(2, 2), Point(6, 2), Point(4, 4), Point(2, 6), Point(6, 6))) {
            drawCircle(ink, radius = cell * 0.08f, center = at(star))
        }
        for (stone in stones) drawCircle(ink, radius = radius, center = at(stone))
    }
}

/**
 * A labelled row of mutually exclusive choices. Every option is outlined; the chosen one
 * has the heavy border, a check mark and bold words ([ChoiceButton]), so it does not wear
 * the solid fill that marks Play, the press that starts the game.
 */
@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextMMD(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (option in options) {
                ChoiceButton(
                    label = optionLabel(option),
                    chosen = option == selected,
                    onClick = { onSelect(option) },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                )
            }
        }
    }
}
