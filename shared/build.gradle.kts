plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Date and Time
            implementation(libs.kotlinx.datetime)

            // Ktor for KMP
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Koin Core
            implementation(libs.koin.core)

            // Firebase Functions
            implementation("dev.gitlive:firebase-functions:1.8.0")
            implementation("com.google.code.gson:gson:2.10.1")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.assertions)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)

            // Firebase
            implementation("dev.gitlive:firebase-functions:1.8.0")
        }

        iosMain.dependencies {
            // Ktor for iOS
            implementation(libs.ktor.client.ios)
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
}

android {
    namespace = "com.guicarneirodev.ltascore"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}