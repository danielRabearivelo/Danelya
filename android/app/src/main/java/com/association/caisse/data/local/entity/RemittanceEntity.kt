package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Enregistrement d'un versement des espèces encaissées à la caisse principale de l'association.
 *
 * @param id Identifiant auto-généré SQLite
 * @param date Date du versement au format "YYYY-MM-DD"
 * @param amount Montant versé en espèces (type Long)
 * @param recipient Destinataire des fonds (ex. Trésorier central, banque, président)
 * @param coveredPeriod Période d'encaissement couverte optionnelle (ex. "Janvier - Février 2026")
 * @param note Note libre sur le versement (ex. reçu papier n° 42)
 * @param createdAt Horodatage système de création
 */
@Entity(tableName = "remittances")
data class RemittanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val amount: Long,
    val recipient: String,
    val coveredPeriod: String? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
