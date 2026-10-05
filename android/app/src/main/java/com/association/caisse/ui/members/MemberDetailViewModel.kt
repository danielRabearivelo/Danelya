package com.association.caisse.ui.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.data.local.entity.PaymentEntity
import com.association.caisse.data.local.entity.ReminderLogEntity
import com.association.caisse.data.repository.MemberRepository
import com.association.caisse.data.repository.PaymentRepository
import com.association.caisse.domain.FeeAllocationEngine
import com.association.caisse.domain.MonthBalanceDetail
import com.association.caisse.domain.MonthFeeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PaymentWithAllocations(
    val payment: PaymentEntity,
    val allocations: List<PaymentAllocationEntity>
)

data class MemberDetailUiState(
    val member: MemberEntity? = null,
    val monthlyBalances: List<MonthBalanceDetail> = emptyList(),
    val yearBalances: Map<String, MonthFeeStatus> = emptyMap(), // Ex. "2026-01" -> PAID
    val totalDue: Long = 0L,
    val totalPaid: Long = 0L,
    val balanceDue: Long = 0L,
    val payments: List<PaymentWithAllocations> = emptyList(),
    val reminders: List<ReminderLogEntity> = emptyList(),
    val selectedYear: String = LocalDate.now().year.toString(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val paymentRepository: PaymentRepository,
    private val feeEngine: FeeAllocationEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val memberId: Long = checkNotNull(savedStateHandle["memberId"])
    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    init {
        loadMemberDetails()
    }

    private fun loadMemberDetails() {
        viewModelScope.launch {
            val member = memberRepository.getMemberById(memberId)
            if (member == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Membre introuvable.") }
                return@launch
            }

            combine(
                paymentRepository.getAllocationsForMember(memberId),
                paymentRepository.getPaymentsForMember(memberId),
                paymentRepository.getAllFeeRates()
            ) { allocations, payments, rates ->
                val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val balances = feeEngine.computeMemberMonthlyBalances(
                    member = member,
                    allocations = allocations,
                    rates = rates,
                    currentMonth = currentMonth
                )

                val due = balances.sumOf { it.dueAmount }
                val paid = balances.sumOf { it.paidAmount }
                val remaining = balances.sumOf { it.remainingAmount }

                val yearStatusMap = balances.associate { it.month to it.status }

                val paymentsWithAlloc = payments.map { p ->
                    PaymentWithAllocations(
                        payment = p,
                        allocations = allocations.filter { it.paymentId == p.id }
                    )
                }

                MemberDetailUiState(
                    member = member,
                    monthlyBalances = balances,
                    yearBalances = yearStatusMap,
                    totalDue = due,
                    totalPaid = paid,
                    balanceDue = remaining,
                    payments = paymentsWithAlloc,
                    selectedYear = LocalDate.now().year.toString(),
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onYearChanged(year: String) {
        _uiState.update { it.copy(selectedYear = year) }
    }

    fun toggleMemberActive() {
        val member = _uiState.value.member ?: return
        viewModelScope.launch {
            memberRepository.setMemberActive(member.id, !member.isActive)
            _uiState.update { it.copy(member = member.copy(isActive = !member.isActive)) }
        }
    }

    suspend fun deleteMemberSafely(): Boolean {
        val member = _uiState.value.member ?: return false
        return memberRepository.deleteMemberSafely(member)
    }
}
