package com.guicarneirodev.ltascore.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guicarneirodev.ltascore.domain.repository.MatchSyncRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MatchSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val matchSyncRepository: MatchSyncRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            matchSyncRepository.syncMatchesToFirestore()

            Result.success()
        } catch (e: Exception) {
            println("Match sync failed: ${e.message}")

            Result.retry()
        }
    }
}