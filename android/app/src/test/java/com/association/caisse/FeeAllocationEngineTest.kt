package com.association.caisse

import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.MonthlyFeeRateEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.domain.FeeAllocationEngine
import com.association.caisse.domain.MonthFeeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires complets de l'algorithme de répartition des cotisations en espèces :
 * 1. Paiement partiel : le solde du mois reste dû
 * 2. Paiement multi-mois : solder les plus anciens puis acompte sur le suivant
 * 3. Paiement d'avance : mois futurs pris en compte
 * 4. Changement de taux mensuel historique
 * 5. Surcharge personnalisée pour un membre solidaire
 */
class FeeAllocationEngineTest {

    private lateinit var engine: FeeAllocationEngine
    private lateinit var standardMember: MemberEntity
    private lateinit var rates: List<MonthlyFeeRateEntity>

    @Before
    fun setup() {
        engine = FeeAllocationEngine()
        standardMember = MemberEntity(
            id = 1L,
            lastName = "Ranaivo",
            firstName = "Jean",
            joinMonth = "2026-01"
        )
        rates = listOf(
            MonthlyFeeRateEntity(id = 1L, effectiveFromMonth = "2026-01", amount = 10000L)
        )
    }

    @Test
    fun testPartialPaymentLeavesRemainingBalance() {
        // Le membre doit 10000 Ar pour Janvier 2026. Il règle 6000 Ar en espèces.
        val proposals = engine.calculateAutoAllocation(
            amountReceived = 6000L,
            member = standardMember,
            existingAllocations = emptyList(),
            rates = rates,
            currentMonth = "2026-01"
        )

        assertEquals(1, proposals.size)
        assertEquals("2026-01", proposals[0].month)
        assertEquals(6000L, proposals[0].allocatedAmount)
        assertEquals(4000L, proposals[0].remainingDueAfter)
        assertEquals(MonthFeeStatus.PARTIAL, proposals[0].statusAfter)
    }

    @Test
    fun testMultiMonthPaymentWithSurplusToAdvance() {
        // Mois courant = Février 2026 (2 mois dus = 20000 Ar).
        // Le membre verse 25000 Ar en espèces.
        // Doit solder Janvier (10k), solder Février (10k) et verser 5k en avance sur Mars 2026.
        val proposals = engine.calculateAutoAllocation(
            amountReceived = 25000L,
            member = standardMember,
            existingAllocations = emptyList(),
            rates = rates,
            currentMonth = "2026-02"
        )

        assertEquals(3, proposals.size)

        // Janvier
        assertEquals("2026-01", proposals[0].month)
        assertEquals(10000L, proposals[0].allocatedAmount)
        assertEquals(MonthFeeStatus.PAID, proposals[0].statusAfter)

        // Février
        assertEquals("2026-02", proposals[1].month)
        assertEquals(10000L, proposals[1].allocatedAmount)
        assertEquals(MonthFeeStatus.PAID, proposals[1].statusAfter)

        // Mars (Avance partielle)
        assertEquals("2026-03", proposals[2].month)
        assertEquals(5000L, proposals[2].allocatedAmount)
        assertEquals(5000L, proposals[2].remainingDueAfter)
        assertEquals(MonthFeeStatus.PARTIAL, proposals[2].statusAfter)
    }

    @Test
    fun testAdvancePaymentWhenUpToDate() {
        // Janvier 2026 est déjà soldé dans la base
        val existing = listOf(
            PaymentAllocationEntity(id = 1L, paymentId = 1L, memberId = 1L, month = "2026-01", amount = 10000L)
        )
        // Le membre verse 20000 Ar pour Février et Mars d'avance
        val proposals = engine.calculateAutoAllocation(
            amountReceived = 20000L,
            member = standardMember,
            existingAllocations = existing,
            rates = rates,
            currentMonth = "2026-01"
        )

        assertEquals(2, proposals.size)
        assertEquals("2026-02", proposals[0].month)
        assertEquals(10000L, proposals[0].allocatedAmount)
        assertEquals(MonthFeeStatus.ADVANCE, proposals[0].statusAfter)

        assertEquals("2026-03", proposals[1].month)
        assertEquals(10000L, proposals[1].allocatedAmount)
        assertEquals(MonthFeeStatus.ADVANCE, proposals[1].statusAfter)
    }

    @Test
    fun testHistoricalRateChangeRespectsEffectiveMonth() {
        // Évolution des tarifs : 5000 Ar en 2025, puis 10000 Ar à partir de 2026-01
        val multiRates = listOf(
            MonthlyFeeRateEntity(id = 2L, effectiveFromMonth = "2026-01", amount = 10000L),
            MonthlyFeeRateEntity(id = 1L, effectiveFromMonth = "2025-01", amount = 5000L)
        )
        val oldMember = standardMember.copy(joinMonth = "2025-12")

        val feeDec2025 = engine.getExpectedFeeForMonth("2025-12", oldMember, multiRates)
        val feeJan2026 = engine.getExpectedFeeForMonth("2026-01", oldMember, multiRates)

        assertEquals(5000L, feeDec2025)
        assertEquals(10000L, feeJan2026)
    }

    @Test
    fun testMemberCustomMonthlyAmountOverridesDefault() {
        // Membre ayant une cotisation solidaire de 15000 Ar
        val customMember = standardMember.copy(customMonthlyAmount = 15000L)
        val expected = engine.getExpectedFeeForMonth("2026-01", customMember, rates)

        assertEquals(15000L, expected)
    }
}
