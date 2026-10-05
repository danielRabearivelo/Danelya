package com.association.caisse.data.local.dao

import androidx.room.*
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE memberId = :memberId ORDER BY date DESC, id DESC")
    fun getPaymentsForMember(memberId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): PaymentEntity?

    @Query("SELECT * FROM payment_allocations")
    fun getAllAllocations(): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations WHERE memberId = :memberId ORDER BY month ASC")
    fun getAllocationsForMember(memberId: Long): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations WHERE paymentId = :paymentId")
    suspend fun getAllocationsForPayment(paymentId: Long): List<PaymentAllocationEntity>

    @Insert
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Insert
    suspend fun insertAllocations(allocations: List<PaymentAllocationEntity>)

    @Query("DELETE FROM payment_allocations WHERE paymentId = :paymentId")
    suspend fun deleteAllocationsForPayment(paymentId: Long)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    /**
     * Enregistre de façon atomique un paiement et l'ensemble de ses ventilations.
     */
    @Transaction
    suspend fun recordPaymentWithAllocations(
        payment: PaymentEntity,
        allocations: List<PaymentAllocationEntity>
    ): Long {
        val paymentId = insertPayment(payment)
        val linkedAllocations = allocations.map { it.copy(paymentId = paymentId) }
        insertAllocations(linkedAllocations)
        return paymentId
    }

    /**
     * Met à jour de façon atomique un paiement et remplace ses ventilations.
     */
    @Transaction
    suspend fun updatePaymentWithAllocations(
        payment: PaymentEntity,
        newAllocations: List<PaymentAllocationEntity>
    ) {
        // Mettre à jour les informations du paiement
        deleteAllocationsForPayment(payment.id)
        val linkedAllocations = newAllocations.map { it.copy(paymentId = payment.id) }
        insertAllocations(linkedAllocations)
    }

    /**
     * Annule un paiement : supprime en cascade ses ventilations et recalcule les statuts.
     */
    @Transaction
    suspend fun cancelPayment(payment: PaymentEntity) {
        deleteAllocationsForPayment(payment.id)
        deletePayment(payment)
    }

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments")
    fun getTotalPaymentsSum(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE date LIKE :monthPrefix || '%'")
    fun getPaymentsSumForMonth(monthPrefix: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE date LIKE :yearPrefix || '%'")
    fun getPaymentsSumForYear(yearPrefix: String): Flow<Long>
}
