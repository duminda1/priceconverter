import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.Properties
import org.gradle.api.Project
import org.gradle.api.provider.Provider

// Release pins must be injected via -P... or environment variables in CI.
// Required CI inputs: EXCHANGE_RATE_API_PINS and FRANKFURTER_API_PINS (CSV of sha256 pins).
val exchangeRatePinsProvider = providers.gradleProperty("EXCHANGE_RATE_API_PINS")
    .orElse(providers.environmentVariable("EXCHANGE_RATE_API_PINS"))
val frankfurterPinsProvider = providers.gradleProperty("FRANKFURTER_API_PINS")
    .orElse(providers.environmentVariable("FRANKFURTER_API_PINS"))
val pinsFileProvider = providers.gradleProperty("PINS_FILE")
    .orElse(providers.environmentVariable("PINS_FILE"))

// Firebase App Distribution uploads require FIREBASE_APP_ID in CI.
val firebaseAppIdProvider = providers.gradleProperty("FIREBASE_APP_ID")
    .orElse(providers.environmentVariable("FIREBASE_APP_ID"))

val keystorePathProvider = providers.environmentVariable("KEYSTORE_PATH")
val keystorePasswordProvider = providers.environmentVariable("KEYSTORE_PASSWORD")
val keyAliasProvider = providers.environmentVariable("KEY_ALIAS")
val keyPasswordProvider = providers.environmentVariable("KEY_PASSWORD")
val hasReleaseSigning = keystorePathProvider.isPresent &&
    keystorePasswordProvider.isPresent &&
    keyAliasProvider.isPresent &&
    keyPasswordProvider.isPresent
val versionPropsFile = rootProject.layout.projectDirectory.file("version.properties").asFile
val changelogFile = rootProject.layout.projectDirectory.file("CHANGELOG.md").asFile
val releaseNotesFileProvider = layout.buildDirectory.file("outputs/release-notes/release_notes.txt")
val releaseVersionFileProvider = layout.buildDirectory.file("outputs/release-notes/release_version.properties")

data class VersionInfo(
    val code: Int,
    val name: String,
    val major: Int,
    val minor: Int,
    val patch: Int
)

fun parseSemver(name: String): Triple<Int, Int, Int> {
    val parts = name.split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
    return Triple(major, minor, patch)
}

fun parseVersionInfo(props: Properties): VersionInfo {
    val code = props.getProperty("versionCode")?.trim()?.toIntOrNull() ?: 1
    val name = props.getProperty("versionName")?.trim().orEmpty().ifBlank { "0.1.0" }
    val (major, minor, patch) = parseSemver(name)
    return VersionInfo(code, name, major, minor, patch)
}

fun readVersionInfo(file: File): VersionInfo {
    val props = Properties()
    if (file.exists()) {
        file.inputStream().use { props.load(it) }
    }
    return parseVersionInfo(props)
}

fun readVersionInfoFromText(content: String): VersionInfo {
    val props = Properties()
    if (content.isNotBlank()) {
        content.reader().use { props.load(it) }
    }
    return parseVersionInfo(props)
}

fun readVersionInfoWithCommit(file: File): Pair<VersionInfo, String?> {
    val props = Properties()
    if (file.exists()) {
        file.inputStream().use { props.load(it) }
    }
    val info = parseVersionInfo(props)
    val commit = props.getProperty("commit")?.trim().orEmpty().ifBlank { null }
    return info to commit
}

fun writeVersionInfo(info: VersionInfo, file: File) {
    file.writeText(
        "versionCode=${info.code}\n" +
            "versionName=${info.name}\n"
    )
}

fun writeReleaseVersionInfo(info: VersionInfo, file: File, commit: String?) {
    val commitLine = commit?.let { "commit=$it\n" }.orEmpty()
    file.writeText(
        "versionCode=${info.code}\n" +
            "versionName=${info.name}\n" +
            commitLine
    )
}

fun bumpPatch(info: VersionInfo): VersionInfo {
    val newPatch = info.patch + 1
    val newName = "${info.major}.${info.minor}.$newPatch"
    return info.copy(code = info.code + 1, name = newName, patch = newPatch)
}

