package com.wanderwildwood.kuroban.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mudita.mmd.eInkTypography

/*
 * The house type scale: MMD's eInkTypography with its three small steps raised to 16 sp. The same
 * file in every Compose app of this shop (shared-housetype/copy.sh); never edit an app's copy.
 * It is handed to the theme once, `ThemeMMD(colorScheme = monochrome, typography = houseTypography)`,
 * so every `MaterialTheme.typography.labelSmall` in the app, and every MMD part that reads one
 * (a text field's supporting line, a badge, a bottom bar's labels), follows it.
 *
 * Why (Mudita's review, 4 Nov 2026): helper text measured 22 px on the Kompakt, and Mudita asks
 * for at least 24 px. The Kompakt draws at 213 dpi (1 sp = 1.33 px at font scale 1). MMD sets no
 * line height, so a line of Lato is 1.2 times its size (hhea 1974 + 426 over 2000 units): a 14 sp
 * line is 22.4 px tall, which is the 22 px Mudita read; 15 sp is 23.96 px, on the line; 16 sp is
 * 25.6 px, clear of it. So nothing in the house scale is smaller than 16 sp.
 *
 * MMD 1.0.2 scale, and what changes:
 *   headlineLarge 28   titleLarge 24   titleMedium 20   bodyLarge 20
 *   bodyMedium 18      labelLarge 18   titleSmall 16
 *   bodySmall 15 -> 16   labelMedium 15 -> 16   labelSmall 14 -> 16
 * bodySmall (a house button's words) rises with labelSmall (its note line) so a note is never
 * larger than the words above it. Weight (Lato Medium), family and the unset line height stay
 * MMD's, so a row grows by the taller line only: 2.4 dp a line for labelSmall, 1.2 dp for the
 * other two. PagedColumn measures rows as drawn, so whole-row pages need nothing.
 */

/** The smallest size the house scale sets, in sp. Type below it needs a written reason (STYLE.md). */
const val HOUSE_TYPE_FLOOR_SP: Float = 16f

/** The house floor as a TextUnit, for the rare call that must set a size of its own. */
val HouseTypeFloor: TextUnit = HOUSE_TYPE_FLOOR_SP.sp

/**
 * MMD's typography with no style below [HOUSE_TYPE_FLOOR_SP]. Every style MMD leaves at the
 * Material default (display*, headlineMedium, headlineSmall) is already far above it.
 */
val houseTypography: Typography = raiseToFloor(eInkTypography)

/** [base] with every style smaller than the floor set to the floor; the rest untouched. */
internal fun raiseToFloor(base: Typography): Typography {
    fun up(s: androidx.compose.ui.text.TextStyle) =
        if (s.fontSize.isSp && s.fontSize.value < HOUSE_TYPE_FLOOR_SP) s.copy(fontSize = HouseTypeFloor) else s
    return base.copy(
        displayLarge = up(base.displayLarge), displayMedium = up(base.displayMedium),
        displaySmall = up(base.displaySmall), headlineLarge = up(base.headlineLarge),
        headlineMedium = up(base.headlineMedium), headlineSmall = up(base.headlineSmall),
        titleLarge = up(base.titleLarge), titleMedium = up(base.titleMedium),
        titleSmall = up(base.titleSmall), bodyLarge = up(base.bodyLarge),
        bodyMedium = up(base.bodyMedium), bodySmall = up(base.bodySmall),
        labelLarge = up(base.labelLarge), labelMedium = up(base.labelMedium),
        labelSmall = up(base.labelSmall),
    )
}
