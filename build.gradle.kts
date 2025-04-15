buildscript {
    repositories {
        google()
        mavenCentral()
    }
    // Aqui precisamos referenciar o classpath diretamente, não através do libs
    dependencies {
        classpath("com.google.gms:google-services:4.4.2")
    }
}

plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    // Você pode adicionar esta linha para poder aplicar o plugin nos submódulos, mas com apply(false)
    alias(libs.plugins.google.services).apply(false)
}