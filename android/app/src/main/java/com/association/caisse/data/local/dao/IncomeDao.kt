package com.association.caisse.data.local.dao

import androidx.room.*
import com.association.caisse.data.local.entity.IncomeCategoryEntity
import com.association.caisse.data.local.entity.OtherIncomeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objet combiné pour joindre une recette et le libellé de sa catégorie.
 */
data class OtherIncomeWithCategory(
    @Embedded val income: OtherIncomeEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: IncomeCategoryEntity?
)

@Dao
interface IncomeDao {

    // --- Catégories ---

    @Query("SELECT * FROM income_categories ORDER BY name COLLATE NOCASE ASC")
    fun getAllCategories(): Flow<List<IncomeCategoryEntity>>

    @Query("SELECT * FROM income_categories WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getActiveCategories(): Flow<List<IncomeCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: IncomeCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultCategories(categories: List<IncomeCategoryEntity>)

    @Update
    suspend fun updateCategory(category: IncomeCategoryEntity)

    @Query("SELECT COUNT(*) FROM other_incomes WHERE categoryId = :categoryId")
    suspend fun countIncomesForCategory(categoryId: Long): Int

    @Delete
    suspend fun deleteCategory(category: IncomeCategoryEntity)

    // --- Recettes hors cotisations ---

    @Transaction
    @Query("SELECT * FROM other_incomes ORDER BY date DESC, id DESC")
    fun getAllOtherIncomesWithCategory(): Flow<List<OtherIncomeWithCategory>>

    @Transaction
    @Query("SELECT * FROM other_incomes WHERE categoryId = :categoryId ORDER BY date DESC, id DESC")
    fun getOtherIncomesByCategory(categoryId: Long): Flow<List<OtherIncomeWithCategory>>

    @Transaction
    @Query("SELECT * FROM other_incomes WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getOtherIncomesByPeriod(startDate: String, endDate: String): Flow<List<OtherIncomeWithCategory>>

    @Query("SELECT * FROM other_incomes WHERE id = :id LIMIT 1")
    suspend fun getOtherIncomeById(id: Long): OtherIncomeEntity?

    @Insert
    suspend fun insertOtherIncome(income: OtherIncomeEntity): Long

    @Update
    suspend fun updateOtherIncome(income: OtherIncomeEntity)

    @Delete
    suspend fun deleteOtherIncome(income: OtherIncomeEntity)

    // --- Agrégrats comptables ---

    @Query("SELECT COALESCE(SUM(amount), 0) FROM other_incomes")
    fun getTotalOtherIncomesSum(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM other_incomes WHERE date LIKE :monthPrefix || '%'")
    fun getOtherIncomesSumForMonth(monthPrefix: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM other_incomes WHERE date LIKE :yearPrefix || '%'")
    fun getOtherIncomesSumForYear(yearPrefix: String): Flow<Long>
}
