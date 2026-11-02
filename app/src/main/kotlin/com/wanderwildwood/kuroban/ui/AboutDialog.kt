package com.wanderwildwood.kuroban.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.kuroban.BuildConfig
import com.wanderwildwood.kuroban.R

/**
 * This app's About: only the words and the address. The dialog, its one Close button, the
 * Licences row, the links and the llama are the shared HouseAbout.kt, the same in every app.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val data = AboutData(
        title = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
        sections = listOf(
            AboutSection(stringResource(R.string.about_privacy_head), stringResource(R.string.about_privacy)),
            AboutSection(stringResource(R.string.about_terms_head), stringResource(R.string.about_terms)),
            AboutSection(null, stringResource(R.string.about_licence)),
            AboutSection(null, stringResource(R.string.about_gnugo)),
            AboutSection(null, stringResource(R.string.about_little_lions)),
        ),
        sourceUrl = SOURCE_URL,
        links = listOf(
            AboutLinkData("archive.org/details/81LittleLions", "https://archive.org/details/81LittleLions"),
        ),
    )
    val labels = AboutLabels(
        close = stringResource(R.string.about_close),
        licences = stringResource(R.string.licences_title),
        feedTheLlamas = stringResource(R.string.about_feed_llamas),
        noBrowser = { address -> context.getString(R.string.about_no_browser_address, address) },
    )
    HouseAboutDialog(data, labels, R.drawable.llama, onDismiss) { modifier, back ->
        LicencesScreen(rememberLicencesData(), rememberLicencesLabels(), onBack = back, modifier = modifier)
    }
}
