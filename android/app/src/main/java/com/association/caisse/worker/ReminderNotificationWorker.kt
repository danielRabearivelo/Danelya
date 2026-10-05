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
import com.association.caisse.R
import com.association.caisse.data.repository.MemberRepository
import com.association.caisse.data.repository.PaymentRepository
import com.association.caisse.domain.FeeAllocationEngine
import com.association.caisse.domain.MonthFeeStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tâche WorkManager périodique exécutée 100% hors-ligne.
 * Déclenche une notification locale pour rappeler au trésorier le nombre de membres en retard.
 */
@HiltWorker
class ReminderNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val memberRepository: MemberRepository,
    private val paymentRepository: PaymentRepository,
    private val feeEngine: FeeAllocationEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val members = memberRepository.getActiveMembers().first()
            val allocations = paymentRepository.getAllAllocations().first()
            val rates = paymentRepository.getAllFeeRates().first()
            val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

            var lateCount = 0

            for (member in members) {
                val balances = feeEngine.computeMemberMonthlyBalances(
                    member = member,
                    allocations = allocations,
                    rates = rates,
                    currentMonth = currentMonth
                )
                val hasUnpaid = balances.any { it.status == MonthFeeStatus.UNPAID || it.status == MonthFeeStatus.PARTIAL }
                if (hasUnpaid) {
                    lateCount++
                }
            }

            if (lateCount > 0) {
                sendNotification(lateCount)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendNotification(lateMembersCount: Int) {
        // Vérification de la permission POST_NOTIFICATIONS sur Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        // Intention pour ouvrir l'application directement sur l'écran des relances
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "REMINDERS")
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Cotisations à relancer"
        val message = "$lateMembersCount membre(s) ont des cotisations impayées en attente."

        val notification = NotificationCompat.Builder(appContext, CaisseApplication.CHANNEL_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "ReminderNotificationWork"
    }
}
