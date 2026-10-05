package com.association.caisse.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.association.caisse.data.local.dao.OtherIncomeWithCategory
import com.association.caisse.data.local.entity.IncomeCategoryEntity
import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.OtherIncomeEntity
import com.association.caisse.data.repository.IncomeRepository
import com.association.caisse.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class OtherIncomeUiState(
    val incomes: List<OtherIncomeWithCategory> = emptyList(),
    val filteredIncomes: List<OtherIncomeWithCategory> = emptyList(),
    val categories: List<IncomeCategoryEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
    val selectedCategoryFilter: Long? = null,
    val selectedPeriodFilter: String = "ALL", // "ALL", "CURRENT_MONTH", "CURRENT_YEAR"
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class OtherIncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherIncomeUiState())
    val uiState: StateFlow<OtherIncomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            incomeRepository.getAllOtherIncomes().collect { incomeList ->
                _uiState.update { state ->
                    val filtered = applyFilters(incomeList, state.selectedCategoryFilter, state.selectedPeriodFilter)
                    state.copy(incomes = incomeList, filteredIncomes = filtered)
                }
            }
        }

        viewModelScope.launch {
            incomeRepository.getAllCategories().collect { catList ->
                _uiState.update { it.copy(categories = catList) }
            }
        }

        viewModelScope.launch {
            memberRepository.getAllMembers().collect { memberList ->
                _uiState.update { it.copy(members = memberList) }
            }
        }
    }

    fun onCategoryFilterChanged(categoryId: Long?) {
        _uiState.update { state ->
            val filtered = applyFilters(state.incomes, categoryId, state.selectedPeriodFilter)
            state.copy(selectedCategoryFilter = categoryId, filteredIncomes = filtered)
        }
    }

    fun onPeriodFilterChanged(period: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.incomes, state.selectedCategoryFilter, period)
            state.copy(selectedPeriodFilter = period, filteredIncomes = filtered)
        }
    }

    private fun applyFilters(
        list: List<OtherIncomeWithCategory>,
        categoryId: Long?,
        period: String
    ): List<OtherIncomeWithCategory> {
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val currentYear = LocalDate.now().year.toString()

        return list.filter { item ->
            val matchesCat = categoryId == null || item.income.categoryId == categoryId
            val matchesPeriod = when (period) {
                "CURRENT_MONTH" -> item.income.date.startsWith(currentMonth)
                "CURRENT_YEAR" -> item.income.date.startsWith(currentYear)
                else -> true
            }
            matchesCat && matchesPeriod
        }
    }

    fun addOtherIncome(
        categoryId: Long,
        memberId: Long?,
        amount: Long,
        date: String,
        description: String
    ) {
        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Le montant doit être supérieur à 0.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                incomeRepository.addOtherIncome(categoryId, memberId, date, amount, description)
                _uiState.update {
                    it.copy(isSaving = false, successMessage = "Recette enregistrée avec succès.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Erreur : ${e.localizedMessage}")
                }
            }
        }
    }

    fun deleteOtherIncome(income: OtherIncomeEntity) {
        viewModelScope.launch {
            try {
                incomeRepository.deleteOtherIncome(income)
                _uiState.update { it.copy(successMessage = "Recette supprimée.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur de suppression : ${e.localizedMessage}") }
            }
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            incomeRepository.addCategory(name)
        }
    }

    fun updateCategory(category: IncomeCategoryEntity) {
        viewModelScope.launch {
            incomeRepository.updateCategory(category)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
