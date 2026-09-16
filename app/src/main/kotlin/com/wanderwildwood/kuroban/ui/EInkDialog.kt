package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * A dialog with no dimmed backdrop. The same file in every app of this shop.
 *
 * The stock scrim is a translucent black over the whole window, which on E Ink is not a
 * shadow but a screenful of dithered grey — it repaints everything behind the dialog and
 * leaves it muddy on the way out. A plain white panel with a black rim reads far better and
 * costs one partial refresh instead of a full one.
 *
 * Material's own `AlertDialog` is wrong here twice over: it animates in, and it sizes itself
 * to its buttons, so a dialog asking a short question and a dialog asking a long one read as
 * two different dialogs. This one is always the same width.
 *
 * MMD has no dialog of its own, which is why this is house rather than library. Everything
 * inside it should still be MMD — `TextMMD` for the words, `OutlinedButtonMMD` for a press.
 */
@Composable
fun EInkDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                content = content,
            )
        }
    }
}
