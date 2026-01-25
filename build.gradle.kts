// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    id("com.google.gms.google-services") version "4.4.4" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("com.google.devtools.ksp") version "2.3.4" apply false
    id("com.google.firebase.appdistribution") version "5.2.0" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}
