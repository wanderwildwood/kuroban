package com.wanderwildwood.kuroban.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

/**
 * Opens [url] in a real browser, and says so in plain words when there is none.
 *
 * Only a real browser is asked: the system's bare WebView shell (org.chromium.webview_shell) answers
 * a web address on some phones and then crashes, so it counts as no browser. [noBrowser] is the
 * message shown instead, already worded by the caller (it usually carries the address, so that it
 * can be typed in by hand). Never a WebView here.
 *
 * A caller with a place to show the message gives [onNone] and shows it there until it is
 * dismissed: on this panel a Toast can be gone before it has been read (and an address is long).
 * Without [onNone] it is still a Toast.
 */
fun openLink(context: Context, url: String, noBrowser: String, onNone: ((String) -> Unit)? = null) {
    val say = onNone ?: { text -> Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
    if (!browserAvailable(context, intent)) {
        say(noBrowser)
        return
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        say(noBrowser)
    }
}

/**
 * Whether [intent] has a real browser to go to. With several browsers and none chosen as the default,
 * Android answers with its own chooser (the package "android"), which is not a browser but is a way
 * to one: then any real browser among the candidates is enough, and the chooser is shown.
 */
fun browserAvailable(context: Context, intent: Intent): Boolean {
    val pm = context.packageManager
    val answer = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    val candidates = if (answer == "android") {
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo?.packageName }
    } else {
        emptyList()
    }
    return canReachBrowser(answer, candidates)
}

/** The decision alone: [answer] is who Android picked, [candidates] everyone who could take the address. */
fun canReachBrowser(answer: String?, candidates: List<String?>): Boolean =
    if (answer == "android") candidates.any { isRealBrowser(it) } else isRealBrowser(answer)

/** Whether the package that answered a web address is something a person can browse with. */
fun isRealBrowser(packageName: String?): Boolean =
    packageName != null && packageName != "android" && !packageName.contains("webview_shell")
