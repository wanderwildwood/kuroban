package com.wanderwildwood.kuroban.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The one colour scheme every app of this shop draws in.
 *
 * MMD's own `eInkColorScheme` is the source of every value here, and would be the whole of
 * this file but for how it is built.
 *
 * ⚠ **Not `eInkColorScheme`, and not `eInkColorScheme.copy(...)`.** MMD constructs its
 * scheme by calling the raw `ColorScheme(...)` constructor as it stood in Material 3 1.3.1,
 * which is what MMD was compiled against. Six roles are `Color.Unspecified` in MMD's own
 * source — `surfaceBright`, `surfaceDim`, and four of the `surfaceContainer` tiers — and
 * every role Material 3 has added since arrives unset on top of those. `copy()` cannot fill
 * what was never set.
 *
 * An unspecified colour does not fail loudly. It is packed zero over an unspecified colour
 * space, so it converts without complaint and paints **fully transparent**. That is why a
 * Material `AlertDialog`, whose container is `surfaceContainerHigh`, came up as text
 * floating over whatever was behind it, and why every `TextFieldMMD` in the shop lost the
 * fill it asks for. Neither threw; both just drew nothing.
 *
 * `lightColorScheme()` fills every role the *resolved* Material 3 has, so starting there and
 * overriding with MMD's values is version-proof.
 *
 * The container tiers are white on purpose rather than by default: Mudita's own component
 * demo draws a text field as white with a single rule beneath it, and a card and a dialog as
 * white inside a black edge. There is no grey in this palette because there is no grey in
 * theirs.
 */
val monochrome = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    inversePrimary = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Color.Black,
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = Color.Black,
    tertiaryContainer = Color.Black,
    onTertiaryContainer = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color.Black,
    surfaceTint = Color.White,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    error = Color.Black,
    onError = Color.White,
    errorContainer = Color.White,
    onErrorContainer = Color.Black,
    outline = Color.Black,
    outlineVariant = Color.Black,
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerLowest = Color.White,
)
