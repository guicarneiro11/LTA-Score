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

            // Serialização
            implementation(libs.kotlinx.serialization.json)

            // Data e Tempo
            implementation(libs.kotlinx.datetime)

            // Ktor para KMP
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Koin Core
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.assertions)
        }

        androidMain.dependencies {
            // Ktor para Android
            implementation(libs.ktor.client.android)

            // Firebase - adicione firebase diretamente sem usar platform()
            implementation(libs.firebase.functions)
        }

        iosMain.dependencies {
            // Ktor para iOS
            implementation(libs.ktor.client.ios)
        }
    }
}

// Aqui podemos adicionar as dependências de plataforma para o projeto Android
dependencies {
    // Isso garante que a plataforma BOM seja aplicada ao projeto quando necessário
    // mas não é referenciada diretamente do sourceSet do KMP
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