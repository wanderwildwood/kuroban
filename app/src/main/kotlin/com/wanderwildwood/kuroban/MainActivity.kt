package com.wanderwildwood.kuroban

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.kuroban.game.GameViewModel
import com.wanderwildwood.kuroban.ui.GameScreen
import com.wanderwildwood.kuroban.ui.NewGameScreen
import com.wanderwildwood.kuroban.ui.monochrome

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD(colorScheme = monochrome) {
                Kuroban()
            }
        }
    }
}

@Composable
private fun Kuroban(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val buzz by viewModel.buzz.on.collectAsStateWithLifecycle()

    // A short tick when a move registers, asked of the phone through the view so its own
    // touch-feedback setting still decides. The view model only sends one while Vibrate is on.
    val view = LocalView.current
    LaunchedEffect(viewModel) {
        viewModel.ticks.collect { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }

    when (val game = state) {
        null -> NewGameScreen(buzz = buzz, onBuzz = viewModel.buzz::set, onPlay = viewModel::newGame)
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
            buzz = buzz,
            onBuzz = viewModel.buzz::set,
        )
    }
}
