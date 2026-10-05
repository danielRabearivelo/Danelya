package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enregistrement d'un paiement en espèces effectué par un membre.
 *
 * @param id Identifiant auto-généré SQLite
 * @param memberId Référence vers le membre ayant payé
 * @param date Date du paiement au format "YYYY-MM-DD"
 * @param amount Montant total en espèces (type Long, zéro centime par défaut)
 * @param note Note libre sur le paiement (ex. remise en main propre, réunion mensuelle)
 * @param createdAt Horodatage système de l'enregistrement
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["memberId"])]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val date: String,
    val amount: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
