package com.association.caisse.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.MonthlyFeeRateEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.data.repository.MemberRepository
import com.association.caisse.data.repository.PaymentRepository
import com.association.caisse.domain.FeeAllocationEngine
import com.association.caisse.domain.MonthAllocationProposal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PaymentUiState(
    val members: List<MemberEntity> = emptyList(),
    val selectedMember: MemberEntity? = null,
    val amountInput: String = "10000",
    val dateInput: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val noteInput: String = "",
    val autoProposals: List<MonthAllocationProposal> = emptyList(),
    val manualAllocations: Map<String, Long> = emptyMap(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val memberRepository: MemberRepository,
    private val allocationEngine: FeeAllocationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var allAllocations: List<PaymentAllocationEntity> = emptyList()
    private var allRates: List<MonthlyFeeRateEntity> = emptyList()

    init {
        viewModelScope.launch {
            memberRepository.getActiveMembers().collect { memberList ->
                _uiState.update { it.copy(members = memberList) }
            }
        }

        viewModelScope.launch {
            paymentRepository.getAllAllocations().collect { allocList ->
                allAllocations = allocList
                recalculateProposals()
            }
        }

        viewModelScope.launch {
            paymentRepository.getAllFeeRates().collect { ratesList ->
                allRates = ratesList
                recalculateProposals()
            }
        }
    }

    fun onSelectMember(member: MemberEntity) {
        _uiState.update { it.copy(selectedMember = member, errorMessage = null) }
        recalculateProposals()
    }

    fun onAmountChanged(amountStr: String) {
        _uiState.update { it.copy(amountInput = amountStr, errorMessage = null) }
        recalculateProposals()
    }

    fun onDateChanged(dateStr: String) {
        _uiState.update { it.copy(dateInput = dateStr) }
    }

    fun onNoteChanged(noteStr: String) {
        _uiState.update { it.copy(noteInput = noteStr) }
    }

    fun onUpdateManualAllocation(month: String, amount: Long) {
        _uiState.update { state ->
            val updated = state.manualAllocations.toMutableMap()
            if (amount <= 0) {
                updated.remove(month)
            } else {
                updated[month] = amount
            }
            state.copy(manualAllocations = updated)
        }
    }

    private fun recalculateProposals() {
        val member = _uiState.value.selectedMember ?: return
        val amount = _uiState.value.amountInput.toLongOrNull() ?: 0L

        if (amount > 0) {
            val proposals = allocationEngine.calculateAutoAllocation(
                amountReceived = amount,
                member = member,
                existingAllocations = allAllocations,
                rates = allRates
            )
            _uiState.update { state ->
                // Initialise les ventilations modifiables avec les montants calculés
                val initialManual = proposals.associate { it.month to it.allocatedAmount }
                state.copy(autoProposals = proposals, manualAllocations = initialManual)
            }
        } else {
            _uiState.update { it.copy(autoProposals = emptyList(), manualAllocations = emptyMap()) }
        }
    }

    fun savePayment() {
        val state = _uiState.value
        val member = state.selectedMember
        if (member == null) {
            _uiState.update { it.copy(errorMessage = "Veuillez sélectionner un membre.") }
            return
        }

        val totalAmount = state.amountInput.toLongOrNull()
        if (totalAmount == null || totalAmount <= 0) {
            _uiState.update { it.copy(errorMessage = "Veuillez saisir un montant supérieur à 0.") }
            return
        }

        // Vérification de la somme des allocations
        val sumAllocations = state.manualAllocations.values.sum()
        if (sumAllocations != totalAmount) {
            _uiState.update {
                it.copy(
                    errorMessage = "La somme des ventilations ($sumAllocations) doit être égale au montant total reçu ($totalAmount)."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val allocationsList = state.manualAllocations.toList()
                paymentRepository.recordCashPayment(
                    memberId = member.id,
                    amount = totalAmount,
                    date = state.dateInput,
                    note = state.noteInput.trim(),
                    allocations = allocationsList
                )
                _uiState.update { it.copy(isSaving = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Erreur lors de l'enregistrement : ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
