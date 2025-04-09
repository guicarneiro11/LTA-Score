package com.guicarneirodev.ltascore.android

import android.app.Application
import android.util.Log
import com.guicarneirodev.ltascore.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LOLVotingApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Log para depuração durante inicialização
        Log.d("LOLVotingApp", "Iniciando aplicação")

        // Inicializa o Koin
        startKoin {
            androidLogger(Level.ERROR) // Level.DEBUG em desenvolvimento
            androidContext(this@LOLVotingApp)
            modules(appModule)
        }

        Log.d("LOLVotingApp", "Koin inicializado com sucesso")
    }
}