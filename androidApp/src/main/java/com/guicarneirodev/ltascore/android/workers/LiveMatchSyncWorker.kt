package com.guicarneirodev.ltascore.android.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guicarneirodev.ltascore.domain.repository.MatchSyncRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LiveMatchSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val matchSyncRepository: MatchSyncRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            Log.d("LiveMatchSync", "Iniciando sincronização de partidas em andamento")
            matchSyncRepository.syncLiveMatches()
            Log.d("LiveMatchSync", "Sincronização de partidas em andamento concluída")

            Result.success()
        } catch (e: Exception) {
            Log.e("LiveMatchSync", "Falha na sincronização de partidas em andamento: ${e.message}")
            e.printStackTrace()

            Result.retry()
        }
    }
}