package com.gmail.vondenuelle.denuspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.repositories.AuthRepository
import com.gmail.vondenuelle.denuspend.data.repositories.BudgetRepository
import com.gmail.vondenuelle.denuspend.data.repositories.TransactionRepository
import com.gmail.vondenuelle.denuspend.data.repositories.WalletRepository
import com.gmail.vondenuelle.denuspend.di.modules.TokenProvider
import com.gmail.vondenuelle.denuspend.domain.models.budget.MonthlyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.WalletScreens
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.network.ResultState
import com.gmail.vondenuelle.denuspend.utils.network.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewmodel @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val repository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val walletRepository: WalletRepository,
) : ViewModel() {
    private val TAG = HomeViewmodel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<HomeScreenState>(HomeScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    // Home is a tab: these stay live while the tab is saved, and ON_START only restarts a listener whose day/month has passed.
    private var todayJob: Job? = null // today's daily doc + its transactions (Today card, chips, Last added)
    private var todayDate: LocalDate? = null // the day todayJob is listening to
    private var monthJob: Job? = null // this month's monthly doc (November card)
    private var monthOf: YearMonth? = null // the month monthJob is listening to
    private var walletsJob: Job? = null // Total balance card

    fun stopTodayListener() {
        todayJob?.cancel()
        todayJob = null
    }
    fun stopMonthListener() {
        monthJob?.cancel()
        monthJob = null
    }
    fun stopWalletsListener() {
        walletsJob?.cancel()
        walletsJob = null
    }

    fun onEvent(event: HomeScreenEvents) {
        when (event) {
            is HomeScreenEvents.OnGetCurrentUser -> sendEvent(OneTimeEvents.ShowToast(message = tokenProvider.getName()))

            // Same query as the Add tab's overview, so the SDK shares one listener between them.
            // Past midnight it's still on yesterday's doc, so it restarts for the new day (same check as Add's overview).
            HomeScreenEvents.OnGetToday -> {
                val today = LocalDate.now()
                if (todayJob?.isActive == true && todayDate == today) return
                stopTodayListener()
                todayDate = today

                todayJob = viewModelScope.launch {
                    transactionRepository.getTodayOverview().asResult().onEach { res ->
                        when (res) {
                            ResultState.Loading -> _stateFlow.update { it.copy(isTodayLoading = true) }
                            ResultState.Completed -> _stateFlow.update { it.copy(isTodayLoading = false) }
                            is ResultState.Error -> {
                                _stateFlow.update { it.copy(isTodayLoading = false) }
                                onError(res.exception)
                            }
                            is ResultState.Success -> _stateFlow.update {
                                it.copy(
                                    todayHistory = res.data.dailyHistory,
                                    todayTransactions = res.data.transactions,
                                    isTodayLoading = false
                                )
                            }
                        }
                    }.collect()
                }
            }

            // Restarts on a new month the same way, since the doc id is "{uid}_yyyyMM"
            HomeScreenEvents.OnGetMonth -> {
                val month = YearMonth.now()
                if (monthJob?.isActive == true && monthOf == month) return
                stopMonthListener()
                monthOf = month

                monthJob = viewModelScope.launch {
                    budgetRepository.getMonthlyHistory(month).asResult().onEach { res ->
                        when (res) {
                            ResultState.Loading -> _stateFlow.update { it.copy(isMonthLoading = true) }
                            ResultState.Completed -> _stateFlow.update { it.copy(isMonthLoading = false) }
                            is ResultState.Error -> {
                                _stateFlow.update { it.copy(isMonthLoading = false) }
                                onError(res.exception)
                            }
                            // null = nothing added this month yet
                            is ResultState.Success -> _stateFlow.update {
                                it.copy(monthHistory = res.data ?: MonthlyHistoryModel(), isMonthLoading = false)
                            }
                        }
                    }.collect()
                }
            }

            // Same query as the Wallet tab's list, so it's shared too
            HomeScreenEvents.OnGetWallets -> {
                if (walletsJob?.isActive == true) return
                walletsJob = viewModelScope.launch {
                    walletRepository.getWallets().asResult().onEach { res ->
                        when (res) {
                            ResultState.Loading -> _stateFlow.update { it.copy(isWalletsLoading = true) }
                            ResultState.Completed -> _stateFlow.update { it.copy(isWalletsLoading = false) }
                            is ResultState.Error -> {
                                _stateFlow.update { it.copy(isWalletsLoading = false) }
                                onError(res.exception)
                            }
                            is ResultState.Success -> _stateFlow.update {
                                it.copy(wallets = res.data, isWalletsLoading = false)
                            }
                        }
                    }.collect()
                }
            }

            is HomeScreenEvents.OnOpenWalletDetails -> {
                sendEvent(OneTimeEvents.OnNavigate(WalletScreens.WalletDetailsNavigation(event.walletId)))
            }

            else -> Unit
        }
    }

    private fun onError(e: Throwable?) {
        sendEvent(OneTimeEvents.ShowError(e?.message.orEmpty()))
    }

    private fun sendEvent(event: OneTimeEvents) {
        viewModelScope.launch {
            _channel.send(event)
        }
    }
}
