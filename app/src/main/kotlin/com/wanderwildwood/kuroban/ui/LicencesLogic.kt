package com.wanderwildwood.kuroban.ui

/** One piece of other people's work that ships in the app (or is fetched by it), and its licence. */
data class LicenceComponent(
    val name: String,
    val author: String,
    val licence: String,
    val url: String,
    /** A short line shown on the component's own page, e.g. where a downloaded file comes from. */
    val note: String? = null,
)

/** A licence's full text, bundled as a raw resource. [id] is a family id from [licenceFamilies]. */
data class LicenceText(val id: String, val title: String, val rawResId: Int)

/** A further group of components, e.g. files the app downloads and does not carry; they get a row each like the rest, [note] on their pages. */
data class ComponentGroup(val heading: String, val note: String?, val components: List<LicenceComponent>)

/** Everything the Licences page shows, all text already in the app's own strings. */
data class LicencesData(
    val appName: String,
    val sourceUrl: String,
    val licenceLine: String,
    val components: List<LicenceComponent>,
    val texts: List<LicenceText>,
    val extraGroups: List<ComponentGroup> = emptyList(),
)

/** The words on the screens, from the app's strings.xml. */
data class LicencesLabels(
    val title: String,
    val back: String,
    /** The words of the app page's link to its source code ("Source code"). */
    val sourceHeading: String,
    /** "%s needs no licence text here." filled with the licence name, for a licence with no bundled text. */
    val noText: (String) -> String,
    /** "No browser is installed. The address is %s." filled with the address. */
    val noBrowser: (String) -> String,
)

/** Family ids in the order their full texts are listed. */
val licenceFamilies: List<String> = listOf("gpl", "gpl2-cpe", "lgpl", "apache", "mpl", "mit", "bsd", "ofl", "unicode", "cc-by", "cc0", "pd")

/** The family a licence name belongs to ("GPL-3.0-or-later" is "gpl"), or null when it has no text to show. */
fun licenceFamily(name: String): String? {
    val n = name.lowercase()
    return when {
        "classpath" in n -> "gpl2-cpe"
        "lgpl" in n || "lesser general public" in n -> "lgpl"
        "gpl" in n || "general public license" in n || "general public licence" in n -> "gpl"
        "unicode" in n -> "unicode"
        "apache" in n -> "apache"
        "mpl" in n || "mozilla" in n -> "mpl"
        "mit" in n.split(Regex("[^a-z]+")) -> "mit"
        "bsd" in n -> "bsd"
        "ofl" in n || "open font" in n -> "ofl"
        "cc-by" in n || "cc by" in n -> "cc-by"
        "cc0" in n -> "cc0"
        "public domain" in n || n.startsWith("pd") -> "pd"
        else -> null
    }
}

/** Every family a licence string names: "Apache-2.0 and BSD" is apache and bsd. */
fun licenceFamilies(licence: String): List<String> =
    licence.split(Regex("\\s+(?:and|or|\\+|/)\\s+|,|;")).mapNotNull { licenceFamily(it.trim()) }.distinct()

/** Key of the app's own row and page. */
const val APP_ROW_KEY = "app"

/** One row of the list: [key] opens a page, [title] is the first line, [subtitle] the second. */
data class LicenceRow(val key: String, val title: String, val subtitle: String)

/** What one licence page shows. [url] is the source link (app) or website (library); [texts] are full texts to read, empty when [licence] needs none. */
data class LicencePage(
    val title: String,
    val author: String?,
    val licence: String,
    val url: String?,
    val note: String?,
    val texts: List<LicenceText>,
)

/** Every component, extra groups included (their group note carried onto each), alphabetical by name. */
fun allComponents(data: LicencesData): List<LicenceComponent> =
    (data.components + data.extraGroups.flatMap { g -> g.components.map { it.copy(note = it.note ?: g.note) } })
        .sortedWith(compareBy<LicenceComponent> { it.name.lowercase() }.thenBy { it.name })