fun gitOutput(project: Project, vararg args: String): String? {
    return try {
        val output = ByteArrayOutputStream()
        project.exec {
            commandLine("git", *args)
            workingDir = rootProject.projectDir
            standardOutput = output
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        output.toString().trim().ifBlank { null }
    } catch (e: Exception) {
        null
    }
}

fun extractChangelogNotes(changelog: String, versionName: String): List<String>? {
    val headerRegex = Regex("^## \\[?${Regex.escape(versionName)}\\]?")
    val lines = changelog.lines()
    val startIndex = lines.indexOfFirst { headerRegex.containsMatchIn(it.trim()) }
    if (startIndex == -1) return null
    val notes = mutableListOf<String>()
    for (i in (startIndex + 1) until lines.size) {
        val line = lines[i].trim()
        if (line.startsWith("## ")) break
        if (line.startsWith("- ") || line.startsWith("* ")) {
            notes.add(line.drop(2).trim())
        }
    }
    return notes
}

fun insertChangelogEntry(changelog: String, entry: String): String {
    if (changelog.isBlank()) {
        return "# Changelog\n\n$entry"
    }
    val headerIndex = changelog.indexOf("# Changelog")
    return if (headerIndex >= 0) {
        val afterHeader = changelog.substring(headerIndex + "# Changelog".length).trimStart()
        "# Changelog\n\n$entry$afterHeader"
    } else {
        "# Changelog\n\n$entry${changelog.trimStart()}"
    }
}

fun upsertChangelogEntry(changelog: String, versionName: String, entry: String): String {
    val sectionRegex = Regex(
        "(?ms)^## \\[?${Regex.escape(versionName)}\\]?.*?(?=^## |\\z)"
    )
    return if (sectionRegex.containsMatchIn(changelog)) {
        sectionRegex.replace(changelog, entry.trimEnd())
    } else {
        insertChangelogEntry(changelog, entry)
    }
}

fun resolvePinsFromFile(filePath: String?): Properties? {
    if (filePath.isNullOrBlank()) {
        return null
    }
    val file = File(filePath)
    if (!file.exists()) {
        return null
    }
    val props = Properties()
    file.inputStream().use { props.load(it) }
    return props
}

fun resolvePinValue(primary: Provider<String>, key: String): String {
    val direct = primary.orNull?.trim().orEmpty()
    if (direct.isNotEmpty()) {
        return direct
    }
    val props = resolvePinsFromFile(pinsFileProvider.orNull) ?: return ""
    return props.getProperty(key)?.trim().orEmpty()
}

fun buildConfigString(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.firebase.appdistribution)

    id("kotlin-kapt")
    id("jacoco")
}

android {
    namespace = "com.pocketcurrency"
    compileSdk = 35

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(keystorePathProvider.get())
                storePassword = keystorePasswordProvider.get()
                keyAlias = keyAliasProvider.get()
                keyPassword = keyPasswordProvider.get()
            }
        }
    }

    defaultConfig {
        applicationId = "com.pocketcurrency"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Pinning config: CSV lists of sha256 pins. Keep current + next for overlap.
        // Remote config can override when PREFS_PIN_CONFIG_VERSION >= PIN_CONFIG_VERSION.
        buildConfigField("int", "PIN_CONFIG_VERSION", "1")
        buildConfigField("String", "EXCHANGE_RATE_API_PINS", "\"\"")
        buildConfigField("String", "FRANKFURTER_API_PINS", "\"\"")
    }

    buildTypes {
        debug {
        enableUnitTestCoverage = true
        firebaseAppDistribution {
            releaseNotes = "PocketCurrency beta – offline rates & camera scan"
            appId = firebaseAppIdProvider.orNull?.trim().orEmpty()
            artifactType = "APK"
         }
       }   

        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
 
            firebaseAppDistribution {
                releaseNotesFile = releaseNotesFileProvider.get().asFile.absolutePath
                appId = firebaseAppIdProvider.orNull?.trim().orEmpty()
                artifactType = "APK"
            }

            // R8 mapping is required for Play Console crash de-obfuscation.
            // Mapping output: app/build/outputs/mapping/release/mapping.txt
            // TODO(security): Update CI pin values and bump PIN_CONFIG_VERSION before 2026-02-01.
            // Keep current + next pins to avoid outages during certificate rotation.
            val releaseExchangeRatePins = resolvePinValue(
                exchangeRatePinsProvider,
                "EXCHANGE_RATE_API_PINS"
            )
            val releaseFrankfurterPins = resolvePinValue(
                frankfurterPinsProvider,
                "FRANKFURTER_API_PINS"
            )
            // Release pins must be injected via CI (-P... or env vars) and non-empty.
            buildConfigField(
                "String",
                "EXCHANGE_RATE_API_PINS",
                buildConfigString(releaseExchangeRatePins)
            )
            buildConfigField(
                "String",
                "FRANKFURTER_API_PINS",
                buildConfigString(releaseFrankfurterPins)
            )
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    lint {
        abortOnError = true
        lintConfig = file("lint.xml")
        checkReleaseBuilds = true
    }
}

