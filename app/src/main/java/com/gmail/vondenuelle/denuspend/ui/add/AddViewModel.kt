package com.gmail.vondenuelle.denuspend.ui.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.ErrorModel
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.data.repositories.TransactionRepository
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionOverviewModel
import com.gmail.vondenuelle.denuspend.navigation.AddScreens
import com.gmail.vondenuelle.denuspend.navigation.AppRootScreens
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.OnNavigate
import com.gmail.vondenuelle.denuspend.utils.SnackbarEvent
import com.gmail.vondenuelle.denuspend.utils.network.ResultState
import com.gmail.vondenuelle.denuspend.utils.network.asResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val TAG = AddViewModel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<AddScreenState>(AddScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    private var overviewJob: Job? = null
    private var perDayJob : Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)

    fun stopOverviewListener() {
        overviewJob?.cancel()
        overviewJob = null
    }
    fun stopPerDayListener() {
        perDayJob?.cancel()
        perDayJob = null
    }

    fun onEvent(event: AddScreenEvents) {
        when (event) {
            is AddScreenEvents.OnNavigateToIncomeScreen -> {
                sendEvent(OnNavigate(AddScreens.AddIncomeScreenNavigation))
            }

            is AddScreenEvents.OnNavigateToRecentTransactions -> {
                sendEvent(OnNavigate(AddScreens.AllRecentTransactionsNavigation))
            }

            is AddScreenEvents.OnNavigateToExpenseScreen -> {
                sendEvent(OnNavigate(AddScreens.AddExpenseScreenNavigation))
            }

            is AddScreenEvents.OnAddTransaction -> {
                viewModelScope.launch {
                    try {
                        _stateFlow.update { it.copy(isLoading = true) }
                        transactionRepository.addTransaction(event.request)
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = if (event.request.amount > 0) "Income Added" else "Expense Added")))
                        sendEvent(OneTimeEvents.OnPopBackStack)
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            AddScreenEvents.OnGetDailyTransactionHistory -> {
                if (overviewJob != null) {
                    viewModelScope.launch {
                        _stateFlow.update { it.copy(isListLoading = true) }
                        delay(500)
                        _stateFlow.update { it.copy(isListLoading = false) }
                    }
                    return
                } // prevent duplicate

                overviewJob =
                viewModelScope.launch {
                    _stateFlow.update { it.copy(isListLoading = true) }
                    delay(500)
                    transactionRepository.getDailyTransactionHistory(null).asResult()
                        .onEach { res ->
                            when (res) {
                                ResultState.Completed -> _stateFlow.update { it.copy(isListLoading = false) }
                                is ResultState.Error -> onError(res.exception)
                                ResultState.Loading -> _stateFlow.update { it.copy(isListLoading = true) }
                                is ResultState.Success -> _stateFlow.update {
                                    it.copy(
                                        dailyHistoryList = res.data,
                                        isListLoading = false
                                    )
                                }
                            }
                        }.collect()
                }
            }

            AddScreenEvents.OnGetTransactionOverview -> {
                if (overviewJob != null) {
                    viewModelScope.launch {
                        _stateFlow.update { it.copy(isListLoading = true) }
                        delay(500)
                        _stateFlow.update { it.copy(isListLoading = false) }
                    }
                    return
                } // prevent duplicate

                overviewJob = viewModelScope.launch {
                    _stateFlow.update { it.copy(isListLoading = true) }
                    delay(500)

                    transactionRepository.getTodayOverview().asResult().onEach { res ->
                        when (res) {
                            ResultState.Completed -> _stateFlow.update { it.copy(isListLoading = false) }
                            is ResultState.Error -> onError(res.exception)
                            ResultState.Loading -> _stateFlow.update { it.copy(isListLoading = true) }
                            is ResultState.Success -> _stateFlow.update {
                                it.copy(
                                    transactionList = res.data.transactions,
                                    dailyHistory = res.data.dailyHistory ?: DailyHistoryModel(),
                                    isListLoading = false
                                )
                            }
                        }
                    }.collect()
                }
            }

            is AddScreenEvents.OnGetTransactionsForDay -> {
                perDayJob = viewModelScope.launch {
                    _stateFlow.update { it.copy(isLoading = true, perDayTransactionList = emptyList()) }

                    transactionRepository.getTransactionsForDay(event.request).asResult().onEach { res ->
                        when (res) {
                            ResultState.Completed -> _stateFlow.update { it.copy(isLoading = false) }
                            is ResultState.Error -> onError(res.exception)
                            ResultState.Loading -> _stateFlow.update { it.copy(isLoading = true) }
                            is ResultState.Success -> _stateFlow.update {
                                it.copy(
                                    perDayTransactionList = res.data,
                                    isLoading = false
                                )
                            }
                        }
                    }.collect()
                }
            }

            is AddScreenEvents.OnAmountChanged -> _stateFlow.update { it.copy(transactionAmount = event.value) }
            is AddScreenEvents.OnDescriptionChanged -> _stateFlow.update {
                it.copy(
                    transactionDescription = event.value
                )
            }

            is AddScreenEvents.OnTitleChanged -> _stateFlow.update { it.copy(transactionTitle = event.value) }
            is AddScreenEvents.OnCategoryChanged -> _stateFlow.update { it.copy(transactionCategory = event.value) }
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
            is HttpException -> {
                val statusCode = e.code()
                val errorBody = e.response()?.errorBody()
                val gson = Gson()
                val type = object : TypeToken<ErrorModel>() {}.type
                val errorResponse: ErrorModel? = gson.fromJson(errorBody?.charStream(), type)

                // Example: Handle specific status
                when (statusCode) {
                    401 -> {
//                        sendEvent(OneTimeEvents.OnNavigate(AuthScreens.LoginNavigation))
                        return
                    }
                }

                //if this is not null, then there is a message regarding bad request of params
                if (errorResponse?.errors != null) {
                    _stateFlow.update {
                        it.copy(
                        )
                    }
                    sendEvent(OneTimeEvents.ShowInputError(errorResponse.errors))
                } else if (errorResponse?.message != null) {
                    sendEvent(OneTimeEvents.ShowError(errorResponse.message))
                }
            }

            is NoUserException -> {
                //send to login
                //remove stored creds
                sendEvent(OneTimeEvents.ShowToast("No user is signed in"))
                logout()
                sendEvent(
                    OneTimeEvents.OnNavigate(
                        AppRootScreens.AuthTopLevel,
                        behavior = NavBehavior.ClearAll
                    )
                )
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