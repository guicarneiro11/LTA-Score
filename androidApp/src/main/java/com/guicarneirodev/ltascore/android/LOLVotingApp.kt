package com.guicarneirodev.ltascore.android

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.guicarneirodev.ltascore.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LOLVotingApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d("LOLVotingApp", "Iniciando aplicação")

        // Inicializar Firebase antes do Koin
        FirebaseApp.initializeApp(this)

        // Inicializa o Koin
        startKoin {
            androidLogger(Level.ERROR) // Use Level.DEBUG para desenvolvimento
            androidContext(this@LOLVotingApp)
            modules(appModule)
        }

        Log.d("LOLVotingApp", "Firebase e Koin inicializados com sucesso")
    }
}