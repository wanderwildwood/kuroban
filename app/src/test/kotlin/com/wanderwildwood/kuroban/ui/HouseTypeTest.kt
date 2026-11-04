package com.wanderwildwood.kuroban.ui

import androidx.compose.ui.unit.sp
import com.mudita.mmd.eInkTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseTypeTest {

    private fun all(t: androidx.compose.material3.Typography) = listOf(
        "displayLarge" to t.displayLarge, "displayMedium" to t.displayMedium,
        "displaySmall" to t.displaySmall, "headlineLarge" to t.headlineLarge,
        "headlineMedium" to t.headlineMedium, "headlineSmall" to t.headlineSmall,
        "titleLarge" to t.titleLarge, "titleMedium" to t.titleMedium, "titleSmall" to t.titleSmall,
        "bodyLarge" to t.bodyLarge, "bodyMedium" to t.bodyMedium, "bodySmall" to t.bodySmall,
        "labelLarge" to t.labelLarge, "labelMedium" to t.labelMedium, "labelSmall" to t.labelSmall,
    )

    @Test
    fun nothingIsBelowTheFloor() {
        for ((name, style) in all(houseTypography)) {
            assertTrue("$name is ${style.fontSize}", style.fontSize.value >= HOUSE_TYPE_FLOOR_SP)
        }
    }

    @Test
    fun theThreeSmallStepsAreSixteen() {
        assertEquals(16.sp, houseTypography.labelSmall.fontSize)
        assertEquals(16.sp, houseTypography.labelMedium.fontSize)
        assertEquals(16.sp, houseTypography.bodySmall.fontSize)
    }

    @Test
    fun theRestIsMmdUnchanged() {
        val mmd = all(eInkTypography).toMap()
        for ((name, style) in all(houseTypography)) {
            if (name in setOf("labelSmall", "labelMedium", "bodySmall")) continue
            assertEquals(name, mmd.getValue(name), style)
        }
    }

    @Test
    fun onlyTheSizeMoves() {
        val raised = houseTypography.labelSmall
        val mmd = eInkTypography.labelSmall
        assertEquals(mmd.fontFamily, raised.fontFamily)
        assertEquals(mmd.fontWeight, raised.fontWeight)
        assertEquals(mmd.lineHeight, raised.lineHeight)
    }

    // A 16 sp line of Lato (1.2 em, MMD sets no line height) on the Kompakt (213 dpi) is
    // 25.6 px: Mudita's 24 px with room to spare. 14 sp was 22.4 px.
    @Test
    fun aLineIsAtLeastTwentyFourPixelsOnTheKompakt() {
        val px = HOUSE_TYPE_FLOOR_SP * 213f / 160f * 1.2f
        assertTrue("$px px", px >= 24f)
    }
}
