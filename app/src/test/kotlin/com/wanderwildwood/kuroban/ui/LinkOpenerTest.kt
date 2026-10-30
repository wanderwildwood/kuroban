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
}
