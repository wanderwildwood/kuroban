package com.wanderwildwood.kuroban.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkOpenerTest {
    @Test fun aBrowserIsABrowser() {
        assertTrue(isRealBrowser("org.mozilla.firefox"))
        assertTrue(isRealBrowser("com.android.chrome"))
        assertTrue(isRealBrowser("com.einkbro"))
    }

    @Test fun theWebViewShellIsNotOne() {
        assertFalse(isRealBrowser("org.chromium.webview_shell"))
        assertFalse(isRealBrowser("com.google.android.webview_shell"))
    }

    @Test fun nobodyAnsweringIsNotOne() {
        assertFalse(isRealBrowser(null))
        assertFalse(isRealBrowser("android"))
    }

    @Test
    fun severalBrowsersWithNoDefaultStillReachABrowser() {
        // Android answers with its own chooser ("android"); the browsers behind it count.
        assertTrue(canReachBrowser("android", listOf("org.mozilla.firefox", "com.einkbro")))
        assertTrue(canReachBrowser("android", listOf("org.chromium.webview_shell", "com.einkbro")))
    }

    @Test
    fun aChooserWithNoRealBrowserBehindItIsNoBrowser() {
        assertFalse(canReachBrowser("android", emptyList()))
        assertFalse(canReachBrowser("android", listOf("org.chromium.webview_shell", null)))
    }

    @Test
    fun aSingleAnswerIsJudgedByItself() {
        assertTrue(canReachBrowser("com.einkbro", emptyList()))
        assertFalse(canReachBrowser(null, emptyList()))
    }
}