androidComponents {
    val versionInfoProvider = providers.provider {
        val releaseVersionFile = releaseVersionFileProvider.get().asFile
        val content = if (releaseVersionFile.exists()) {
            releaseVersionFile.readText()
        } else if (versionPropsFile.exists()) {
            versionPropsFile.readText()
        } else {
            ""
        }
        readVersionInfoFromText(content)
    }
    val versionCodeProvider = versionInfoProvider.map { it.code }
    val versionNameProvider = versionInfoProvider.map { it.name }

    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set(versionCodeProvider)
            output.versionName.set(versionNameProvider)
        }
    }
}

val prepareAppDistributionReleaseVersion = tasks.register("prepareAppDistributionReleaseVersion") {
    group = "release"
    description = "Generate the next version for App Distribution without persisting it."
    doLast {
        val releaseVersionFile = releaseVersionFileProvider.get().asFile
        releaseVersionFile.parentFile.mkdirs()
        val headCommit = gitOutput(project, "rev-parse", "HEAD")
        if (releaseVersionFile.exists()) {
            val (existingInfo, existingCommit) = readVersionInfoWithCommit(releaseVersionFile)
            val shouldReuse = when {
                headCommit == null -> true
                existingCommit == null -> false
                headCommit == existingCommit -> true
                else -> false
            }
            if (shouldReuse) {
                logger.lifecycle(
                    "Reusing App Distribution version ${existingInfo.name} (${existingInfo.code})."
                )
                return@doLast
            }
        }

        val current = readVersionInfo(versionPropsFile)
        val bumped = bumpPatch(current)
        writeReleaseVersionInfo(bumped, releaseVersionFile, headCommit)
        logger.lifecycle("Prepared App Distribution version ${bumped.name} (${bumped.code}).")
    }
}

val generateReleaseNotes = tasks.register("generateReleaseNotes") {
    group = "release"
    description = "Generate release notes for App Distribution."
    dependsOn(prepareAppDistributionReleaseVersion)
    doLast {
        val releaseVersionFile = releaseVersionFileProvider.get().asFile
        val current = readVersionInfo(releaseVersionFile)
        val versionName = current.name
        val releaseNotesFile = releaseNotesFileProvider.get().asFile

        val changelogText = if (changelogFile.exists()) changelogFile.readText() else ""
        val changelogNotes = extractChangelogNotes(changelogText, versionName)

        val notes = if (changelogNotes.isNullOrEmpty()) {
            val lastTag = gitOutput(project, "describe", "--tags", "--abbrev=0")
            val gitNotes = if (lastTag.isNullOrBlank()) {
                gitOutput(
                    project,
                    "log",
                    "--no-merges",
                    "-n",
                    "20",
                    "--pretty=format:%s"
                )
            } else {
                gitOutput(
                    project,
                    "log",
                    "--no-merges",
                    "--pretty=format:%s",
                    "$lastTag..HEAD"
                )
            }?.lines().orEmpty().filter { it.isNotBlank() }.take(50)
            if (gitNotes.isNotEmpty()) gitNotes else listOf("No changes listed.")
        } else {
            changelogNotes
        }

        val formattedNotes = notes.map { it.trimStart('-', ' ').trim() }
        releaseNotesFile.parentFile.mkdirs()
        releaseNotesFile.writeText(formattedNotes.joinToString("\n") { "- $it" } + "\n")
    }
}

tasks.register("prepareRelease") {
    group = "release"
    description = "Prepare App Distribution release notes and version without building."
    dependsOn(generateReleaseNotes)
}

tasks.register("printReleaseNotes") {
    group = "release"
    description = "Print the latest generated release notes."
    doLast {
        val notesFile = releaseNotesFileProvider.get().asFile
        if (!notesFile.exists()) {
            throw GradleException(
                "Release notes not found. Run :app:prepareRelease or :app:generateReleaseNotes first."
            )
        }
        println(notesFile.readText().trimEnd())
    }
}

val verifyReleasePinConfig = tasks.register("verifyReleasePinConfig") {
    doLast {
        val exchangePins = resolvePinValue(exchangeRatePinsProvider, "EXCHANGE_RATE_API_PINS")
        val frankfurterPins = resolvePinValue(frankfurterPinsProvider, "FRANKFURTER_API_PINS")
        if (exchangePins.isEmpty() || frankfurterPins.isEmpty()) {
            throw GradleException(
                "Release builds require EXCHANGE_RATE_API_PINS and FRANKFURTER_API_PINS. " +
                    "Set them via -P, environment variables, or PINS_FILE."
            )
        }
    }
}

