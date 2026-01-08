import com.google.firebase.appdistribution.gradle.firebaseAppDistribution

// Release pins must be injected via -P... or environment variables in CI.
// Required CI inputs: EXCHANGE_RATE_API_PINS and FRANKFURTER_API_PINS (CSV of sha256 pins).
val exchangeRatePinsProvider = providers.gradleProperty("EXCHANGE_RATE_API_PINS")
    .orElse(providers.environmentVariable("EXCHANGE_RATE_API_PINS"))
val frankfurterPinsProvider = providers.gradleProperty("FRANKFURTER_API_PINS")
    .orElse(providers.environmentVariable("FRANKFURTER_API_PINS"))

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
        versionName = "1.3"
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
                releaseNotes = "PocketCurrency beta – offline rates & camera scan"
                appId = firebaseAppIdProvider.orNull?.trim().orEmpty()
                artifactType = "APK"
            }

            // R8 mapping is required for Play Console crash de-obfuscation.
            // Mapping output: app/build/outputs/mapping/release/mapping.txt
            // TODO(security): Update CI pin values and bump PIN_CONFIG_VERSION before 2026-02-01.
            // Keep current + next pins to avoid outages during certificate rotation.
            val releaseExchangeRatePins = exchangeRatePinsProvider.orNull?.trim().orEmpty()
            val releaseFrankfurterPins = frankfurterPinsProvider.orNull?.trim().orEmpty()
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

val verifyReleasePinConfig = tasks.register("verifyReleasePinConfig") {
    doLast {
        val exchangePins = exchangeRatePinsProvider.orNull?.trim().orEmpty()
        val frankfurterPins = frankfurterPinsProvider.orNull?.trim().orEmpty()
        if (exchangePins.isEmpty() || frankfurterPins.isEmpty()) {
            throw GradleException(
                "Release builds require EXCHANGE_RATE_API_PINS and FRANKFURTER_API_PINS. " +
                    "Set them via -P or environment variables in CI."
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
        "bundleRelease",
        "appDistributionUploadRelease"
    )
}.configureEach {
    dependsOn(verifyReleasePinConfig)
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
