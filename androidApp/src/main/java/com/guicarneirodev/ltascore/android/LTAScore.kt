package com.guicarneirodev.ltascore.android

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.guicarneirodev.ltascore.android.di.appModule
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.android.workers.LiveMatchSyncWorker
import com.guicarneirodev.ltascore.android.workers.MatchSyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.concurrent.TimeUnit

class LTAScore : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d("LTAScore", "Iniciando aplicação")

        FirebaseApp.initializeApp(this)
        StringResources.initialize(this)
        FirebaseFunctions.getInstance("us-central1")

        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@LTAScore)
            modules(appModule)
        }

        scheduleMatchSync()

        Log.d("LTAScore", "Firebase, Koin e WorkManager inicializados com sucesso")
    }

    private fun scheduleMatchSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<MatchSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        val liveMatchSyncRequest = PeriodicWorkRequestBuilder<LiveMatchSyncWorker>(2, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "match_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "live_match_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                liveMatchSyncRequest
            )

        Log.d("LTAScore", "Sincronização periódica de partidas agendada")
    }
}