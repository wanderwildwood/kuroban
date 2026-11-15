package com.wanderwildwood.kuroban.ui

import com.mudita.mmd.components.buttons.ButtonDefaultsMMD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoiceTest {
    @Test
    fun anOptionNotChosenKeepsMmdsOwnOutline() {
        assertEquals(ButtonDefaultsMMD.borderWidth, choiceBorderWidth(chosen = false))
    }

    @Test
    fun theChosenOptionHasAVisiblyHeavierBorder() {
        // At least 1 dp more than the plain outline, or the difference is lost on the panel.
        assertTrue(choiceBorderWidth(chosen = true).value - choiceBorderWidth(chosen = false).value >= 1f)
    }
}
