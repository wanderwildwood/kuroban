package com.wanderwildwood.kuroban.ui

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.kuroban.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The row in About that opens the Licences page: a row of its own, 48 dp tall, with no rule.
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
    if (open) LicencesScreen(onBack = { open = false })
}

/**
 * The Licences page: where the source is, the licence this app is under, what it is built with,
 * and the full text of each licence once. The texts are raw resources, paged a paragraph to a
 * row like every list here. A full-screen dialog, so Back returns to About.
 */
@Composable
fun LicencesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val gpl by produceTexts(context, R.raw.gpl_3_0)
    val apache by produceTexts(context, R.raw.apache_2_0)
    val ofl by produceTexts(context, R.raw.ofl_1_1)

    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                TextMMD(
                    text = stringResource(R.string.licences_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                PagedColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Heading(stringResource(R.string.licences_source_heading)) }
                    item { Link(stringResource(R.string.licences_source, SOURCE_URL.removePrefix("https://")), SOURCE_URL) }
                    item { Heading(stringResource(R.string.licences_licence_heading)) }
                    item { Paragraph(stringResource(R.string.licences_licence)) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Heading(stringResource(R.string.licences_gnugo_heading))
                            Paragraph(stringResource(R.string.licences_gnugo))
                        }
                    }
                    item { Link(stringResource(R.string.licences_gnugo_source, GNUGO_SOURCE_URL.removePrefix("https://")), GNUGO_SOURCE_URL) }
                    item { Link(stringResource(R.string.licences_gnugo_site, GNUGO_SITE_URL.removePrefix("https://")), GNUGO_SITE_URL) }
                    item { Heading(stringResource(R.string.licences_built_heading)) }
                    LIBRARIES.forEach { library ->
                        item { Link("${library.name}, ${library.by}: ${library.licence}", library.link) }
                    }
                    item { Heading(stringResource(R.string.licences_gpl)) }
                    gpl.forEach { item { Paragraph(it) } }
                    item { Heading(stringResource(R.string.licences_apache)) }
                    apache.forEach { item { Paragraph(it) } }
                    item { Heading(stringResource(R.string.licences_ofl)) }
                    ofl.forEach { item { Paragraph(it) } }
                }
                OutlinedButtonMMD(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).height(48.dp),
                ) { TextMMD(text = stringResource(R.string.licences_close), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun produceTexts(context: Context, @RawRes resource: Int): State<List<String>> =
    produceState(initialValue = emptyList<String>(), resource) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    licenceParagraphs(context.resources.openRawResource(resource).bufferedReader().use { it.readText() })
                }.getOrDefault(emptyList())
            }
    }

@Composable
private fun Heading(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Paragraph(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodyMedium)
}

/** A line that opens an address in a browser; a phone with none says so rather than doing nothing. */
@Composable
private fun Link(text: String, address: String) {
    val context = LocalContext.current
    val none = stringResource(R.string.licences_no_browser, address.removePrefix("https://"))
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable {
                openLink(context, address, none)
            }
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
