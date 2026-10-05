package com.association.caisse.data.repository

import com.association.caisse.data.local.dao.RemittanceDao
import com.association.caisse.data.local.entity.RemittanceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemittanceRepository @Inject constructor(
    private val remittanceDao: RemittanceDao
) {

    fun getAllRemittances(): Flow<List<RemittanceEntity>> = remittanceDao.getAllRemittances()

    suspend fun getLastRemittance(): RemittanceEntity? = remittanceDao.getLastRemittance()

    suspend fun addRemittance(
        date: String,
        amount: Long,
        recipient: String,
        coveredPeriod: String?,
        note: String
    ): Long {
        return remittanceDao.insertRemittance(
            RemittanceEntity(
                date = date,
                amount = amount,
                recipient = recipient.trim(),
                coveredPeriod = coveredPeriod?.trim()?.ifBlank { null },
                note = note.trim()
            )
        )
    }

    suspend fun updateRemittance(remittance: RemittanceEntity) {
        remittanceDao.updateRemittance(remittance)
    }

    suspend fun deleteRemittance(remittance: RemittanceEntity) {
        remittanceDao.deleteRemittance(remittance)
    }

    fun getTotalRemittancesSum(): Flow<Long> = remittanceDao.getTotalRemittancesSum()

    fun getRemittancesSumForMonth(monthPrefix: String): Flow<Long> =
        remittanceDao.getRemittancesSumForMonth(monthPrefix)

    fun getRemittancesSumForYear(yearPrefix: String): Flow<Long> =
        remittanceDao.getRemittancesSumForYear(yearPrefix)
}
