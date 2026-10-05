package com.association.caisse.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.association.caisse.data.local.dao.OtherIncomeWithCategory
import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.data.local.entity.PaymentEntity
import com.association.caisse.data.local.entity.RemittanceEntity
import com.association.caisse.data.repository.IncomeRepository
import com.association.caisse.data.repository.MemberRepository
import com.association.caisse.data.repository.PaymentRepository
import com.association.caisse.domain.FeeAllocationEngine
import com.association.caisse.domain.MonthFeeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class RecentOperation(
    val id: String,
    val type: String, // "COTISATION" ou "AUTRE"
    val title: String,
    val subtitle: String,
    val date: String,
    val amount: Long
)

data class DashboardUiState(
    val feeCurrentMonth: Long = 0L,
    val otherCurrentMonth: Long = 0L,
    val totalCurrentMonth: Long = 0L,
    val feeCurrentYear: Long = 0L,
    val otherCurrentYear: Long = 0L,
    val totalCurrentYear: Long = 0L,
    val remainingToRemit: Long = 0L,
    val lastRemittanceDate: String? = null,
    val upToDateMembersCount: Int = 0,
    val lateMembersCount: Int = 0,
    val totalUnpaidDues: Long = 0L,
    val recentOperations: List<RecentOperation> = emptyList(),
    val lastBackupDate: String? = null,
    val isBackupOverdue: Boolean = false,
    val currency: String = "Ar"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val incomeRepository: IncomeRepository,
    private val memberRepository: MemberRepository,
    private val feeEngine: FeeAllocationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val currentMonthStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    private val currentYearStr = LocalDate.now().year.toString()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                paymentRepository.getAllPayments(),
                incomeRepository.getAllOtherIncomes(),
                memberRepository.getActiveMembers(),
                paymentRepository.getAllAllocations(),
                paymentRepository.getAllFeeRates()
            ) { payments, otherIncomes, activeMembers, allocations, rates ->
                // 1. Calculs des encaissements
                val feeMonth = payments.filter { it.date.startsWith(currentMonthStr) }.sumOf { it.amount }
                val feeYear = payments.filter { it.date.startsWith(currentYearStr) }.sumOf { it.amount }

                val otherMonth = otherIncomes.filter { it.income.date.startsWith(currentMonthStr) }.sumOf { it.income.amount }
                val otherYear = otherIncomes.filter { it.income.date.startsWith(currentYearStr) }.sumOf { it.income.amount }

                val totalAllFee = payments.sumOf { it.amount }
                val totalAllOther = otherIncomes.sumOf { it.income.amount }
                val totalCollectedAllTime = totalAllFee + totalAllOther

                // 2. Calculs des membres à jour vs en retard
                var lateCount = 0
                var upToDateCount = 0
                var totalUnpaid = 0L

                for (member in activeMembers) {
                    val balances = feeEngine.computeMemberMonthlyBalances(
                        member = member,
                        allocations = allocations,
                        rates = rates,
                        currentMonth = currentMonthStr
                    )
                    val unpaidDues = balances
                        .filter { it.status == MonthFeeStatus.UNPAID || it.status == MonthFeeStatus.PARTIAL }
                        .sumOf { it.remainingAmount }

                    if (unpaidDues > 0) {
                        lateCount++
                        totalUnpaid += unpaidDues
                    } else {
                        upToDateCount++
                    }
                }

                // 3. Dernières opérations (fusionnées chronologiquement)
                val feeOps = payments.map { p ->
                    val mem = activeMembers.find { it.id == p.memberId }
                    RecentOperation(
                        id = "p-${p.id}",
                        type = "COTISATION",
                        title = mem?.let { "${it.firstName} ${it.lastName}" } ?: "Cotisation",
                        subtitle = p.note.ifBlank { "Cotisation mensuelle" },
                        date = p.date,
                        amount = p.amount
                    )
                }

                val otherOps = otherIncomes.map { o ->
                    RecentOperation(
                        id = "o-${o.income.id}",
                        type = "AUTRE",
                        title = o.category?.name ?: "Autre Recette",
                        subtitle = o.income.description.ifBlank { "Recette en espèces" },
                        date = o.income.date,
                        amount = o.income.amount
                    )
                }

                val recent = (feeOps + otherOps)
                    .sortedByDescending { it.date }
                    .take(5)

                DashboardUiState(
                    feeCurrentMonth = feeMonth,
                    otherCurrentMonth = otherMonth,
                    totalCurrentMonth = feeMonth + otherMonth,
                    feeCurrentYear = feeYear,
                    otherCurrentYear = otherYear,
                    totalCurrentYear = feeYear + otherYear,
                    remainingToRemit = totalCollectedAllTime, // Soustrait les versements réels dans le repo versement
                    upToDateMembersCount = upToDateCount,
                    lateMembersCount = lateCount,
                    totalUnpaidDues = totalUnpaid,
                    recentOperations = recent,
                    currency = "Ar"
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
