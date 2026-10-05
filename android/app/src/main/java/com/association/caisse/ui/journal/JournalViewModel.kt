package com.association.caisse.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.association.caisse.data.local.dao.OtherIncomeWithCategory
import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.data.local.entity.PaymentEntity
import com.association.caisse.data.repository.IncomeRepository
import com.association.caisse.data.repository.MemberRepository
import com.association.caisse.data.repository.PaymentRepository
import com.association.caisse.domain.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalEntry(
    val id: String,
    val type: String, // "COTISATION" ou "AUTRE"
    val date: String,
    val memberName: String,
    val detail: String,
    val amount: Long,
    val note: String
)

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val filteredEntries: List<JournalEntry> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
    val selectedTypeFilter: String = "ALL", // "ALL", "COTISATION", "AUTRE"
    val selectedMemberFilter: Long? = null,
    val totalAmount: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val incomeRepository: IncomeRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private var rawPayments: List<PaymentEntity> = emptyList()
    private var rawAllocations: List<PaymentAllocationEntity> = emptyList()
    private var rawIncomes: List<OtherIncomeWithCategory> = emptyList()
    private var rawMembers: List<MemberEntity> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                paymentRepository.getAllPayments(),
                paymentRepository.getAllAllocations(),
                incomeRepository.getAllOtherIncomes(),
                memberRepository.getAllMembers()
            ) { payments, allocations, incomes, members ->
                rawPayments = payments
                rawAllocations = allocations
                rawIncomes = incomes
                rawMembers = members

                val list = mutableListOf<JournalEntry>()

                for (p in payments) {
                    val m = members.find { it.id == p.memberId }
                    val name = m?.let { "${it.lastName.uppercase()} ${it.firstName}" } ?: "Inconnu"
                    val allocs = allocations.filter { it.paymentId == p.id }.joinToString(", ") { "${it.month} (${it.amount})" }
                    list.add(
                        JournalEntry(
                            id = "p-${p.id}",
                            type = "COTISATION",
                            date = p.date,
                            memberName = name,
                            detail = allocs.ifBlank { "Cotisation" },
                            amount = p.amount,
                            note = p.note
                        )
                    )
                }

                for (item in incomes) {
                    val inc = item.income
                    val catName = item.category?.name ?: "Autre"
                    val m = inc.memberId?.let { id -> members.find { it.id == id } }
                    val name = m?.let { "${it.lastName.uppercase()} ${it.firstName}" } ?: "Association / Tiers"
                    list.add(
                        JournalEntry(
                            id = "o-${inc.id}",
                            type = "AUTRE",
                            date = inc.date,
                            memberName = name,
                            detail = catName,
                            amount = inc.amount,
                            note = inc.description
                        )
                    )
                }

                list.sortByDescending { it.date }
                val filtered = applyFilters(list, _uiState.value.selectedTypeFilter, _uiState.value.selectedMemberFilter)

                JournalUiState(
                    entries = list,
                    filteredEntries = filtered,
                    members = members,
                    selectedTypeFilter = _uiState.value.selectedTypeFilter,
                    selectedMemberFilter = _uiState.value.selectedMemberFilter,
                    totalAmount = filtered.sumOf { it.amount },
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onTypeFilterChanged(type: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.entries, type, state.selectedMemberFilter)
            state.copy(selectedTypeFilter = type, filteredEntries = filtered, totalAmount = filtered.sumOf { it.amount })
        }
    }

    fun onMemberFilterChanged(memberId: Long?) {
        _uiState.update { state ->
            val filtered = applyFilters(state.entries, state.selectedTypeFilter, memberId)
            state.copy(selectedMemberFilter = memberId, filteredEntries = filtered, totalAmount = filtered.sumOf { it.amount })
        }
    }

    private fun applyFilters(entries: List<JournalEntry>, type: String, memberId: Long?): List<JournalEntry> {
        return entries.filter { entry ->
            val matchesType = when (type) {
                "COTISATION" -> entry.type == "COTISATION"
                "AUTRE" -> entry.type == "AUTRE"
                else -> true
            }
            // Filtrage membre
            val matchesMember = if (memberId == null) {
                true
            } else {
                val targetMember = rawMembers.find { it.id == memberId }
                val targetName = targetMember?.let { "${it.lastName.uppercase()} ${it.firstName}" }
                entry.memberName == targetName
            }
            matchesType && matchesMember
        }
    }

    fun generateCsvContent(): String {
        return CsvExporter.exportReceiptsJournalCsv(
            payments = rawPayments,
            allocations = rawAllocations,
            otherIncomes = rawIncomes,
            members = rawMembers
        )
    }
}
