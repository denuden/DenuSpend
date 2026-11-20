package com.gmail.vondenuelle.denuspend.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.repositories.BudgetRepository
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
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
                stopGetBudgetSummaryListener()
                getBudgetSummary()
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
                stopGetBudgetSummaryListener()
                getBudgetSummary()
            }
            is BudgetScreenEvents.OnShowDatePicker -> {
                _stateFlow.update {
                    it.copy(
                        showDateDialog = event.value
                    )
                }
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