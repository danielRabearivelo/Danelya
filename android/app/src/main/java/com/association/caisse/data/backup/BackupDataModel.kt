package com.association.caisse.data.backup

import com.association.caisse.data.local.entity.*
import kotlinx.serialization.Serializable

/**
 * Modèle complet de sauvegarde au format JSON portable.
 * Inclut les métadonnées de versionnement et l'intégralité des tables de la base de données.
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val appVersion: String = "1.0.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val associationName: String,
    val currency: String,
    val defaultPrefix: String,
    val reminderTemplate: String,
    val members: List<MemberBackupDto>,
    val feeRates: List<FeeRateBackupDto>,
    val payments: List<PaymentBackupDto>,
    val allocations: List<AllocationBackupDto>,
    val categories: List<CategoryBackupDto>,
    val otherIncomes: List<OtherIncomeBackupDto>,
    val remittances: List<RemittanceBackupDto>,
    val reminderLogs: List<ReminderLogBackupDto>
)

@Serializable
data class MemberBackupDto(
    val id: Long,
    val lastName: String,
    val firstName: String,
    val phone: String,
    val joinMonth: String,
    val isActive: Boolean,
    val customMonthlyAmount: Long? = null,
    val note: String = ""
)

@Serializable
data class FeeRateBackupDto(
    val id: Long,
    val effectiveFromMonth: String,
    val amount: Long
)

@Serializable
data class PaymentBackupDto(
    val id: Long,
    val memberId: Long,
    val date: String,
    val amount: Long,
    val note: String = ""
)

@Serializable
data class AllocationBackupDto(
    val id: Long,
    val paymentId: Long,
    val memberId: Long,
    val month: String,
    val amount: Long
)

@Serializable
data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val isActive: Boolean
)

@Serializable
data class OtherIncomeBackupDto(
    val id: Long,
    val categoryId: Long,
    val memberId: Long? = null,
    val date: String,
    val amount: Long,
    val description: String = ""
)

@Serializable
data class RemittanceBackupDto(
    val id: Long,
    val date: String,
    val amount: Long,
    val recipient: String,
    val coveredPeriod: String? = null,
    val note: String = ""
)

@Serializable
data class ReminderLogBackupDto(
    val id: Long,
    val memberId: Long,
    val date: String,
    val channel: String,
    val amountDueAtTime: Long
)

/**
 * Résumé affiché au trésorier pour prévisualisation avant confirmation d'écrasement.
 */
data class BackupPreviewSummary(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val schemaVersion: Int = 1,
    val exportedAtDate: String = "",
    val membersCount: Int = 0,
    val paymentsCount: Int = 0,
    val otherIncomesCount: Int = 0,
    val remittancesCount: Int = 0,
    val lastOperationDate: String = "",
    val rawPayload: BackupPayload? = null
)
