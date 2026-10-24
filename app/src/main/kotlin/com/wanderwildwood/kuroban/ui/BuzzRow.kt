package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.kuroban.R

/**
 * The one setting this app has: a short tick when a move registers. Off or On, the chosen one
 * solid and the other outlined, as the choices on the new-game screen are.
 */
@Composable
fun BuzzRow(on: Boolean, onChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextMMD(text = stringResource(R.string.buzz_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        TextMMD(text = stringResource(R.string.buzz_note), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (choice in listOf(false, true)) {
                val label: @Composable () -> Unit = {
                    TextMMD(
                        text = stringResource(if (choice) R.string.buzz_on else R.string.buzz_off),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
                val modifier = Modifier.weight(1f).height(44.dp)
                if (on == choice) {
                    ButtonMMD(onClick = { onChange(choice) }, modifier = modifier) { label() }
                } else {
                    OutlinedButtonMMD(onClick = { onChange(choice) }, modifier = modifier) { label() }
                }
            }
        }
    }
}
