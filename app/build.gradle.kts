import java.util.Properties
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries.android)
}

android {
    namespace = "com.wanderwildwood.kuroban"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wanderwildwood.kuroban"
        // The Kompakt runs Android 12 (API 31); nothing here needs anything newer, or even
        // that: Supernote's tablets run Android 11 (API 30), and it installs there too.
        minSdk = 30
        targetSdk = 31
        versionCode = 35
        versionName = "1.2.22"

        ndk {
            // The bundled engine is built for the Kompakt and nothing else.
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
    }

    // A real keystore in signing/ signs every build type when it is present, so the
    // very first install is already release-signed and a later update can never hit
    // INSTALL_FAILED_UPDATE_INCOMPATIBLE. It is gitignored, and there is no fallback:
    // a fresh clone builds an unsigned release APK, which will not install anywhere.
    // A keystore committed to a public repo is not a signing key, it is a formality,
    // and a missing one should stop you rather than produce something installable.
    // (Debug builds still get the ordinary Android debug key from AGP.)
    val signingPropertiesFile = rootProject.file("signing/signing.properties")
    val realSigningConfig = if (signingPropertiesFile.isFile) {
        val signingProperties = Properties().apply {
            signingPropertiesFile.inputStream().use(::load)
        }
        signingConfigs.create("real") {
            storeFile = rootProject.file("signing/signing.keystore")
            storePassword = signingProperties.getProperty("STORE_PASSWORD")
            keyAlias = signingProperties.getProperty("KEY_ALIAS")
            keyPassword = signingProperties.getProperty("KEY_PASSWORD")
        }
    } else {
        null
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            realSigningConfig?.let { signingConfig = it }
        }
        getByName("release") {
            // The one thing that differs between a release built here and the one GitHub
            // publishes: AGP stamps the git revision into META-INF, and the build box works
            // from an rsync with no .git, so it writes NO_SUPPORTED_VCS_FOUND where the CI
            // runner writes the commit. Off, so the two have identical contents.
            vcsInfo {
                include = false
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            realSigningConfig?.let { signingConfig = it }
        }
    }

    packaging {
        jniLibs {
            // The engine is an executable, not a library: it has to be unpacked onto
            // disk in nativeLibraryDir for the app to be able to exec it.
            useLegacyPackaging = true
        }
    }

    lint {
        // This app is sideloaded onto a Kompakt and is not going to Google Play, whose
        // API-33 floor this otherwise trips. Targeting the OS the device actually runs
        // is deliberate: see minSdk above.
        disable += "ExpiredTargetSdkVersion"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        // The About screen shows the version it is actually running.
        buildConfig = true
    }

    sourceSets {
        named("main") {
            kotlin.srcDir("src/main/kotlin")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.mmd)

    testImplementation(libs.junit)
}

// The Licenses page lists what the build actually packs: this plugin walks each variant's runtime
// dependencies and writes res/raw/aboutlibraries.json (one entry per artifact, with its POM's
// licence). What Gradle cannot see (fonts, data sets, native code, desugaring) and licence texts
// of our own choosing are in config/libraries and config/licenses. Offline: the build fetches
// nothing; the full texts come from res/raw and config/licenses. The app reads the file with
// shared-licences' LicencesJson.kt (no AboutLibraries runtime library).
aboutLibraries {
    offlineMode.set(true)
    collect {
        configPath.set(rootProject.file("config"))
        fetchRemoteLicense.set(false)
        fetchRemoteFunding.set(false)
        includePlatform.set(false)
    }
    export {
        excludeFields.addAll("funding")
    }
    library {
        mergePlatformArtifacts.set(true)
        // The page lists what the release APK carries. These come in only through
        // debugImplementation (Compose's ui-tooling), so a debug build leaves them off too.
        exclusionPatterns.addAll(
            listOf(
                "androidx.compose.ui:ui-tooling", "androidx.compose.ui:ui-tooling-android",
                "androidx.compose.ui:ui-tooling-data", "androidx.compose.ui:ui-tooling-data-android",
                "androidx.compose.material:material", "androidx.compose.material:material-icons-core",
            ).map { Pattern.compile("^" + Regex.escape(it) + "$") },
        )
    }
}

// The build fails when a bundled library has no license, or a license with no full text to show
// (neither its own text from config/licenses nor a family text in res/raw). Mudita review, 4 Nov 2026.
val licenceRawTexts: List<String> = file("src/main/res/raw").listFiles().orEmpty().map { it.name.lowercase() }
tasks.matching { it.name.startsWith("prepareLibraryDefinitions") }.configureEach {
    val raw = licenceRawTexts
    inputs.property("licenceRawTexts", raw) // so the check reruns when a text is added or removed
    doLast {
        fun family(n: String): String? {
            val l = n.lowercase()
            return when {
                "classpath" in l -> "classpath"; "lgpl" in l || "lesser general" in l -> "lgpl"
                "gpl" in l || "general public licen" in l -> "gpl"; "unicode" in l -> "unicode"; "apache" in l -> "apache"
                "mpl" in l || "mozilla" in l -> "mpl"; "mit" in l.split(Regex("[^a-z]+")) -> "mit"; "bsd" in l -> "bsd"
                "ofl" in l || "open font" in l -> "ofl"; "cc-by" in l || "cc by" in l || "cc0" in l || "public domain" in l -> "none"
                else -> null
            }
        }
        val bad = mutableListOf<String>()
        outputs.files.asFileTree.matching { include("**/aboutlibraries.json") }.forEach { f ->
            @Suppress("UNCHECKED_CAST")
            val root = groovy.json.JsonSlurper().parse(f) as Map<String, Any?>
            val lics = (root["licenses"] as? Map<String, Map<String, Any?>>).orEmpty()
            (root["libraries"] as? List<Map<String, Any?>>).orEmpty().forEach { lib ->
                val keys = (lib["licenses"] as? List<String>).orEmpty()
                if (keys.isEmpty()) bad += "${lib["uniqueId"]}: no license"
                keys.forEach { k ->
                    val e = lics[k].orEmpty()
                    if ((e["content"] as? String).isNullOrBlank()) {
                        val fam = family((e["spdxId"] ?: e["name"] ?: k).toString())
                        if (fam == null || (fam != "none" && raw.none { fam in it }))
                            bad += "${lib["uniqueId"]}: no text for ${e["name"] ?: k}"
                    }
                }
            }
        }
        if (bad.isNotEmpty()) throw GradleException("Licenses page: add the text in config/licenses or res/raw: " + bad.distinct().joinToString("; "))
    }
}