/**
 * The short licence name for the app's row: the SPDX-style name in [line] ("GPL-3.0-only"), or built from
 * "GNU General Public License, version 3 only / or later", or the line itself when it names neither.
 */
fun appLicenceName(line: String): String {
    Regex("""\b(?:A|L)?GPL-\d(?:\.\d)?-(?:only|or-later)\b""").find(line)?.let { return it.value }
    val version = Regex("""[Gg]eneral [Pp]ublic [Ll]icen[cs]e,? version (\d)(?:\.\d)? (only|or later)""").find(line)
    if (version != null) return "GPL-${version.groupValues[1]}.0-" + if (version.groupValues[2] == "only") "only" else "or-later"
    return line
}

/** The list: the app itself first, then one row per component, alphabetical. */
fun licenceRows(data: LicencesData): List<LicenceRow> =
    listOf(LicenceRow(APP_ROW_KEY, data.appName, appLicenceName(data.licenceLine))) +
        allComponents(data).map { LicenceRow("lib:${it.name}", it.name, it.author) }

/** The full texts to show for a licence string, in the order its families are named; none when it needs none. */
fun textsForLicence(licence: String, texts: List<LicenceText>): List<LicenceText> =
    licenceFamilies(licence).mapNotNull { id -> texts.firstOrNull { it.id == id } }

/** The page behind the row [key], or null when no row has it. */
fun licencePage(data: LicencesData, key: String): LicencePage? =
    if (key == APP_ROW_KEY) {
        LicencePage(data.appName, null, data.licenceLine, data.sourceUrl, null, textsForLicence(appLicenceName(data.licenceLine), data.texts))
    } else {
        allComponents(data).firstOrNull { "lib:${it.name}" == key }?.let {
            LicencePage(it.name, it.author, it.licence, it.url, it.note, textsForLicence(it.licence, data.texts))
        }
    }

/** The texts in the house order (GPL first, then Apache, MPL, MIT, BSD, OFL, ...), one per id. */
fun orderedTexts(texts: List<LicenceText>): List<LicenceText> =
    texts.distinctBy { it.id }.sortedBy { t -> licenceFamilies.indexOf(t.id).let { if (it < 0) Int.MAX_VALUE else it } }

/** The components grouped by licence family, in the order of [orderedTexts]; "" holds the ones without a family. */
fun groupByLicence(components: List<LicenceComponent>): Map<String, List<LicenceComponent>> =
    components
        .flatMap { c -> licenceFamilies(c.licence).ifEmpty { listOf("") }.map { it to c } }
        .groupBy({ it.first }, { it.second })
        .toSortedMap(compareBy { licenceFamilies.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } })

/**
 * What is wrong with the data, as plain sentences; empty when it is fine. For the app's own test:
 * a licence with a full text to show but none bundled, a bundled text nothing uses, a repeated name.
 */
fun licencesProblems(data: LicencesData): List<String> {
    val problems = mutableListOf<String>()
    val all = data.components + data.extraGroups.flatMap { it.components }
    val used = (all.flatMap { licenceFamilies(it.licence) } + licenceFamilies(data.licenceLine)).toSet()
    val have = data.texts.map { it.id }.toSet()
    (used - have - setOf("cc-by", "cc0", "pd")).forEach { problems += "no full text for $it" }
    (have - used).forEach { problems += "text $it is not used by anything" }
    if (data.texts.map { it.id }.size != have.size) problems += "a licence text is listed twice"
    if (all.map { it.name }.size != all.map { it.name }.toSet().size) problems += "a component is listed twice"
    all.filterNot { it.url.startsWith("https://") }.forEach { problems += "${it.name}: link is not https" }
    if (!data.sourceUrl.startsWith("https://")) problems += "source link is not https"
    return problems
}

/**
 * A licence text as the paragraphs a reader turns pages by: blocks split at blank lines, each
 * joined into one row. No word is dropped or repeated by it.
 */
fun licenceParagraphs(text: String): List<String> =
    text
        .replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n"))
        .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }
