package com.potentia.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.potentia.MainActivity
import com.potentia.R

class WeeklyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(
            "potentia_prefs",
            Context.MODE_PRIVATE
        )
        val reminderEnabled = prefs.getBoolean("reminder_weekly", false)
        val recommendationsEnabled = prefs.getBoolean("development_recommendations", true)

        if (!reminderEnabled ||
            !recommendationsEnabled ||
            !WeeklyReminderScheduler.canPostNotifications(applicationContext)
        ) {
            return Result.success()
        }

        createChannel()

        val openGrowthIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_GROWTH, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE,
            openGrowthIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_potentia)
            .setContentTitle("Waktunya latihan Potentia")
            .setContentText("Lanjutkan satu latihan singkat dari rekomendasi pengembanganmu minggu ini.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Lanjutkan satu latihan singkat dari rekomendasi pengembanganmu minggu ini. Ketuk untuk membuka tab Tumbuh."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID, notification)
            Result.success()
        } catch (_: SecurityException) {
            // Permission can be revoked after WorkManager scheduled this run.
            Result.success()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pengingat latihan",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Pengingat mingguan untuk melanjutkan latihan di tab Tumbuh."
        }

        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_OPEN_GROWTH = "potentia_open_growth"
        private const val CHANNEL_ID = "potentia_growth_reminder"
        private const val NOTIFICATION_ID = 4107
        private const val REQUEST_CODE = 4107
    }
}
