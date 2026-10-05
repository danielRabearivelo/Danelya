package com.association.caisse.data.repository

import com.association.caisse.data.local.dao.IncomeDao
import com.association.caisse.data.local.dao.OtherIncomeWithCategory
import com.association.caisse.data.local.entity.IncomeCategoryEntity
import com.association.caisse.data.local.entity.OtherIncomeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao
) {

    fun getAllCategories(): Flow<List<IncomeCategoryEntity>> = incomeDao.getAllCategories()

    fun getActiveCategories(): Flow<List<IncomeCategoryEntity>> = incomeDao.getActiveCategories()

    suspend fun addCategory(name: String): Long {
        return incomeDao.insertCategory(IncomeCategoryEntity(name = name.trim(), isActive = true))
    }

    suspend fun updateCategory(category: IncomeCategoryEntity) {
        incomeDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: IncomeCategoryEntity): Boolean {
        val count = incomeDao.countIncomesForCategory(category.id)
        if (count > 0) {
            // Désactiver plutôt que supprimer pour garder l'intégrité
            incomeDao.updateCategory(category.copy(isActive = false))
            return false
        }
        incomeDao.deleteCategory(category)
        return true
    }

    fun getAllOtherIncomes(): Flow<List<OtherIncomeWithCategory>> =
        incomeDao.getAllOtherIncomesWithCategory()

    suspend fun addOtherIncome(
        categoryId: Long,
        memberId: Long?,
        date: String,
        amount: Long,
        description: String
    ): Long {
        return incomeDao.insertOtherIncome(
            OtherIncomeEntity(
                categoryId = categoryId,
                memberId = memberId,
                date = date,
                amount = amount,
                description = description.trim()
            )
        )
    }

    suspend fun updateOtherIncome(income: OtherIncomeEntity) {
        incomeDao.updateOtherIncome(income)
    }

    suspend fun deleteOtherIncome(income: OtherIncomeEntity) {
        incomeDao.deleteOtherIncome(income)
    }

    fun getTotalOtherIncomesSum(): Flow<Long> = incomeDao.getTotalOtherIncomesSum()

    fun getOtherIncomesSumForMonth(monthPrefix: String): Flow<Long> =
        incomeDao.getOtherIncomesSumForMonth(monthPrefix)

    fun getOtherIncomesSumForYear(yearPrefix: String): Flow<Long> =
        incomeDao.getOtherIncomesSumForYear(yearPrefix)
}
