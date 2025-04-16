package com.guicarneirodev.ltascore.android

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.guicarneirodev.ltascore.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LTAScore : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d("LOLVotingApp", "Iniciando aplicação")

        // Inicializar Firebase antes do Koin
        FirebaseApp.initializeApp(this)

        // Configurar região para Firebase Functions, se necessário
        FirebaseFunctions.getInstance("us-central1")

        // Inicializa o Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@LTAScore)
            modules(appModule)
        }

        Log.d("LTAScore", "Firebase e Koin inicializados com sucesso")
    }
}