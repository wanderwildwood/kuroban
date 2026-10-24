package com.wanderwildwood.kuroban.game

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the phone gives a short tick when a move registers. Off until asked for.
 *
 * On E Ink the picture lags behind the touch, so the tick says the tap landed before the
 * board catches up. It is only a remembered yes or no; the tick itself is asked of the phone
 * through the view (see GameScreen), so the phone's own touch-feedback setting still decides.
 */
class BuzzSetting(private val prefs: SharedPreferences) {

    private val state = MutableStateFlow(prefs.getBoolean(KEY, false))
    val on: StateFlow<Boolean> = state.asStateFlow()

    fun set(value: Boolean) {
        prefs.edit().putBoolean(KEY, value).apply()
        state.value = value
    }

    companion object {
        const val KEY = "buzz"
    }
}