val verifyFirebaseAppId = tasks.register("verifyFirebaseAppId") {
    doLast {
        val appId = firebaseAppIdProvider.orNull?.trim().orEmpty()
        if (appId.isEmpty()) {
            throw GradleException(
                "Firebase App Distribution uploads require FIREBASE_APP_ID. " +
                    "Set it via -P or environment variables in CI."
            )
        }
    }
}

tasks.matching {
    it.name in setOf(
        "assembleRelease",
        "bundleRelease"
    )
}.configureEach {
    dependsOn(verifyReleasePinConfig)
}

tasks.matching {
    it.name in setOf("appDistributionUploadRelease")
}.configureEach {
    dependsOn(verifyReleasePinConfig)
    dependsOn(generateReleaseNotes)
    doLast {
        val releaseVersionFile = releaseVersionFileProvider.get().asFile
        if (!releaseVersionFile.exists()) {
            throw GradleException(
                "Release version missing. Run :app:prepareAppDistributionReleaseVersion first."
            )
        }
        val releaseNotesFile = releaseNotesFileProvider.get().asFile
        if (!releaseNotesFile.exists()) {
            throw GradleException(
                "Release notes missing. Run :app:generateReleaseNotes first."
            )
        }
        val releaseInfo = readVersionInfo(releaseVersionFile)
        writeVersionInfo(releaseInfo, versionPropsFile)

        val date = LocalDate.now().toString()
        val releaseNotes = releaseNotesFile.readLines()
            .map { it.trim() }
            .filter { it.startsWith("- ") }
            .map { it.removePrefix("- ").trim() }
        val entry = buildString {
            appendLine("## [${releaseInfo.name}] - $date")
            releaseNotes.forEach { appendLine("- $it") }
            appendLine()
        }
        val changelogText = if (changelogFile.exists()) changelogFile.readText() else ""
        changelogFile.writeText(
            upsertChangelogEntry(changelogText, releaseInfo.name, entry)
        )
    }
}

val appDistributionReleaseRequested = gradle.startParameter.taskNames.any {
    it.contains("appDistributionUploadRelease")
}
if (appDistributionReleaseRequested) {
    tasks.matching {
        it.name in setOf("assembleRelease", "bundleRelease")
    }.configureEach {
        dependsOn(prepareAppDistributionReleaseVersion)
    }
}

tasks.matching {
    it.name in setOf(
        "appDistributionUploadDebug",
        "appDistributionUploadRelease"
    )
}.configureEach {
    dependsOn(verifyFirebaseAppId)
}

val archiveReleaseMapping = tasks.register<Copy>("archiveReleaseMapping") {
    // Keep a stable mapping.txt location for Play Console de-obfuscation uploads.
    // CI should archive app/build/outputs/mapping-archive/release/mapping.txt.
    val mappingFile = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
    from(mappingFile)
    into(layout.buildDirectory.dir("outputs/mapping-archive/release"))
    rename { "mapping.txt" }
    doFirst {
        if (!mappingFile.get().asFile.exists()) {
            throw GradleException(
                "mapping.txt not found. Run :app:bundleRelease or :app:assembleRelease first."
            )
        }
    }
}

configurations.configureEach {
    // Work around Hilt/Javapoet classpath mismatch in the aggregate deps task.
    resolutionStrategy.force("com.squareup:javapoet:1.13.0")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val kotlinClasses = fileTree(
        layout.buildDirectory.dir("tmp/kotlin-classes/debug")
    ) {
        exclude(fileFilter)
    }

    val javaClasses = fileTree(
        layout.buildDirectory.dir("intermediates/javac/debug/classes")
    ) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(kotlinClasses, javaClasses))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
            )
        }
    )
}

dependencies {
    // --- Android + Compose ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- Testing ---
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // --- Network & JSON ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // --- Coroutines & ViewModel ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // --- ML Kit OCR ---
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-base:18.5.0")

    // --- CameraX for live image capture ---
    val camerax_version = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    implementation("androidx.camera:camera-extensions:$camerax_version")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.3")

    // Add this for await() support on ML Kit tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // --- Retrofit and Gson ---
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Optional: for logging network requests in debug builds
    debugImplementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // EncryptedSharedPreferences for API keys
    implementation("androidx.security:security-crypto:1.1.0")

    // Material icons for dropdown indicators
    implementation("androidx.compose.material:material-icons-extended")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
