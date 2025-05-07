plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.guicarneirodev.ltascore.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.guicarneirodev.ltascore.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "1.7.3"

        resourceConfigurations.addAll(listOf("en", "pt"))
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = false
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.shared)

    // Compose UI
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.foundation)

    // Navegação
    implementation(libs.navigation.compose)

    // Ciclo de Vida e ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Injeção de Dependência
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Datetime
    implementation(libs.kotlinx.datetime)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Imagens
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.preferences.core)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation("dev.gitlive:firebase-functions:1.8.0")
    implementation(libs.firebase.messaging)

    // Testes
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotlin.test)

    implementation(libs.material.icons.extended)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}