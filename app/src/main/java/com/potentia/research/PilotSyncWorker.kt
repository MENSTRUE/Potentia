package com.potentia.research

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PilotSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!PilotSyncConfig.isConfigured) return@withContext Result.success()

        val prefs = applicationContext.getSharedPreferences(
            "potentia_prefs",
            Context.MODE_PRIVATE
        )

        // Withdrawal / personal mode stops automatic network transfer.
        val consent = PilotResearchStorage.readConsentState(prefs)
        if (!consent.researchEnabled) return@withContext Result.success()

        val sessions = PilotResearchStorage.loadSessions(prefs)
        val pending = PilotSyncStorage.pendingSessions(prefs, sessions)
        if (pending.isEmpty()) return@withContext Result.success()

        PilotSyncStorage.markAttempt(prefs)

        for (session in pending) {
            when (val upload = PilotSyncClient.upload(session)) {
                PilotSyncClient.Result.Success -> {
                    PilotSyncStorage.markSynced(prefs, session.sessionId)
                }

                is PilotSyncClient.Result.Retryable -> {
                    PilotSyncStorage.markError(prefs, upload.message)
                    return@withContext Result.retry()
                }

                is PilotSyncClient.Result.PermanentFailure -> {
                    PilotSyncStorage.markError(prefs, upload.message)
                    return@withContext Result.failure()
                }
            }
        }

        Result.success()
    }
}
