package com.association.caisse.ui.remittances

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.association.caisse.data.local.dao.OtherIncomeWithCategory
import com.association.caisse.data.local.entity.PaymentEntity
import com.association.caisse.data.local.entity.RemittanceEntity
import com.association.caisse.data.repository.IncomeRepository
import com.association.caisse.data.repository.PaymentRepository
import com.association.caisse.data.repository.RemittanceRepository
import com.association.caisse.domain.RemittancePdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MonthlyStatementRow(
    val month: String,
    val feesCollected: Long,
    val otherCollected: Long,
    val totalCollected: Long,
    val remitted: Long,
    val cumulativeRemaining: Long
)

data class RemittancesUiState(
    val remittances: List<RemittanceEntity> = emptyList(),
    val totalFees: Long = 0L,
    val totalOtherIncomes: Long = 0L,
    val totalCollectedAllTime: Long = 0L,
    val totalRemittedAllTime: Long = 0L,
    val remainingToRemit: Long = 0L,
    val lastRecipient: String = "Trésorerie Centrale",
    val monthlyStatement: List<MonthlyStatementRow> = emptyList(),
    val selectedPeriod: String = "ALL", // "ALL", "CURRENT_YEAR"
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currency: String = "Ar"
)

@HiltViewModel
class RemittancesViewModel @Inject constructor(
    private val remittanceRepository: RemittanceRepository,
    private val paymentRepository: PaymentRepository,
    private val incomeRepository: IncomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemittancesUiState())
    val uiState: StateFlow<RemittancesUiState> = _uiState.asStateFlow()

    private var rawPayments: List<PaymentEntity> = emptyList()
    private var rawOtherIncomes: List<OtherIncomeWithCategory> = emptyList()
    private var rawRemittances: List<RemittanceEntity> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                remittanceRepository.getAllRemittances(),
                paymentRepository.getAllPayments(),
                incomeRepository.getAllOtherIncomes()
            ) { remittances, payments, otherIncomes ->
                rawRemittances = remittances
                rawPayments = payments
                rawOtherIncomes = otherIncomes

                val totalFees = payments.sumOf { it.amount }
                val totalOther = otherIncomes.sumOf { it.income.amount }
                val totalCollected = totalFees + totalOther
                val totalRemitted = remittances.sumOf { it.amount }
                val remaining = totalCollected - totalRemitted

                val lastRecipient = remittances.firstOrNull()?.recipient ?: "Trésorerie Centrale"
                val statement = computeStatementTable(payments, otherIncomes, remittances)

                RemittancesUiState(
                    remittances = remittances,
                    totalFees = totalFees,
                    totalOtherIncomes = totalOther,
                    totalCollectedAllTime = totalCollected,
                    totalRemittedAllTime = totalRemitted,
                    remainingToRemit = remaining,
                    lastRecipient = lastRecipient,
                    monthlyStatement = statement,
                    currency = "Ar"
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun computeStatementTable(
        payments: List<PaymentEntity>,
        otherIncomes: List<OtherIncomeWithCategory>,
        remittances: List<RemittanceEntity>
    ): List<MonthlyStatementRow> {
        val allMonths = mutableSetOf<String>()
        payments.forEach { allMonths.add(it.date.take(7)) }
        otherIncomes.forEach { allMonths.add(it.income.date.take(7)) }
        remittances.forEach { allMonths.add(it.date.take(7)) }

        val sortedMonths = allMonths.sorted()
        val result = mutableListOf<MonthlyStatementRow>()
        var runningRemaining = 0L

        for (m in sortedMonths) {
            val fees = payments.filter { it.date.startsWith(m) }.sumOf { it.amount }
            val other = otherIncomes.filter { it.income.date.startsWith(m) }.sumOf { it.income.amount }
            val totalMonth = fees + other
            val rem = remittances.filter { it.date.startsWith(m) }.sumOf { it.amount }

            runningRemaining += (totalMonth - rem)

            result.add(
                MonthlyStatementRow(
                    month = m,
                    feesCollected = fees,
                    otherCollected = other,
                    totalCollected = totalMonth,
                    remitted = rem,
                    cumulativeRemaining = runningRemaining
                )
            )
        }

        return result.reversed() // Mois les plus récents en premier
    }

    fun addRemittance(
        amount: Long,
        recipient: String,
        coveredPeriod: String?,
        date: String,
        note: String
    ) {
        if (amount <= 0 || recipient.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Montant (>0) et destinataire obligatoires.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                remittanceRepository.addRemittance(date, amount, recipient, coveredPeriod, note)
                _uiState.update {
                    it.copy(isSaving = false, successMessage = "Versement enregistré avec succès.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Erreur : ${e.localizedMessage}")
                }
            }
        }
    }

    fun deleteRemittance(remittance: RemittanceEntity) {
        viewModelScope.launch {
            try {
                remittanceRepository.deleteRemittance(remittance)
                _uiState.update { it.copy(successMessage = "Versement supprimé.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur : ${e.localizedMessage}") }
            }
        }
    }

    fun exportPdf(context: Context, uri: Uri, associationName: String) {
        RemittancePdfGenerator.generateRemittanceStatementPdf(
            context = context,
            outputUri = uri,
            associationName = associationName,
            periodLabel = "Année " + LocalDate.now().year,
            totalFees = _uiState.value.totalFees,
            totalOtherIncomes = _uiState.value.totalOtherIncomes,
            remittances = _uiState.value.remittances,
            currency = _uiState.value.currency
        )
    }

    fun generateCsvContent(): String {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("\"Date\";\"Destinataire\";\"Période Couverte\";\"Montant (Espèces)\";\"Note\"\r\n")

        for (r in _uiState.value.remittances) {
            sb.append("\"${r.date}\";\"${r.recipient}\";\"${r.coveredPeriod ?: "-"}\";\"${r.amount}\";\"${r.note}\"\r\n")
        }

        return sb.toString()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
