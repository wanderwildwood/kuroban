package com.wanderwildwood.kuroban

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.kuroban.game.GameViewModel
import com.wanderwildwood.kuroban.ui.ArmedScope
import com.wanderwildwood.kuroban.ui.GameScreen
import com.wanderwildwood.kuroban.ui.NewGameScreen
import com.wanderwildwood.kuroban.ui.houseTypography
import com.wanderwildwood.kuroban.ui.monochrome
import com.wanderwildwood.kuroban.ui.DarkSelection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD(colorScheme = monochrome, typography = houseTypography) { DarkSelection {
                ArmedScope { Kuroban() }
            } }
        }
    }
}

@Composable
private fun Kuroban(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val game = state) {
        null -> NewGameScreen(
            initial = remember { viewModel.lastChoices() },
            onPlay = { picked, config ->
                viewModel.rememberChoices(picked)
                viewModel.newGame(config)
            },
        )
        else -> GameScreen(
            state = game,
            onTap = viewModel::tap,
            onConfirm = viewModel::confirmMove,
            onPass = viewModel::pass,
            onHint = viewModel::hint,
            onUndo = viewModel::undo,
            onResign = viewModel::resign,
            onNewGame = viewModel::endGame,
            onSetupTap = viewModel::setupTap,
            onFirstToMove = viewModel::setFirstToMove,
            onPlayFromSetup = viewModel::startFromSetup,
        )
    }
}
