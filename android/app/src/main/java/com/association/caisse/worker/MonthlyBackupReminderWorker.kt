package com.association.caisse.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.association.caisse.CaisseApplication
import com.association.caisse.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.temporal.ChronoUnit

@HiltWorker
class MonthlyBackupReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs: SharedPreferences = appContext.getSharedPreferences("caisse_settings", Context.MODE_PRIVATE)
        val lastBackupTime = prefs.getLong("last_manual_backup_time", 0L)

        val daysSinceLast = if (lastBackupTime > 0) {
            ChronoUnit.DAYS.between(Instant.ofEpochMilli(lastBackupTime), Instant.now())
        } else {
            31L
        }

        if (daysSinceLast >= 30) {
            sendBackupReminderNotification()
        }

        return Result.success()
    }

    private fun sendBackupReminderNotification() {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "SETTINGS")
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CaisseApplication.CHANNEL_REMITTANCE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Sauvegarde de caisse recommandée")
            .setContentText("Aucune sauvegarde manuelle n'a été exportée depuis plus de 30 jours.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Pensez à exporter une copie de sauvegarde sur Google Drive ou clé USB pour sécuriser votre comptabilité.")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1004
        const val WORK_NAME = "MonthlyBackupReminderWork"
    }
}
