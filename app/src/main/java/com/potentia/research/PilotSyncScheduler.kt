package com.potentia.research

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object PilotSyncScheduler {
    private const val UNIQUE_WORK = "potentia-pilot-auto-sync"

    fun enqueue(context: Context) {
        if (!PilotSyncConfig.isConfigured) return

        val request = OneTimeWorkRequestBuilder<PilotSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    fun enqueueNow(context: Context) {
        if (!PilotSyncConfig.isConfigured) return

        val request = OneTimeWorkRequestBuilder<PilotSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK)
    }
}
