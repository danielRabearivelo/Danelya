package com.association.caisse.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.association.caisse.data.local.dao.ReminderDao
import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.ReminderChannel
import com.association.caisse.data.local.entity.ReminderLogEntity
import com.association.caisse.data.repository.MemberRepository
import com.association.caisse.data.repository.PaymentRepository
import com.association.caisse.domain.FeeAllocationEngine
import com.association.caisse.domain.MonthBalanceDetail
import com.association.caisse.domain.MonthFeeStatus
import com.association.caisse.domain.ReminderHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class LateMemberItem(
    val member: MemberEntity,
    val unpaidMonths: List<MonthBalanceDetail>,
    val totalDue: Long,
    val oldestUnpaidMonth: String,
    val lastReminderDate: String?,
    val lastReminderTimestamp: Long?,
    val isPhoneMissing: Boolean
)

enum class RemindersSortOrder {
    AMOUNT_DESC,
    SENIORITY_DESC
}

data class RemindersUiState(
    val lateMembers: List<LateMemberItem> = emptyList(),
    val sortOrder: RemindersSortOrder = RemindersSortOrder.AMOUNT_DESC,
    val lateThresholdMonths: Int = 1,
    val minReminderIntervalDays: Int = 7,
    val defaultPrefix: String = "+261",
    val associationName: String = "Notre Association",
    val currency: String = "Ar",
    val messageTemplate: String = "Bonjour {prenom}, sauf erreur de notre part, il vous reste {montant_du} de cotisation à régler pour : {mois_impayes}. Merci ! – {association}",
    // État de la file séquentielle en série
    val isBatchQueueActive: Boolean = false,
    val batchQueueIndex: Int = 0,
    val lastLoggedReminderId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val paymentRepository: PaymentRepository,
    private val reminderDao: ReminderDao,
    private val feeEngine: FeeAllocationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    init {
        loadLateMembers()
    }

    private fun loadLateMembers() {
        viewModelScope.launch {
            combine(
                memberRepository.getActiveMembers(),
                paymentRepository.getAllAllocations(),
                paymentRepository.getAllFeeRates(),
                reminderDao.getAllReminderLogs()
            ) { members, allocations, rates, allLogs ->
                val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val items = mutableListOf<LateMemberItem>()

                for (member in members) {
                    val balances = feeEngine.computeMemberMonthlyBalances(
                        member = member,
                        allocations = allocations,
                        rates = rates,
                        currentMonth = currentMonth
                    )

                    val unpaid = balances.filter {
                        it.status == MonthFeeStatus.UNPAID || it.status == MonthFeeStatus.PARTIAL
                    }

                    if (unpaid.size >= _uiState.value.lateThresholdMonths) {
                        val totalDue = unpaid.sumOf { it.remainingAmount }
                        val memberLogs = allLogs.filter { it.memberId == member.id }
                        val lastLog = memberLogs.maxByOrNull { it.createdAt }

                        items.add(
                            LateMemberItem(
                                member = member,
                                unpaidMonths = unpaid,
                                totalDue = totalDue,
                                oldestUnpaidMonth = unpaid.firstOrNull()?.month ?: "",
                                lastReminderDate = lastLog?.date,
                                lastReminderTimestamp = lastLog?.createdAt,
                                isPhoneMissing = member.phone.isBlank()
                            )
                        )
                    }
                }

                sortList(items, _uiState.value.sortOrder)
            }.collect { sortedList ->
                _uiState.update { it.copy(lateMembers = sortedList, isLoading = false) }
            }
        }
    }

    fun onSortOrderChanged(newOrder: RemindersSortOrder) {
        _uiState.update { state ->
            val sorted = sortList(state.lateMembers.toMutableList(), newOrder)
            state.copy(sortOrder = newOrder, lateMembers = sorted)
        }
    }

    private fun sortList(list: MutableList<LateMemberItem>, order: RemindersSortOrder): List<LateMemberItem> {
        when (order) {
            RemindersSortOrder.AMOUNT_DESC -> list.sortByDescending { it.totalDue }
            RemindersSortOrder.SENIORITY_DESC -> list.sortBy { it.oldestUnpaidMonth }
        }
        return list
    }

    fun buildMessageForMember(item: LateMemberItem): String {
        val unpaidFormatted = item.unpaidMonths.joinToString(", ") { it.month }
        val amountFormatted = "${item.totalDue} ${_uiState.value.currency}"

        return ReminderHelper.buildReminderMessage(
            template = _uiState.value.messageTemplate,
            firstName = item.member.firstName,
            lastName = item.member.lastName,
            amountDueFormatted = amountFormatted,
            unpaidMonthsFormatted = unpaidFormatted,
            associationName = _uiState.value.associationName,
            currency = _uiState.value.currency
        )
    }

    fun logReminderAction(memberId: Long, channel: ReminderChannel, amountDue: Long) {
        viewModelScope.launch {
            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            val id = reminderDao.insertReminderLog(
                ReminderLogEntity(
                    memberId = memberId,
                    date = nowStr,
                    channel = channel,
                    amountDueAtTime = amountDue
                )
            )
            _uiState.update { it.copy(lastLoggedReminderId = id) }
        }
    }

    fun undoLastReminderLog() {
        val lastId = _uiState.value.lastLoggedReminderId ?: return
        viewModelScope.launch {
            reminderDao.deleteReminderLogById(lastId)
            _uiState.update { it.copy(lastLoggedReminderId = null) }
        }
    }

    // --- Gestion de la file de relance en série ---

    fun startBatchQueue() {
        if (_uiState.value.lateMembers.isNotEmpty()) {
            _uiState.update { it.copy(isBatchQueueActive = true, batchQueueIndex = 0) }
        }
    }

    fun nextBatchItem() {
        _uiState.update { state ->
            val nextIndex = state.batchQueueIndex + 1
            if (nextIndex >= state.lateMembers.size) {
                state.copy(isBatchQueueActive = false, batchQueueIndex = 0)
            } else {
                state.copy(batchQueueIndex = nextIndex)
            }
        }
    }

    fun cancelBatchQueue() {
        _uiState.update { it.copy(isBatchQueueActive = false, batchQueueIndex = 0) }
    }
}
