package com.wanderwildwood.kuroban.ui

/** One piece of other people's work that ships in the app (or is fetched by it), and its licence. */
data class LicenceComponent(val name: String, val author: String, val licence: String, val url: String)

/** A licence's full text, bundled as a raw resource. [id] is a family id from [licenceFamilies]. */
data class LicenceText(val id: String, val title: String, val rawResId: Int)

/** A further list on the short page, e.g. files the app downloads and does not carry. */
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

/** The words on the two screens, from the app's strings.xml. */
data class LicencesLabels(
    val title: String,
    val back: String,
    val sourceHeading: String,
    val licenceHeading: String,
    val builtWithHeading: String,
    val fullTextsHeading: String,
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
        "lgpl" in n -> "lgpl"
        "gpl" in n -> "gpl"
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
