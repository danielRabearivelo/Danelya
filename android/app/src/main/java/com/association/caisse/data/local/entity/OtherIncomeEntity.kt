package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enregistrement d'une recette perçue en espèces hors cotisation mensuelle.
 *
 * @param id Identifiant auto-généré SQLite
 * @param categoryId Référence vers IncomeCategoryEntity
 * @param memberId Référence optionnelle vers MemberEntity (null si tiers ou don anonyme)
 * @param date Date au format "YYYY-MM-DD"
 * @param amount Montant en espèces (type Long)
 * @param description Description ou motif libre de la recette
 * @param createdAt Horodatage système de création
 */
@Entity(
    tableName = "other_incomes",
    foreignKeys = [
        ForeignKey(
            entity = IncomeCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["memberId"]),
        Index(value = ["date"])
    ]
)
data class OtherIncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val memberId: Long? = null,
    val date: String,
    val amount: Long,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
