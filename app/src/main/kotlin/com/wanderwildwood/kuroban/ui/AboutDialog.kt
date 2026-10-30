package com.wanderwildwood.kuroban.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.kuroban.BuildConfig
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.kuroban.R

/**
 * What this is, what it is made of, and where the source lives.
 *
 * The store listing links the repository, which is what the GPL actually requires. But
 * somebody who installed the APK and never saw a listing has no way to learn they have
 * source rights at all, and a licence nobody can find is not much of one.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    // A full screen of pages rather than a dialog: with Privacy, Terms and Licences it is longer
    // than a panel, and a page turns on whole rows like every other list here.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                PagedColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        TextMMD(
                            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    item {
                        TextMMD(
                            text = stringResource(R.string.about_privacy_head),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    item { TextMMD(text = stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodyMedium) }
                    item {
                        TextMMD(
                            text = stringResource(R.string.about_terms_head),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    item { TextMMD(text = stringResource(R.string.about_terms), style = MaterialTheme.typography.bodyMedium) }
                    item { LicencesLink() }
                    item { TextMMD(text = stringResource(R.string.about_licence), style = MaterialTheme.typography.bodyMedium) }
                    item { TextMMD(text = stringResource(R.string.about_gnugo), style = MaterialTheme.typography.bodyMedium) }
                    item { TextMMD(text = stringResource(R.string.about_little_lions), style = MaterialTheme.typography.bodyMedium) }
                    item {
                        val lionsContext = LocalContext.current
                        TextMMD(
                            text = "archive.org/details/81LittleLions",
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openLink(
                                        lionsContext,
                                        "https://archive.org/details/81LittleLions",
                                        lionsContext.getString(R.string.about_no_browser_address, "archive.org/details/81LittleLions"),
                                    )
                                }
                                .heightIn(min = 48.dp)
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                    }
                    item { Llama() }
                }
                OutlinedButtonMMD(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(48.dp),
                ) { TextMMD(text = stringResource(R.string.about_close), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to.
 *
 * Three words rather than an address: a verb and an object, so what happens when you press
 * them is not a surprise even though the page is not named. The drawing is his own, and it is
 * ink rather than an emoji, which is a colour glyph and reaches the panel as a pale smudge.
 * The site's address sits at the start of the same line and opens the site; the llama and its words
 * open the page.
 *
 * The Kompakt may have nothing registered for a web address, so the intent is allowed to fail
 * quietly rather than take the dialog down with it.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextMMD(
            text = "wanderthe.dev",
            style = MaterialTheme.typography.labelSmall,
            // The site's address opens the site, the way the llama beside it opens its page.
            modifier = Modifier
                .clickable {
                    openLink(context, "https://wanderthe.dev", context.getString(R.string.about_no_browser_address, "wanderthe.dev"))
                }
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
        )
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    // Straight to the checkout. The Donate button on the site only leads
                    // here anyway, so the page in between is a press the reader does not need.
                    // The short square.link form, not the long checkout.square.site address it
                    // redirects to -- the short one is what the site itself links to, so a
                    // regenerated checkout follows it and a published app does not break.
                    openLink(context, "https://square.link/u/AGu8oT10", context.getString(R.string.about_no_browser_address, "square.link/u/AGu8oT10"))
                }
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(R.string.about_feed_llamas), style = MaterialTheme.typography.labelSmall)
        }
    }
}
