package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmedTest {

    @Test
    fun aTapElsewhereDisarms() {
        val row = Any()
        assertTrue(disarmAfterGesture(row, row))
    }

    @Test
    fun theRowsOwnSecondTapIsNotSomethingElse() {
        assertFalse(disarmAfterGesture(Any(), null))
    }

    @Test
    fun armingAnotherRowIsNotADisarm() {
        assertFalse(disarmAfterGesture(Any(), Any()))
    }

    @Test
    fun nothingArmedNothingToDo() {
        assertFalse(disarmAfterGesture(null, null))
        assertFalse(disarmAfterGesture(null, Any()))
    }

    @Test
    fun firstPressArmsSecondRuns() {
        val slot = ArmedSlot<Unit>(ArmedScopeState())
        val flag = ArmedFlag(slot)
        var ran = 0
        flag.press { ran++ }
        assertTrue(flag.on)
        assertEquals(0, ran)
        flag.press { ran++ }
        assertFalse(flag.on)
        assertEquals(1, ran)
    }

    @Test
    fun noTimerStaysArmedUntilDisarmed() {
        val scope = ArmedScopeState()
        val flag = ArmedFlag(ArmedSlot(scope))
        flag.on = true
        Thread.sleep(50)
        assertTrue(flag.on)
        assertTrue(scope.isArmed)
        scope.disarm()
        assertFalse(flag.on)
    }

    @Test
    fun oneArmedRowPerScope() {
        val scope = ArmedScopeState()
        val a = ArmedFlag(ArmedSlot(scope))
        val b = ArmedFlag(ArmedSlot(scope))
        a.on = true
        b.on = true
        assertFalse(a.on)
        assertTrue(b.on)
        a.on = false // disarming a row that is not live leaves the live one alone
        assertTrue(b.on)
    }

    @Test
    fun aKeyedSlotArmsOneItem() {
        val slot = ArmedSlot<Long>(ArmedScopeState())
        var deleted: Long? = null
        slot.press(7L) { deleted = 7L }
        assertEquals(7L, slot.value)
        slot.press(9L) { deleted = 9L } // another item: re-arms on that one, runs nothing
        assertEquals(9L, slot.value)
        assertNull(deleted)
        slot.press(9L) { deleted = 9L }
        assertEquals(9L, deleted)
        assertNull(slot.value)
    }

    @Test
    fun withoutAScopeItStillHoldsUntilDisarmed() {
        val flag = ArmedFlag(ArmedSlot(null))
        flag.on = true
        assertTrue(flag.on)
        flag.disarm()
        assertFalse(flag.on)
    }

    @Test
    fun aFlagStandsInForAMutableState() {
        val state: androidx.compose.runtime.MutableState<Boolean> = ArmedFlag(ArmedSlot(ArmedScopeState()))
        val (armed, setArmed) = state
        assertFalse(armed)
        setArmed(true)
        assertTrue(state.value)
    }
}
