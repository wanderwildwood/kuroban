package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.kuroban.R

/**
 * The line in About that opens the Licences page: where the source is, the licence this app is
 * under, what it is built with, and the full text of each licence. A full-screen dialog, so Back
 * returns to About.
 */
@Composable
fun LicencesLink() {
    var open by rememberSaveable { mutableStateOf(false) }
    TextMMD(
        text = stringResource(R.string.licences_title),
        style = MaterialTheme.typography.bodyMedium,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { open = true }
            .wrapContentHeight(Alignment.CenterVertically),
    )
    if (open) {
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    LicencesScreen(rememberLicencesData(), rememberLicencesLabels(), onBack = { open = false }, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
