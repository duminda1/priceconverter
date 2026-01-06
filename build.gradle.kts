// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    configurations.classpath {
        // Ensure Hilt's Gradle plugin sees a Javapoet version with canonicalName().
        resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Firebase plugins (available to modules)
    alias(libs.plugins.firebase.appdistribution) apply false
}
