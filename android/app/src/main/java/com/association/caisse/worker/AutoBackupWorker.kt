package com.association.caisse.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.association.caisse.CaisseApplication
import com.association.caisse.data.local.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Sauvegarde automatique quotidienne locale dans l'espace privé de l'application.
 * Conserve une rotation de 7 sauvegardes quotidiennes et 4 hebdomadaires.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Forcer Room à vider le journal WAL
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = appContext.getDatabasePath("caisse_association.db")
            if (!dbFile.exists()) return@withContext Result.success()

            val backupDir = File(appContext.filesDir, "auto_backups").apply { mkdirs() }
            val now = LocalDate.now()
            val dayStr = now.format(DateTimeFormatter.BASIC_ISO_DATE) // YYYYMMDD
            val weekFields = WeekFields.of(Locale.getDefault())
            val weekNum = now.get(weekFields.weekOfYear())
            val weekStr = "${now.year}_w$weekNum"

            // 1. Sauvegarde quotidienne du jour
            val dailyFile = File(backupDir, "backup_daily_$dayStr.db")
            copyFile(dbFile, dailyFile)

            // 2. Sauvegarde hebdomadaire (si non existante pour cette semaine)
            val weeklyFile = File(backupDir, "backup_weekly_$weekStr.db")
            if (!weeklyFile.exists()) {
                copyFile(dbFile, weeklyFile)
            }

            // 3. Rétention : garder les 7 dernières quotidiennes
            val dailyBackups = backupDir.listFiles { _, name -> name.startsWith("backup_daily_") }
                ?.sortedByDescending { it.name }
                ?: emptyList()

            if (dailyBackups.size > 7) {
                dailyBackups.drop(7).forEach { it.delete() }
            }

            // 4. Rétention : garder les 4 dernières hebdomadaires
            val weeklyBackups = backupDir.listFiles { _, name -> name.startsWith("backup_weekly_") }
                ?.sortedByDescending { it.name }
                ?: emptyList()

            if (weeklyBackups.size > 4) {
                weeklyBackups.drop(4).forEach { it.delete() }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            sendFailureNotification(e.localizedMessage ?: "Erreur inconnue")
            Result.retry()
        }
    }

    private fun copyFile(source: File, target: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun sendFailureNotification(error: String) {
        val notification = NotificationCompat.Builder(appContext, CaisseApplication.CHANNEL_REMITTANCE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Échec de la sauvegarde automatique")
            .setContentText("Impossible d'effectuer la sauvegarde quotidienne : $error")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1003
        const val WORK_NAME = "AutoBackupDailyWork"
    }
}
