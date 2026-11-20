package com.gmail.vondenuelle.denuspend.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.repositories.BudgetRepository
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.data.repositories.TransactionRepository
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.navigation.BudgetScreens
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.network.ResultState
import com.gmail.vondenuelle.denuspend.utils.network.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
): ViewModel(){
    private val TAG = BudgetViewModel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<BudgetScreenState>(BudgetScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    private var getBudgetSummaryJob : Job? = null //stopper for ux refresh

    fun stopGetBudgetSummaryListener() {
        getBudgetSummaryJob?.cancel()
        getBudgetSummaryJob = null
    }

    private var insightsJob : Job? = null

    fun stopInsightsListener() {
        insightsJob?.cancel()
        insightsJob = null
    }

    private var insightsMonthJob : Job? = null

    fun stopInsightsMonthListener() {
        insightsMonthJob?.cancel()
        insightsMonthJob = null
    }

    private var insightDayJob : Job? = null
    private var insightDayDocId = ""

    fun stopInsightDayListener() {
        insightDayJob?.cancel()
        insightDayJob = null
        // A cancel before the first result would leave the sheet's spinner on
        _stateFlow.update { it.copy(isInsightDaySheetLoading = false) }
    }

    // Back from the background with the day sheet still open: listen to that day again
    fun resumeInsightDayListener() {
        if (insightDayJob == null && insightDayDocId.isNotEmpty()) getInsightDayTransactions(insightDayDocId)
    }

    fun onEvent(event : BudgetScreenEvents) {
        when(event){
            is BudgetScreenEvents.OnNavigateToBudgetTransactionScreen -> {
                sendEvent(OneTimeEvents.OnNavigate(BudgetScreens.BudgetInsightsScreenNavigation(event.category)))
            }
            is BudgetScreenEvents.OnChangeFilter -> {
                // Custom waits for the user to pick a range in the dialog first
                if (event.filter == BudgetChartFilter.CUSTOM) {
                    _stateFlow.update { it.copy(showRangePicker = true) }
                    return
                }
                _stateFlow.update { it.copy(filter = event.filter) }
                restartForFilter()
            }
            BudgetScreenEvents.OnGetBudgetSummary -> {
                // Already live, so nothing to restart. isActive also covers a listener that ended with an error.
                if (getBudgetSummaryJob?.isActive == true) return
                getBudgetSummary()
            }
            is BudgetScreenEvents.OnShowRangePicker -> _stateFlow.update { it.copy(showRangePicker = event.value) }
            is BudgetScreenEvents.OnCustomRangeSelected -> {
                _stateFlow.update {
                    it.copy(
                        filter = BudgetChartFilter.CUSTOM,
                        showRangePicker = false,
                        customStartMillis = event.startMillis,
                        customEndMillis = event.endMillis
                    )
                }
                restartForFilter()
            }
            is BudgetScreenEvents.OnGetBudgetInsights -> {
                // Same category already live (e.g. ON_START after a rotation): nothing to restart
                if (!(insightsJob?.isActive == true && _stateFlow.value.insightsCategory == event.category)) {
                    _stateFlow.update { it.copy(insightsCategory = event.category) }
                    stopInsightsListener()
                    getBudgetInsights()
                }
                // A month that was open before ON_STOP gets its listener back too
                if (_stateFlow.value.insightsMonth != null && insightsMonthJob?.isActive != true) {
                    getInsightsMonthDays()
                }
            }
            is BudgetScreenEvents.OnOpenInsightsMonth -> {
                stopInsightsMonthListener()
                _stateFlow.update { it.copy(insightsMonth = event.month, insightsMonthDays = null) }
                if (event.month != null) getInsightsMonthDays()
            }
            is BudgetScreenEvents.OnGetInsightDayTransactions -> {
                // A new day replaces the old one's listener. A day without a doc has nothing to load.
                stopInsightDayListener()
                insightDayDocId = event.dailyDocId
                _stateFlow.update { it.copy(insightDayTransactions = emptyList()) }
                if (event.dailyDocId.isEmpty()) return
                getInsightDayTransactions(event.dailyDocId)
            }
        }
    }

    private fun getBudgetSummary() {
        getBudgetSummaryJob = viewModelScope.launch {
            _stateFlow.update { it.copy(isLoading = true) }
            delay(500)

            val current = _stateFlow.value
            budgetRepository.getBudgetSummary(current.filter, current.customStartMillis, current.customEndMillis).asResult()
                .onEach { res ->
                    when (res) {
                        ResultState.Completed -> _stateFlow.update { it.copy(isLoading = false) }
                        is ResultState.Error -> onError(res.exception)
                        ResultState.Loading -> _stateFlow.update { it.copy(isLoading = true) }
                        is ResultState.Success -> _stateFlow.update {
                            it.copy(
                                budgetTotalSummaryModel = res.data,
                                isLoading = false,
                            )
                        }
                    }
                }.collect()
        }
    }

    // The dashboard and Insights each get their own ViewModel instance, so a filter change restarts this instance's listener
    private fun restartForFilter() {
        if (_stateFlow.value.insightsCategory.isEmpty()) {
            stopGetBudgetSummaryListener()
            getBudgetSummary()
        } else {
            // A new range closes an opened month
            stopInsightsMonthListener()
            _stateFlow.update { it.copy(insightsMonth = null, insightsMonthDays = null) }
            stopInsightsListener()
            getBudgetInsights()
        }
    }

    private fun getInsightsMonthDays() {
        val current = _stateFlow.value
        val month = current.insightsMonth ?: return
        insightsMonthJob = viewModelScope.launch {
            budgetRepository.getBudgetInsightsMonthDays(current.insightsCategory, month, current.budgetInsights.endDate)
                .asResult()
                .onEach { res ->
                    when (res) {
                        ResultState.Completed -> _stateFlow.update { it.copy(isInsightsMonthLoading = false) }
                        is ResultState.Error -> {
                            _stateFlow.update { it.copy(isInsightsMonthLoading = false) }
                            onError(res.exception)
                        }
                        ResultState.Loading -> _stateFlow.update { it.copy(isInsightsMonthLoading = true) }
                        is ResultState.Success -> _stateFlow.update {
                            it.copy(insightsMonthDays = res.data, isInsightsMonthLoading = false)
                        }
                    }
                }.collect()
        }
    }

    private fun getBudgetInsights() {
        val current = _stateFlow.value
        insightsJob = viewModelScope.launch {
            budgetRepository.getBudgetInsights(
                current.insightsCategory,
                current.filter,
                current.customStartMillis,
                current.customEndMillis
            ).asResult()
                .onEach { res ->
                    when (res) {
                        ResultState.Completed -> _stateFlow.update { it.copy(isInsightsLoading = false) }
                        is ResultState.Error -> {
                            _stateFlow.update { it.copy(isInsightsLoading = false) }
                            onError(res.exception)
                        }
                        ResultState.Loading -> _stateFlow.update { it.copy(isInsightsLoading = true) }
                        is ResultState.Success -> _stateFlow.update {
                            it.copy(budgetInsights = res.data, isInsightsLoading = false)
                        }
                    }
                }.collect()
        }
    }

    // Reads the whole day (usually a few docs) and keeps this category's ones on the device
    private fun getInsightDayTransactions(dailyDocId: String) {
        insightDayJob = viewModelScope.launch {
            transactionRepository.getTransactionsForDay(TransactionsForDayRequest(dailyDocId = dailyDocId))
                .asResult()
                .onEach { res ->
                    when (res) {
                        ResultState.Completed -> _stateFlow.update { it.copy(isInsightDaySheetLoading = false) }
                        is ResultState.Error -> {
                            _stateFlow.update { it.copy(isInsightDaySheetLoading = false) }
                            onError(res.exception)
                        }
                        ResultState.Loading -> _stateFlow.update { it.copy(isInsightDaySheetLoading = true) }
                        is ResultState.Success -> _stateFlow.update { state ->
                            state.copy(
                                insightDayTransactions = res.data.filter { it.category == state.insightsCategory },
                                isInsightDaySheetLoading = false
                            )
                        }
                    }
                }.collect()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                profileRepository.logout()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }


    private fun onError(e: Throwable?) {
        when (e) {
            is NoUserException -> {
                // Signing out is enough. AppRoot swaps to the auth screens on its own.
                sendEvent(OneTimeEvents.ShowToast("No user is signed in"))
                logout()
            }

            else -> {
                sendEvent(OneTimeEvents.ShowError(e?.message.orEmpty()))
            }
        }
    }

    private fun sendEvent(event: OneTimeEvents) {
        viewModelScope.launch {
            _channel.send(event)
        }
    }
}