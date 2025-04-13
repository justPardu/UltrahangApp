
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Hozzá kell adni a Google Services classpath-t
        classpath(libs.google.services)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
