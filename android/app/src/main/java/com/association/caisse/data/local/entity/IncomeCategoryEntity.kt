package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Catégories configurables pour les recettes hors cotisations.
 * Par défaut : Dons, Événements, Amendes, Autres.
 * Le trésorier peut ajouter, renommer et activer/désactiver des catégories.
 */
@Entity(tableName = "income_categories")
data class IncomeCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true
)
