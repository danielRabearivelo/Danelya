package com.association.caisse.data.repository

import com.association.caisse.data.local.dao.FeeRateDao
import com.association.caisse.data.local.dao.PaymentDao
import com.association.caisse.data.local.entity.MonthlyFeeRateEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao,
    private val feeRateDao: FeeRateDao
) {

    fun getAllPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    fun getPaymentsForMember(memberId: Long): Flow<List<PaymentEntity>> =
        paymentDao.getPaymentsForMember(memberId)

    fun getAllAllocations(): Flow<List<PaymentAllocationEntity>> =
        paymentDao.getAllAllocations()

    fun getAllocationsForMember(memberId: Long): Flow<List<PaymentAllocationEntity>> =
        paymentDao.getAllocationsForMember(memberId)

    fun getAllFeeRates(): Flow<List<MonthlyFeeRateEntity>> =
        feeRateDao.getAllRates()

    suspend fun recordCashPayment(
        memberId: Long,
        amount: Long,
        date: String,
        note: String,
        allocations: List<Pair<String, Long>>
    ): Long {
        val payment = PaymentEntity(
            memberId = memberId,
            date = date,
            amount = amount,
            note = note
        )

        val allocationEntities = allocations.map { (month, allocAmount) ->
            PaymentAllocationEntity(
                paymentId = 0, // Sera complété par la transaction Room
                memberId = memberId,
                month = month,
                amount = allocAmount
            )
        }

        return paymentDao.recordPaymentWithAllocations(payment, allocationEntities)
    }

    suspend fun cancelPayment(payment: PaymentEntity) {
        paymentDao.cancelPayment(payment)
    }

    suspend fun addFeeRate(effectiveFromMonth: String, amount: Long): Long {
        return feeRateDao.insertRate(
            MonthlyFeeRateEntity(
                effectiveFromMonth = effectiveFromMonth,
                amount = amount
            )
        )
    }
}
