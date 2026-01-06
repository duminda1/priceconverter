import com.google.firebase.appdistribution.gradle.firebaseAppDistribution

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)

    alias(libs.plugins.firebase.appdistribution)

    id("kotlin-kapt")
    id("jacoco")
}

android {
    namespace = "com.pocketcurrency"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pocketcurrency"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "EXCHANGE_RATE_API_PINS", "\"\"")
        buildConfigField("String", "FRANKFURTER_API_PINS", "\"\"")
    }

    buildTypes {
        debug {
        enableUnitTestCoverage = true
        firebaseAppDistribution {
            releaseNotes = "PocketCurrency beta – offline rates & camera scan"
         }
       }   

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // TODO(security): Rotate exchange rate API pins before 2026-02-01.
            buildConfigField(
                "String",
                "EXCHANGE_RATE_API_PINS",
                "\"sha256/vzYXzoQOSpsEdzn3ONdvLgUwlIApQnxmTMtploslE/8=,sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=\""
            )
            buildConfigField(
                "String",
                "FRANKFURTER_API_PINS",
                "\"sha256/D8//K9pEwUhq04zJsf6nBegYSLQV+XLnbR8qAOq6dtc=,sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=\""
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
