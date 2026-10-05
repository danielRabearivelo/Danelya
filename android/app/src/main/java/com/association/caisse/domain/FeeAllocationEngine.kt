package com.association.caisse.domain

import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.MonthlyFeeRateEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Statut d'un mois pour un membre.
 */
enum class MonthFeeStatus {
    PAID,       // Soldé en totalité
    PARTIAL,    // Partiellement réglé
    UNPAID,     // Aucun règlement reçu
    ADVANCE     // Mois futur réglé (d'avance)
}

/**
 * Représente la situation détaillée d'un mois pour un membre.
 */
data class MonthBalanceDetail(
    val month: String,           // "YYYY-MM"
    val dueAmount: Long,         // Montant théoriquement dû
    val paidAmount: Long,        // Montant total déjà réglé
    val status: MonthFeeStatus,
    val remainingAmount: Long    // Solde restant à régler
)

/**
 * Proposition de ventilation pour un mois lors de la saisie d'un nouveau paiement.
 */
data class MonthAllocationProposal(
    val month: String,
    val monthLabel: String,
    val expectedFee: Long,
    val previouslyPaid: Long,
    val allocatedAmount: Long,
    val remainingDueAfter: Long,
    val statusAfter: MonthFeeStatus
)

/**
 * Moteur pur de calcul des cotisations et de la répartition automatique en espèces.
 *
 * Algorithme imposé :
 * 1. Détermination du montant exigible par mois (surcharge spécifique du membre prioritaire,
 *    sinon taux par défaut le plus récent applicable à ce mois).
 * 2. Sommation des paiements déjà enregistrés pour ce mois.
 * 3. Affectation prioritaire du cash disponible aux mois impayés/partiels les plus anciens (FIFO).
 * 4. Si le montant dépasse la dette échue, affectation séquentielle aux mois futurs (Avances).
 */
class FeeAllocationEngine @Inject constructor() {

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    /**
     * Calcule le montant attendu pour un mois spécifique en tenant compte de l'historique des taux.
     */
    fun getExpectedFeeForMonth(
        month: String,
        member: MemberEntity,
        rates: List<MonthlyFeeRateEntity>
    ): Long {
        if (member.customMonthlyAmount != null && member.customMonthlyAmount > 0) {
            return member.customMonthlyAmount
        }
        val applicableRate = rates
            .sortedByDescending { it.effectiveFromMonth }
            .firstOrNull { it.effectiveFromMonth <= month }
            ?: rates.minByOrNull { it.effectiveFromMonth }

        return applicableRate?.amount ?: 10000L
    }

    /**
     * Calcule la situation financière mois par mois pour un membre donné,
     * depuis son mois d'adhésion jusqu'au mois courant (plus les mois futurs ayant reçu des avances).
     */
    fun computeMemberMonthlyBalances(
        member: MemberEntity,
        allocations: List<PaymentAllocationEntity>,
        rates: List<MonthlyFeeRateEntity>,
        currentMonth: String = YearMonth.now().format(monthFormatter)
    ): List<MonthBalanceDetail> {
        val paidByMonth = allocations
            .filter { it.memberId == member.id }
            .groupBy { it.month }
            .mapValues { it.value.sumOf { alloc -> alloc.amount } }

        val result = mutableListOf<MonthBalanceDetail>()
        var cursor = YearMonth.parse(member.joinMonth, monthFormatter)
        val endMonth = YearMonth.parse(currentMonth, monthFormatter)

        // Mois dus du joinMonth jusqu'à currentMonth
        while (!cursor.isAfter(endMonth)) {
            val monthStr = cursor.format(monthFormatter)
            val expected = getExpectedFeeForMonth(monthStr, member, rates)
            val paid = paidByMonth[monthStr] ?: 0L

            val status = when {
                paid >= expected -> MonthFeeStatus.PAID
                paid > 0L -> MonthFeeStatus.PARTIAL
                else -> MonthFeeStatus.UNPAID
            }

            result.add(
                MonthBalanceDetail(
                    month = monthStr,
                    dueAmount = expected,
                    paidAmount = paid,
                    status = status,
                    remainingAmount = (expected - paid).coerceAtLeast(0L)
                )
            )
            cursor = cursor.plusMonths(1)
        }

        // Mois futurs ayant des paiements d'avance
        val futurePaidMonths = paidByMonth.keys
            .filter { it > currentMonth }
            .sorted()

        for (futureMonth in futurePaidMonths) {
            val expected = getExpectedFeeForMonth(futureMonth, member, rates)
            val paid = paidByMonth[futureMonth] ?: 0L
            val status = if (paid >= expected) MonthFeeStatus.ADVANCE else MonthFeeStatus.PARTIAL

            result.add(
                MonthBalanceDetail(
                    month = futureMonth,
                    dueAmount = expected,
                    paidAmount = paid,
                    status = status,
                    remainingAmount = (expected - paid).coerceAtLeast(0L)
                )
            )
        }

        return result
    }

    /**
     * Génère la proposition de répartition automatique pour un montant en espèces reçu.
     */
    fun calculateAutoAllocation(
        amountReceived: Long,
        member: MemberEntity,
        existingAllocations: List<PaymentAllocationEntity>,
        rates: List<MonthlyFeeRateEntity>,
        currentMonth: String = YearMonth.now().format(monthFormatter)
    ): List<MonthAllocationProposal> {
        if (amountReceived <= 0) return emptyList()

        var remainingCash = amountReceived
        val proposals = mutableListOf<MonthAllocationProposal>()

        val paidByMonth = existingAllocations
            .filter { it.memberId == member.id }
            .groupBy { it.month }
            .mapValues { it.value.sumOf { alloc -> alloc.amount } }

        var cursor = YearMonth.parse(member.joinMonth, monthFormatter)
        val nowMonth = YearMonth.parse(currentMonth, monthFormatter)

        // Parcourt les mois tant qu'il y a du cash à ventiler
        while (remainingCash > 0) {
            val monthStr = cursor.format(monthFormatter)
            val expected = getExpectedFeeForMonth(monthStr, member, rates)
            val previouslyPaid = paidByMonth[monthStr] ?: 0L
            val needed = (expected - previouslyPaid).coerceAtLeast(0L)

            if (needed > 0) {
                val allocate = minOf(remainingCash, needed)
                val newPaid = previouslyPaid + allocate
                remainingCash -= allocate

                val isFuture = cursor.isAfter(nowMonth)
                val statusAfter = when {
                    newPaid >= expected -> if (isFuture) MonthFeeStatus.ADVANCE else MonthFeeStatus.PAID
                    newPaid > 0L -> MonthFeeStatus.PARTIAL
                    else -> MonthFeeStatus.UNPAID
                }

                proposals.add(
                    MonthAllocationProposal(
                        month = monthStr,
                        monthLabel = formatMonthToDisplay(cursor),
                        expectedFee = expected,
                        previouslyPaid = previouslyPaid,
                        allocatedAmount = allocate,
                        remainingDueAfter = (expected - newPaid).coerceAtLeast(0L),
                        statusAfter = statusAfter
                    )
                )
            }

            cursor = cursor.plusMonths(1)
            // Sécurité anti-dépassement (max 10 ans)
            if (proposals.size >= 120) break
        }

        return proposals
    }

    private fun formatMonthToDisplay(ym: YearMonth): String {
        val monthNames = arrayOf(
            "Janv.", "Févr.", "Mars", "Avr.", "Mai", "Juin",
            "Juil.", "Août", "Sept.", "Oct.", "Nov.", "Déc."
        )
        return "${monthNames[ym.monthValue - 1]} ${ym.year}"
    }
}
