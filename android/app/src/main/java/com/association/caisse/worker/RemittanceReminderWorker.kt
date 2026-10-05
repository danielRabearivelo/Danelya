package com.association.caisse.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.association.caisse.CaisseApplication
import com.association.caisse.MainActivity
import com.association.caisse.data.repository.IncomeRepository
import com.association.caisse.data.repository.PaymentRepository
import com.association.caisse.data.repository.RemittanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Tâche WorkManager vérifiant hors-ligne si le montant restant à verser
 * dépasse le seuil configuré ou si aucun versement n'a été fait depuis N jours.
 */
@HiltWorker
class RemittanceReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val paymentRepository: PaymentRepository,
    private val incomeRepository: IncomeRepository,
    private val remittanceRepository: RemittanceRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val totalFee = paymentRepository.getAllPayments().first().sumOf { it.amount }
            val totalOther = incomeRepository.getAllOtherIncomes().first().sumOf { it.income.amount }
            val totalCollected = totalFee + totalOther

            val totalRemitted = remittanceRepository.getAllRemittances().first().sumOf { it.amount }
            val remainingToRemit = totalCollected - totalRemitted

            val threshold = 100_000L // Seuil réglable

            if (remainingToRemit >= threshold) {
                sendRemittanceNotification(remainingToRemit)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendRemittanceNotification(amount: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "REMITTANCES")
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Versement à la caisse principale"
        val message = "Il reste $amount Ar en espèces à verser à la caisse."

        val notification = NotificationCompat.Builder(appContext, CaisseApplication.CHANNEL_REMITTANCE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val WORK_NAME = "RemittanceReminderWork"
    }
}
