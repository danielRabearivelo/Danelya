package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ventilation exacte du montant d'un paiement sur un mois spécifique ("YYYY-MM").
 * Cette table dédiée assure la traçabilité intégrale :
 * - Paiements partiels (le solde du mois reste dû)
 * - Paiements de plusieurs mois en un seul versement
 * - Paiements d'avance sur des mois futurs
 */
@Entity(
    tableName = "payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = PaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["paymentId"]),
        Index(value = ["memberId"]),
        Index(value = ["month"])
    ]
)
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentId: Long,
    val memberId: Long,
    val month: String, // Format standard "YYYY-MM"
    val amount: Long   // Montant affecté à ce mois
)
