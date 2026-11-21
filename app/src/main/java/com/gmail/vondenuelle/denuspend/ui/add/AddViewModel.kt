package com.gmail.vondenuelle.denuspend.ui.add

import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateItem
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.EditTransactionRequest
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.data.repositories.TransactionRepository
import com.gmail.vondenuelle.denuspend.data.repositories.WalletRepository
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionOverviewModel
import com.gmail.vondenuelle.denuspend.navigation.AddScreens
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.OnNavigate
import com.gmail.vondenuelle.denuspend.utils.SnackbarEvent
import com.gmail.vondenuelle.denuspend.utils.network.ResultState
import com.gmail.vondenuelle.denuspend.utils.network.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import javax.inject.Inject
import kotlin.math.abs
import java.time.LocalDate
import java.util.UUID

@HiltViewModel
class AddViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
) : ViewModel() {
    private val TAG = AddViewModel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<AddScreenState>(AddScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    private var overviewJob: Job? = null
    private var overviewDate: LocalDate? = null // the day overviewJob is listening to (its daily doc id is built from it)
    private var historyJob: Job? = null
    private var perDayJob : Job? = null
    private var templatesJob : Job? = null
    private var walletsJob : Job? = null // "Paid from" choices on the expense form
    private var todayJob : Job? = null // today's daily doc for the form's "left today" line

    // History grows by one page per load-more; the listener is restarted with the bigger limit.
    private var historyLimit = HISTORY_PAGE_SIZE

    companion object {
        private const val HISTORY_PAGE_SIZE = 20L
        private const val WALLETS_RETRY_MS = 3_000L
    }

    @OptIn(ExperimentalCoroutinesApi::class)

    fun stopOverviewListener() {
        overviewJob?.cancel()
        overviewJob = null
    }
    fun stopHistoryListener() {
        historyJob?.cancel()
        historyJob = null
    }
    fun stopPerDayListener() {
        perDayJob?.cancel()
        perDayJob = null
    }
    fun stopTemplatesListener() {
        templatesJob?.cancel()
        templatesJob = null
    }
    fun stopTodayListener() {
        todayJob?.cancel()
        todayJob = null
    }
    fun stopWalletsListener() {
        walletsJob?.cancel()
        walletsJob = null
    }

    fun onEvent(event: AddScreenEvents) {
        when (event) {
            is AddScreenEvents.OnNavigateToIncomeScreen -> {
                sendEvent(OnNavigate(AddScreens.AddIncomeScreenNavigation()))
            }

            is AddScreenEvents.OnNavigateToAllowanceTemplates -> {
                sendEvent(OnNavigate(AddScreens.AllowanceTemplatesNavigation))
            }

            AddScreenEvents.OnGetAllowanceTemplates -> {
                if (templatesJob?.isActive == true) return // already live
                templatesJob = viewModelScope.launch {
                    transactionRepository.getAllowanceTemplates().asResult().onEach { res ->
                        when (res) {
                            ResultState.Completed, ResultState.Loading -> Unit
                            is ResultState.Error -> onError(res.exception)
                            is ResultState.Success -> _stateFlow.update {
                                it.copy(allowanceTemplates = res.data, hasLoadedTemplates = true)
                            }
                        }
                    }.collect()
                }
            }

            // Fills dailyHistory on the form's own VM (the tab's overview isn't running there). No loading flag: it's one line of text.
            AddScreenEvents.OnGetTodayTotals -> {
                if (todayJob?.isActive == true) return
                todayJob = viewModelScope.launch {
                    transactionRepository.getTodayDailyHistory().asResult().onEach { res ->
                        when (res) {
                            is ResultState.Success -> _stateFlow.update { it.copy(dailyHistory = res.data ?: DailyHistoryModel()) }
                            is ResultState.Error -> onError(res.exception)
                            else -> Unit
                        }
                    }.collect()
                }
            }

            AddScreenEvents.OnGetWallets -> {
                if (walletsJob?.isActive == true) return
                // Rare: in release testing (2026-11-21) the first wallet list never arrived on a freshly opened form, cause not found.
                // Re-adding the listener fixed it on the device, so if nothing came after WALLETS_RETRY_MS it restarts once.
                // The children live inside walletsJob, so stopWalletsListener() still stops both.
                walletsJob = viewModelScope.launch {
                    val firstTry = launch { collectWallets() }
                    delay(WALLETS_RETRY_MS)
                    if (!_stateFlow.value.hasLoadedWallets) {
                        firstTry.cancel()
                        launch { collectWallets() }
                    }
                }
            }

            is AddScreenEvents.OnWalletChanged -> _stateFlow.update {
                it.copy(transactionWalletId = event.walletId, walletPicked = true)
            }

            is AddScreenEvents.OnOpenTemplateEditor -> _stateFlow.update {
                it.copy(templateDraft = event.template ?: AllowanceTemplateModel(items = listOf(AllowanceTemplateItem(id = newId()))))
            }
            AddScreenEvents.OnCloseTemplateEditor -> _stateFlow.update { it.copy(templateDraft = null) }
            is AddScreenEvents.OnTemplateDraftChanged -> _stateFlow.update { it.copy(templateDraft = event.draft) }

            AddScreenEvents.OnSaveTemplateDraft -> {
                val current = _stateFlow.value
                val draft = current.templateDraft ?: return
                if (current.isLoading || !current.hasLoadedTemplates) return // ignore double taps, and wait for the list
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        val saved = if (draft.id.isEmpty()) draft.copy(id = newId()) else draft
                        val isNew = current.allowanceTemplates.none { it.id == saved.id }
                        val templates = if (isNew) current.allowanceTemplates + saved
                            else current.allowanceTemplates.map { if (it.id == saved.id) saved else it }
                        transactionRepository.saveAllowanceTemplates(templates)
                        _stateFlow.update { it.copy(isLoading = false, templateDraft = null) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.add_snackbar_template_saved))))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is AddScreenEvents.OnDeleteAllowanceTemplate -> {
                val current = _stateFlow.value
                if (current.isLoading || !current.hasLoadedTemplates) return
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        transactionRepository.saveAllowanceTemplates(current.allowanceTemplates.filter { it.id != event.template.id })
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.add_snackbar_template_deleted))))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is AddScreenEvents.OnApplyAllowanceTemplate -> {
                if (_stateFlow.value.isLoading) return // ignore double taps while saving
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        transactionRepository.applyAllowanceTemplate(event.template)
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.add_snackbar_template_applied, event.template.name))))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is AddScreenEvents.OnNavigateToRecentTransactions -> {
                sendEvent(OnNavigate(AddScreens.AllRecentTransactionsNavigation))
            }

            is AddScreenEvents.OnNavigateToExpenseScreen -> {
                sendEvent(OnNavigate(AddScreens.AddExpenseScreenNavigation()))
            }

            is AddScreenEvents.OnNavigateToEditTransaction -> {
                val docId = event.transaction.docId
                sendEvent(
                    OnNavigate(
                        if (event.transaction.amount > 0) AddScreens.AddIncomeScreenNavigation(docId, event.dailyDocId)
                        else AddScreens.AddExpenseScreenNavigation(docId, event.dailyDocId)
                    )
                )
            }

            is AddScreenEvents.OnLoadTransactionForEdit -> {
                // Already loaded (e.g. after rotation), so keep the user's unsaved edits
                if (_stateFlow.value.editingTransaction?.docId == event.docId || _stateFlow.value.isLoading) return
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        val transaction = transactionRepository.getTransactionById(event.docId)
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                editingTransaction = transaction,
                                editingDailyDocId = event.dailyDocId,
                                transactionTitle = transaction.title,
                                transactionDescription = if (transaction.description == "---") "" else transaction.description,
                                transactionAmount = abs(transaction.amount),
                                transactionCategory = transaction.category,
                                transactionWalletId = transaction.walletId,
                                walletPicked = true
                            )
                        }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            AddScreenEvents.OnUpdateTransaction -> {
                val current = _stateFlow.value
                val original = current.editingTransaction ?: return
                if (current.isLoading) return // ignore double taps while saving
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        transactionRepository.updateTransaction(
                            EditTransactionRequest(
                                docId = original.docId,
                                dailyDocId = current.editingDailyDocId,
                                oldAmount = original.amount,
                                oldCategory = original.category,
                                title = current.transactionTitle,
                                description = current.transactionDescription,
                                // keep the original sign, the form only holds the positive amount
                                amount = if (original.amount < 0) -current.transactionAmount else current.transactionAmount,
                                category = current.transactionCategory,
                                walletId = current.transactionWalletId,
                                oldWalletId = original.walletId
                            )
                        )
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.add_snackbar_transaction_updated))))
                        sendEvent(OneTimeEvents.OnPopBackStack)
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is AddScreenEvents.OnDeleteTransaction -> {
                if (_stateFlow.value.isLoading) return // ignore double taps while deleting
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        transactionRepository.deleteTransaction(event.transaction, event.dailyDocId)
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.add_snackbar_transaction_deleted))))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is AddScreenEvents.OnAddTransaction -> {
                if (_stateFlow.value.isLoading) return // ignore double taps while saving
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        transactionRepository.addTransaction(event.request)
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(if (event.request.amount > 0) R.string.add_snackbar_allowance_added else R.string.add_snackbar_expense_added))))
                        sendEvent(OneTimeEvents.OnPopBackStack)
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            AddScreenEvents.OnGetDailyTransactionHistory -> {
                if (historyJob != null) {
                    viewModelScope.launch {
                        _stateFlow.update { it.copy(isListLoading = true) }
                        delay(500)
                        _stateFlow.update { it.copy(isListLoading = false) }
                    }
                    return
                } // prevent duplicate when doing ux reloading

                historyJob =
                viewModelScope.launch {
                    _stateFlow.update { it.copy(isListLoading = true) }
                    delay(500)
                    collectDailyHistory(isLoadMore = false)
                }
            }

            AddScreenEvents.OnLoadMoreHistory -> {
                val current = _stateFlow.value
                if (!current.hasMoreHistory || current.isLoadingMoreHistory || current.isListLoading) return

                historyLimit += HISTORY_PAGE_SIZE
                stopHistoryListener()
                historyJob = viewModelScope.launch {
                    _stateFlow.update { it.copy(isLoadingMoreHistory = true) }
                    collectDailyHistory(isLoadMore = true)
                }
            }

            AddScreenEvents.OnRefreshOverview -> {
                if (_stateFlow.value.isRefreshing) return
                // The listeners are already live, so there's nothing to re-read. This only restarts what needs it:
                // a new day after midnight, or a listener that ended after an error. The short spinner is just feedback.
                onEvent(AddScreenEvents.OnGetTransactionOverview)
                onEvent(AddScreenEvents.OnGetAllowanceTemplates)
                viewModelScope.launch {
                    _stateFlow.update { it.copy(isRefreshing = true) }
                    delay(500)
                    _stateFlow.update { it.copy(isRefreshing = false) }
                }
            }

            AddScreenEvents.OnGetTransactionOverview -> {
                // Already live (the tab keeps its listener), so coming back to the tab doesn't flash a loading state or re-read.
                // Past midnight it's still listening to yesterday's doc, so restart it for the new day instead.
                val today = LocalDate.now()
                if (overviewJob?.isActive == true && overviewDate == today) return
                stopOverviewListener()
                overviewDate = today

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
                stopPerDayListener() // drop the previous day's listener before starting a new one
                perDayJob = viewModelScope.launch {
                    _stateFlow.update { it.copy(isSheetLoading = true, perDayTransactionList = emptyList()) }

                    transactionRepository.getTransactionsForDay(event.request).asResult().onEach { res ->
                        when (res) {
                            ResultState.Completed -> _stateFlow.update { it.copy(isSheetLoading = false) }
                            is ResultState.Error -> onError(res.exception)
                            ResultState.Loading -> _stateFlow.update { it.copy(isSheetLoading = true) }
                            is ResultState.Success -> _stateFlow.update {
                                it.copy(
                                    perDayTransactionList = res.data,
                                    isSheetLoading = false
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

    /**
     * Collects the daily history listener with the current [historyLimit].
     * Load-more skips the full skeleton (isListLoading) so the list and scroll position stay on screen.
     */
    private suspend fun collectDailyHistory(isLoadMore: Boolean) {
        transactionRepository.getDailyTransactionHistory(historyLimit).asResult()
            .onEach { res ->
                when (res) {
                    ResultState.Completed -> _stateFlow.update { it.copy(isListLoading = false, isLoadingMoreHistory = false) }
                    is ResultState.Error -> onError(res.exception)
                    ResultState.Loading -> if (!isLoadMore) _stateFlow.update { it.copy(isListLoading = true) }
                    is ResultState.Success -> _stateFlow.update {
                        it.copy(
                            dailyHistoryList = res.data,
                            hasMoreHistory = res.data.size.toLong() == historyLimit,
                            isListLoading = false,
                            isLoadingMoreHistory = false
                        )
                    }
                }
            }.collect()
    }

    private fun newId() = UUID.randomUUID().toString()

    private fun logout() {
        viewModelScope.launch {
            try {
                profileRepository.logout()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private suspend fun collectWallets() {
        walletRepository.getWallets().asResult().onEach { res ->
            when (res) {
                ResultState.Completed, ResultState.Loading -> Unit
                is ResultState.Error -> onError(res.exception)
                // A new expense starts on the physical wallet, until the user picks something else
                is ResultState.Success -> _stateFlow.update { state ->
                    val default = if (state.walletPicked) state.transactionWalletId
                        else res.data.firstOrNull { it.isPhysical }?.docId.orEmpty()
                    state.copy(wallets = res.data, hasLoadedWallets = true, transactionWalletId = default)
                }
            }
        }.collect()
    }

    private fun onError(e: Throwable?) {
        when (e) {
            is NoUserException -> {
                // Signing out is enough. AppRoot swaps to the auth screens on its own.
                sendEvent(OneTimeEvents.ShowToast(context.getString(R.string.error_no_user_signed_in)))
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