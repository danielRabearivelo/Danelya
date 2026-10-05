package com.association.caisse.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.association.caisse.data.local.AppDatabase
import com.association.caisse.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val database: AppDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Génère l'objet payload complet à partir de la base SQLite.
     */
    suspend fun createBackupPayload(
        associationName: String = "Notre Association",
        currency: String = "Ar",
        defaultPrefix: String = "+261",
        reminderTemplate: String = ""
    ): BackupPayload = withContext(Dispatchers.IO) {
        val members = database.memberDao().getAllMembers().first()
        val feeRates = database.feeRateDao().getAllRates().first()
        val payments = database.paymentDao().getAllPayments().first()
        val allocations = database.paymentDao().getAllAllocations().first()
        val categories = database.incomeDao().getAllCategories().first()
        val otherIncomes = database.incomeDao().getAllOtherIncomesWithCategory().first()
        val remittances = database.remittanceDao().getAllRemittances().first()
        val reminderLogs = database.reminderDao().getAllReminderLogs().first()

        BackupPayload(
            schemaVersion = 1,
            appVersion = "1.0.0",
            exportedAt = System.currentTimeMillis(),
            associationName = associationName,
            currency = currency,
            defaultPrefix = defaultPrefix,
            reminderTemplate = reminderTemplate,
            members = members.map {
                MemberBackupDto(it.id, it.lastName, it.firstName, it.phone, it.joinMonth, it.isActive, it.customMonthlyAmount, it.note)
            },
            feeRates = feeRates.map {
                FeeRateBackupDto(it.id, it.effectiveFromMonth, it.amount)
            },
            payments = payments.map {
                PaymentBackupDto(it.id, it.memberId, it.date, it.amount, it.note)
            },
            allocations = allocations.map {
                AllocationBackupDto(it.id, it.paymentId, it.memberId, it.month, it.amount)
            },
            categories = categories.map {
                CategoryBackupDto(it.id, it.name, it.isActive)
            },
            otherIncomes = otherIncomes.map {
                OtherIncomeBackupDto(it.income.id, it.income.categoryId, it.income.memberId, it.income.date, it.income.amount, it.income.description)
            },
            remittances = remittances.map {
                RemittanceBackupDto(it.id, it.date, it.amount, it.recipient, it.coveredPeriod, it.note)
            },
            reminderLogs = reminderLogs.map {
                ReminderLogBackupDto(it.id, it.memberId, it.date, it.channel.name, it.amountDueAtTime)
            }
        )
    }

    /**
     * Écrit la sauvegarde JSON dans l'URI SAF choisie par le trésorier.
     */
    suspend fun exportJsonToUri(context: Context, uri: Uri, payload: BackupPayload): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(payload)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                        writer.write(jsonString)
                        writer.flush()
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * Copie physique brute du fichier SQLite vers l'URI SAF choisie.
     */
    suspend fun copyDatabaseFileToUri(context: Context, destinationUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Forcer Room à vider son journal WAL dans le fichier principal .db
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

                val dbFile = context.getDatabasePath("caisse_association.db")
                if (!dbFile.exists()) return@withContext false

                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    FileInputStream(dbFile).use { input ->
                        input.copyTo(out)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * Lit et inspecte le fichier de sauvegarde sélectionné sans modifier la base,
     * pour fournir un aperçu détaillé (nombre de membres, paiements, etc.) et vérifier l'intégrité.
     */
    suspend fun inspectBackupFile(context: Context, uri: Uri): BackupPreviewSummary =
        withContext(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).readText()
                } ?: return@withContext BackupPreviewSummary(isValid = false, errorMessage = "Fichier vide ou illisible.")

                val payload = json.decodeFromString<BackupPayload>(content)

                if (payload.schemaVersion < 1) {
                    return@withContext BackupPreviewSummary(isValid = false, errorMessage = "Version de schéma de sauvegarde non supportée.")
                }

                val allDates = (payload.payments.map { it.date } + payload.otherIncomes.map { it.date } + payload.remittances.map { it.date })
                val latestOpDate = allDates.maxOrNull() ?: "Aucune opération"

                val exportedDateStr = Instant.ofEpochMilli(payload.exportedAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

                BackupPreviewSummary(
                    isValid = true,
                    schemaVersion = payload.schemaVersion,
                    exportedAtDate = exportedDateStr,
                    membersCount = payload.members.size,
                    paymentsCount = payload.payments.size,
                    otherIncomesCount = payload.otherIncomes.size,
                    remittancesCount = payload.remittances.size,
                    lastOperationDate = latestOpDate,
                    rawPayload = payload
                )
            } catch (e: Exception) {
                BackupPreviewSummary(
                    isValid = false,
                    errorMessage = "Fichier de sauvegarde invalide ou corrompu : ${e.localizedMessage}"
                )
            }
        }

    /**
     * Exécute la restauration atomique complète dans la base de données Room SQLite.
     */
    suspend fun restoreFromPayload(payload: BackupPayload): Boolean = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                // 1. Vider les tables existantes
                database.clearAllTables()

                // 2. Restaurer les taux mensuels
                for (r in payload.feeRates) {
                    database.feeRateDao().insertRate(
                        MonthlyFeeRateEntity(r.id, r.effectiveFromMonth, r.amount)
                    )
                }

                // 3. Restaurer les membres
                for (m in payload.members) {
                    database.memberDao().insertMember(
                        MemberEntity(m.id, m.lastName, m.firstName, m.phone, m.joinMonth, m.isActive, m.customMonthlyAmount, m.note)
                    )
                }

                // 4. Restaurer les paiements et allocations
                for (p in payload.payments) {
                    database.paymentDao().insertPayment(
                        PaymentEntity(p.id, p.memberId, p.date, p.amount, p.note)
                    )
                }
                val allocEntities = payload.allocations.map {
                    PaymentAllocationEntity(it.id, it.paymentId, it.memberId, it.month, it.amount)
                }
                database.paymentDao().insertAllocations(allocEntities)

                // 5. Restaurer les catégories et autres recettes
                for (c in payload.categories) {
                    database.incomeDao().insertCategory(
                        IncomeCategoryEntity(c.id, c.name, c.isActive)
                    )
                }
                for (o in payload.otherIncomes) {
                    database.incomeDao().insertOtherIncome(
                        OtherIncomeEntity(o.id, o.categoryId, o.memberId, o.date, o.amount, o.description)
                    )
                }

                // 6. Restaurer les versements
                for (rem in payload.remittances) {
                    database.remittanceDao().insertRemittance(
                        RemittanceEntity(rem.id, rem.date, rem.amount, rem.recipient, rem.coveredPeriod, rem.note)
                    )
                }

                // 7. Restaurer l'historique des relances
                for (log in payload.reminderLogs) {
                    val channelEnum = try {
                        ReminderChannel.valueOf(log.channel)
                    } catch (e: Exception) {
                        ReminderChannel.SMS
                    }
                    database.reminderDao().insertReminderLog(
                        ReminderLogEntity(log.id, log.memberId, log.date, channelEnum, log.amountDueAtTime)
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
