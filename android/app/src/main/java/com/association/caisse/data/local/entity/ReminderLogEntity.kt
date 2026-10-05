package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canaux possibles pour relancer un membre en retard.
 */
enum class ReminderChannel {
    WHATSAPP,
    SMS,
    CALL
}

/**
 * Historique des relances effectuées auprès d'un membre.
 *
 * @param id Identifiant auto-généré SQLite
 * @param memberId Référence vers MemberEntity
 * @param date Date et heure de la relance au format "YYYY-MM-DD HH:mm"
 * @param channel Canal utilisé (WhatsApp, SMS ou Appel)
 * @param amountDueAtTime Montant des cotisations restant dues au moment de la relance
 * @param createdAt Horodatage système
 */
@Entity(
    tableName = "reminder_logs",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memberId"]),
        Index(value = ["date"])
    ]
)
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val date: String,
    val channel: ReminderChannel,
    val amountDueAtTime: Long,
    val createdAt: Long = System.currentTimeMillis()
)
