package com.association.caisse.domain

import com.association.caisse.data.local.dao.OtherIncomeWithCategory
import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.data.local.entity.PaymentEntity

/**
 * Générateur de rapports CSV conformes aux spécifications :
 * - Encodage UTF-8 avec BOM (\uFEFF) pour compatibilité immédiate avec Microsoft Excel
 * - Séparateur point-virgule (;) standard pour les systèmes francophones
 * - Échappement rigoureux des guillemets
 */
object CsvExporter {

    private const val BOM = "\uFEFF"
    private const val DELIMITER = ";"

    private fun escape(value: Any?): String {
        val str = value?.toString() ?: ""
        val escaped = str.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Exporte le journal chronologique complet de toutes les recettes (cotisations et autres).
     */
    fun exportReceiptsJournalCsv(
        payments: List<PaymentEntity>,
        allocations: List<PaymentAllocationEntity>,
        otherIncomes: List<OtherIncomeWithCategory>,
        members: List<MemberEntity>
    ): String {
        val sb = StringBuilder()
        sb.append(BOM)

        // En-têtes
        val headers = listOf(
            "Date",
            "Type d'opération",
            "Nom & Prénom",
            "Mois concerné(s) / Catégorie",
            "Montant (Espèces)",
            "Note / Description"
        )
        sb.append(headers.joinToString(DELIMITER) { escape(it) }).append("\r\n")

        val rows = mutableListOf<JournalRow>()

        // 1. Ajouter les cotisations
        for (payment in payments) {
            val member = members.find { it.id == payment.memberId }
            val memberName = member?.let { "${it.lastName.uppercase()} ${it.firstName}" } ?: "Inconnu"
            val memberAllocations = allocations
                .filter { it.paymentId == payment.id }
                .joinToString(", ") { "${it.month} (${it.amount})" }

            rows.add(
                JournalRow(
                    date = payment.date,
                    type = "Cotisation Mensuelle",
                    name = memberName,
                    details = memberAllocations.ifBlank { "Non ventilé" },
                    amount = payment.amount,
                    note = payment.note
                )
            )
        }

        // 2. Ajouter les autres recettes
        for (item in otherIncomes) {
            val income = item.income
            val catName = item.category?.name ?: "Autre"
            val member = income.memberId?.let { id -> members.find { it.id == id } }
            val name = member?.let { "${it.lastName.uppercase()} ${it.firstName}" } ?: "Association / Tiers"

            rows.add(
                JournalRow(
                    date = income.date,
                    type = "Autre Recette",
                    name = name,
                    details = catName,
                    amount = income.amount,
                    note = income.description
                )
            )
        }

        // Tri chronologique décroissant
        rows.sortByDescending { it.date }

        for (row in rows) {
            val line = listOf(
                escape(formatDateFr(row.date)),
                escape(row.type),
                escape(row.name),
                escape(row.details),
                escape(row.amount),
                escape(row.note)
            )
            sb.append(line.joinToString(DELIMITER)).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Exporte l'état détaillé de chaque membre (mois dus, réglés, solde ou avance).
     */
    fun exportMembersBalanceCsv(
        members: List<MemberEntity>,
        balancesMap: Map<Long, List<MonthBalanceDetail>>
    ): String {
        val sb = StringBuilder()
        sb.append(BOM)

        val headers = listOf(
            "Nom",
            "Prénom",
            "Téléphone",
            "Mois d'adhésion",
            "Statut",
            "Total Dû",
            "Total Payé",
            "Solde Restant Dû",
            "Mois Impayés"
        )
        sb.append(headers.joinToString(DELIMITER) { escape(it) }).append("\r\n")

        for (member in members) {
            val balances = balancesMap[member.id] ?: emptyList()
            val totalDue = balances.sumOf { it.dueAmount }
            val totalPaid = balances.sumOf { it.paidAmount }
            val balanceDue = balances.sumOf { it.remainingAmount }
            val unpaidMonths = balances
                .filter { it.status == MonthFeeStatus.UNPAID || it.status == MonthFeeStatus.PARTIAL }
                .joinToString(", ") { it.month }

            val line = listOf(
                escape(member.lastName.uppercase()),
                escape(member.firstName),
                escape(member.phone),
                escape(member.joinMonth),
                escape(if (member.isActive) "Actif" else "Archivé"),
                escape(totalDue),
                escape(totalPaid),
                escape(balanceDue),
                escape(unpaidMonths.ifBlank { "À jour" })
            )
            sb.append(line.joinToString(DELIMITER)).append("\r\n")
        }

        return sb.toString()
    }

    private fun formatDateFr(isoDate: String): String {
        val parts = isoDate.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
    }

    private data class JournalRow(
        val date: String,
        val type: String,
        val name: String,
        val details: String,
        val amount: Long,
        val note: String
    )
}
