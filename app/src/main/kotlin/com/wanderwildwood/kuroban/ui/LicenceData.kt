package com.wanderwildwood.kuroban.ui

/** One piece of other people's work that ships inside the app (or is fetched by it), and its licence. */
data class Library(
    val name: String,
    val by: String,
    val licence: String,
    val link: String,
)

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/kuroban"

/** The unmodified GNU Go 3.8 source tarball and the script that builds it, in the app's repository. */
const val GNUGO_SOURCE_URL = "https://github.com/wanderwildwood/kuroban/tree/main/engine"

/** GNU Go's own page. */
const val GNUGO_SITE_URL = "https://www.gnu.org/software/gnugo/"

/** Everything the APK carries, or fetches, that this project did not write. */
val LIBRARIES: List<Library> =
    listOf(
        Library("GNU Go 3.8, the opponent (unmodified, built into the app)", "Free Software Foundation", "GPL-3.0", "https://www.gnu.org/software/gnugo/"),
        Library("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
        Library("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
        Library("Jetpack Compose (UI, Foundation, Material 3), Activity, Lifecycle, Core", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
        Library("Kotlin standard library and coroutines", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
    )

/**
 * A licence text as the paragraphs a reader turns pages by: the texts are wrapped at about
 * seventy columns with a blank line between paragraphs; each paragraph is joined into one row.
 */
fun licenceParagraphs(text: String): List<String> =
    text
        .replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n"))
        .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }
