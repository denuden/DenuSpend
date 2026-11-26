package com.gmail.vondenuelle.denuspend.ui.wallet

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletMovementRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.data.repositories.WalletRepository
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.navigation.WalletScreens
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletMovementPresets
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletStyle
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.OnNavigate
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import com.gmail.vondenuelle.denuspend.utils.SnackbarEvent
import com.gmail.vondenuelle.denuspend.utils.network.ResultState
import com.gmail.vondenuelle.denuspend.utils.network.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate

/**
 * One ViewModel for the wallet module: the Wallet tab, wallet details and the first-time setup.
 * Each destination gets its own instance (hiltViewModel()), like the Add module.
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow(
        WalletScreenState(physicalDraft = WalletModel(name = context.getString(R.string.wallet_default_physical_name), iconKey = "wallet"))
    )
    val stateFlow = _stateFlow.asStateFlow()

    private var walletsJob: Job? = null
    private var transactionsJob: Job? = null
    private var movementsJob: Job? = null
    private var statementJob: Job? = null
    private var recurringJob: Job? = null
    private var dueJob: Job? = null // charges since the last close, only for cards inside their due window (the red "!")
    private var dueCardsKey: List<Any>? = null // the cards dueJob listens to, so a balance change doesn't restart it
    private var statementSince: LocalDate? = null // the range the statement listener runs for, so the same range isn't restarted

    // Expenses paid from a wallet grow by one page per load-more; the listener restarts with the bigger limit
    private var transactionsLimit = TRANSACTIONS_PAGE_SIZE
    private var movementsLimit = TRANSACTIONS_PAGE_SIZE

    companion object {
        private const val TRANSACTIONS_PAGE_SIZE = 20L
    }

    fun stopWalletsListener() {
        walletsJob?.cancel()
        walletsJob = null
        dueJob?.cancel()
        dueJob = null
        dueCardsKey = null
    }

    // Restarts only when the set of cards in their due window (or their days) changes, not on every balance update
    private fun watchDueCards(wallets: List<WalletModel>) {
        val cards = wallets.filter { CardStatementHelper.isInDueWindow(it) }
        val key = cards.map { listOf(it.docId, it.statementDay, it.paymentDueDays) } + LocalDate.now()
        if (key == dueCardsKey && dueJob?.isActive == true) return
        dueCardsKey = key
        dueJob?.cancel()
        dueJob = viewModelScope.launch {
            walletRepository.getChargesSinceClose(cards)
                .catch { e -> Log.e("WalletViewModel", "due cards: ${e.message}") } // no badge is better than an error for a warning
                .collect { charges -> _stateFlow.update { it.copy(dueCardCharges = charges) } }
        }
    }

    fun stopTransactionsListener() {
        transactionsJob?.cancel()
        transactionsJob = null
    }

    fun stopMovementsListener() {
        movementsJob?.cancel()
        movementsJob = null
    }

    fun stopStatementListener() {
        statementJob?.cancel()
        statementJob = null
    }

    fun stopRecurringListener() {
        recurringJob?.cancel()
        recurringJob = null
    }

    fun onEvent(event: WalletScreenEvents) {
        when (event) {
            WalletScreenEvents.OnGetWallets -> {
                if (walletsJob?.isActive == true) return // already live (the tab keeps its listener)
                walletsJob = viewModelScope.launch {
                    walletRepository.getWallets().asResult().onEach { res ->
                        when (res) {
                            ResultState.Completed -> _stateFlow.update { it.copy(isListLoading = false) }
                            is ResultState.Error -> onError(res.exception)
                            ResultState.Loading -> _stateFlow.update { it.copy(isListLoading = true) }
                            is ResultState.Success -> {
                                _stateFlow.update { it.copy(wallets = res.data, hasLoadedWallets = true, isListLoading = false) }
                                watchDueCards(res.data)
                            }
                        }
                    }.collect()
                }
            }

            is WalletScreenEvents.OnNavigateToWalletDetails -> {
                sendEvent(OnNavigate(WalletScreens.WalletDetailsNavigation(event.walletId)))
            }

            is WalletScreenEvents.OnOpenWalletEditor -> _stateFlow.update {
                it.copy(
                    walletDraft = event.wallet ?: WalletModel(iconKey = "wallet", colorIndex = it.wallets.size % WalletStyle.colors.size),
                    walletDraftOriginalBalance = event.wallet?.balance ?: 0L
                )
            }
            WalletScreenEvents.OnCloseWalletEditor -> _stateFlow.update { it.copy(walletDraft = null) }
            is WalletScreenEvents.OnWalletDraftChanged -> _stateFlow.update { it.copy(walletDraft = event.draft) }

            WalletScreenEvents.OnSaveWalletDraft -> {
                val current = _stateFlow.value
                val draft = current.walletDraft ?: return
                if (current.isLoading) return // ignore double taps while saving
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        walletRepository.saveWallet(
                            draft.toRequest().copy(
                                originalBalance = current.walletDraftOriginalBalance,
                                correctionTitle = context.getString(R.string.wallet_movement_preset_correction)
                            )
                        )
                        _stateFlow.update { it.copy(isLoading = false, walletDraft = null) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.wallet_snackbar_saved))))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            // The details screen pops itself once the wallet is gone from the list and isLoading is false.
            // The snackbar goes straight to the global SnackBarController first, because this VM is cleared right after that pop.
            is WalletScreenEvents.OnCloseWallet -> {
                if (_stateFlow.value.isLoading || event.wallet.isPhysical) return
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        walletRepository.closeWallet(event.wallet.docId)
                        SnackBarController.sendEvent(SnackbarEvent(message = context.getString(R.string.wallet_snackbar_closed, event.wallet.name)))
                        _stateFlow.update { it.copy(isLoading = false, walletDraft = null) }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is WalletScreenEvents.OnGetWalletTransactions -> {
                if (transactionsJob?.isActive == true) return
                transactionsJob = viewModelScope.launch {
                    collectWalletTransactions(event.walletId, isLoadMore = false)
                }
            }

            is WalletScreenEvents.OnGetWalletMovements -> {
                if (movementsJob?.isActive == true) return
                movementsJob = viewModelScope.launch {
                    collectWalletMovements(event.walletId, isLoadMore = false)
                }
            }

            // One "Load more" for the merged list: each list that still has more grows by a page
            is WalletScreenEvents.OnLoadMoreWalletTransactions -> {
                val current = _stateFlow.value
                val isBusy = current.isLoadingMoreTransactions || current.isTransactionsLoading ||
                    current.isLoadingMoreMovements || current.isMovementsLoading
                if (isBusy) return

                if (current.hasMoreTransactions) {
                    transactionsLimit += TRANSACTIONS_PAGE_SIZE
                    stopTransactionsListener()
                    transactionsJob = viewModelScope.launch {
                        _stateFlow.update { it.copy(isLoadingMoreTransactions = true) }
                        collectWalletTransactions(event.walletId, isLoadMore = true)
                    }
                }
                if (current.hasMoreMovements) {
                    movementsLimit += TRANSACTIONS_PAGE_SIZE
                    stopMovementsListener()
                    movementsJob = viewModelScope.launch {
                        _stateFlow.update { it.copy(isLoadingMoreMovements = true) }
                        collectWalletMovements(event.walletId, isLoadMore = true)
                    }
                }
            }

            // Restarts only when the range changes (another month picked, or the statement day was edited)
            is WalletScreenEvents.OnGetStatement -> {
                if (statementJob?.isActive == true && statementSince == event.since) return
                stopStatementListener()
                statementSince = event.since
                statementJob = viewModelScope.launch {
                    val since = event.since
                    walletRepository.getWalletActivitySince(event.walletId, CardStatementHelper.startOfDay(event.since)).asResult()
                        .onEach { res ->
                            when (res) {
                                ResultState.Completed -> _stateFlow.update { it.copy(isStatementLoading = false) }
                                is ResultState.Error -> {
                                    _stateFlow.update { it.copy(isStatementLoading = false, hasStatementError = true) }
                                    onError(res.exception)
                                }
                                ResultState.Loading -> _stateFlow.update { it.copy(isStatementLoading = true, hasStatementError = false) }
                                is ResultState.Success -> _stateFlow.update {
                                    it.copy(
                                        statementTransactions = res.data.first,
                                        statementMovements = res.data.second,
                                        hasLoadedStatement = true,
                                        statementLoadedSince = since,
                                        isStatementLoading = false,
                                        hasStatementError = false
                                    )
                                }
                            }
                        }.collect()
                }
            }

            is WalletScreenEvents.OnNavigateToStatement -> {
                sendEvent(OnNavigate(WalletScreens.WalletStatementNavigation(event.walletId, event.monthsBack)))
            }

            // One listener for every card's subscriptions and installments (a handful of docs). The Wallet tab keeps it running.
            WalletScreenEvents.OnGetRecurringCharges -> {
                if (recurringJob?.isActive == true) return
                recurringJob = viewModelScope.launch {
                    walletRepository.getRecurringCharges().asResult().onEach { res ->
                        when (res) {
                            ResultState.Completed -> Unit
                            is ResultState.Error -> onError(res.exception)
                            ResultState.Loading -> Unit
                            is ResultState.Success -> _stateFlow.update { it.copy(recurringCharges = res.data, hasLoadedRecurring = true) }
                        }
                    }.collect()
                }
            }

            is WalletScreenEvents.OnNavigateToRecurring -> {
                sendEvent(OnNavigate(WalletScreens.WalletRecurringNavigation(event.walletId)))
            }

            // A new subscription starts today, monthly. A new installment starts at 12 months; its first date is set on save.
            is WalletScreenEvents.OnOpenRecurringSheet -> _stateFlow.update {
                it.copy(
                    recurringDraft = event.charge ?: RecurringChargeModel(
                        walletId = event.walletId,
                        kind = event.kind,
                        startDate = CardStatementHelper.startOfDay(LocalDate.now()),
                        totalCount = if (event.kind == RecurringChargeModel.KIND_INSTALLMENT) 12 else 0,
                    )
                )
            }
            WalletScreenEvents.OnCloseRecurringSheet -> _stateFlow.update { it.copy(recurringDraft = null) }
            is WalletScreenEvents.OnRecurringDraftChanged -> _stateFlow.update { it.copy(recurringDraft = event.draft) }

            is WalletScreenEvents.OnSaveRecurring -> {
                val current = _stateFlow.value
                val draft = current.recurringDraft ?: return
                if (current.isLoading) return // ignore double taps while saving
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        walletRepository.saveRecurringCharge(draft, event.card.statementDay)
                        _stateFlow.update { it.copy(isLoading = false, recurringDraft = null) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.wallet_snackbar_recurring_saved, draft.name.trim()))))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is WalletScreenEvents.OnDeleteRecurring -> {
                if (_stateFlow.value.isLoading) return
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        walletRepository.deleteRecurringCharge(event.charge.docId)
                        _stateFlow.update { it.copy(isLoading = false, recurringDraft = null) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.wallet_snackbar_recurring_deleted, event.charge.name))))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            // A transfer starts with the "Transfer" title filled in, since it rarely needs another one. "Pay card" starts as "Card payment" the same way.
            is WalletScreenEvents.OnOpenMovementSheet -> _stateFlow.update {
                it.copy(
                    movementDraft = when {
                        event.type == MovementType.TRANSFER -> MovementDraft(
                            type = event.type,
                            title = context.getString(R.string.wallet_movement_preset_transfer),
                            presetKey = WalletMovementModel.PRESET_TRANSFER
                        )
                        event.type == MovementType.IN && event.isCreditCard -> MovementDraft(
                            type = event.type,
                            title = context.getString(R.string.wallet_movement_preset_card_payment),
                            presetKey = WalletMovementPresets.PRESET_CARD_PAYMENT
                        )
                        else -> MovementDraft(type = event.type)
                    }
                )
            }
            WalletScreenEvents.OnCloseMovementSheet -> _stateFlow.update { it.copy(movementDraft = null) }
            is WalletScreenEvents.OnMovementDraftChanged -> _stateFlow.update { it.copy(movementDraft = event.draft) }

            is WalletScreenEvents.OnSaveMovement -> {
                val current = _stateFlow.value
                val draft = current.movementDraft ?: return
                if (current.isLoading) return // ignore double taps while saving
                _stateFlow.update { it.copy(isLoading = true) }
                // "Pay card" with a "Paid from" wallet is saved as a transfer from that wallet into the card, so both balances move
                val isPaidFromWallet = event.wallet.isCreditCard && draft.type == MovementType.IN && draft.fromWalletId.isNotEmpty() &&
                    !WalletMovementPresets.isFromBank(draft.presetKey)
                viewModelScope.launch {
                    try {
                        walletRepository.addMovement(
                            if (isPaidFromWallet) {
                                WalletMovementRequest(
                                    walletId = draft.fromWalletId,
                                    amount = draft.amount,
                                    title = draft.title,
                                    presetKey = draft.presetKey,
                                    note = draft.note,
                                    toWalletId = event.wallet.docId
                                )
                            } else {
                                WalletMovementRequest(
                                    walletId = event.wallet.docId,
                                    amount = if (draft.type == MovementType.OUT) -draft.amount else draft.amount,
                                    title = draft.title,
                                    presetKey = draft.presetKey,
                                    note = draft.note,
                                    toWalletId = if (draft.type == MovementType.TRANSFER) draft.toWalletId else ""
                                )
                            }
                        )
                        _stateFlow.update { it.copy(isLoading = false, movementDraft = null) }
                        // "Moved to BDO" for a transfer, "Saved to Cash" otherwise
                        val message = if (draft.type == MovementType.TRANSFER) {
                            context.getString(R.string.wallet_snackbar_transfer_saved, current.wallets.find { it.docId == draft.toWalletId }?.name.orEmpty())
                        } else {
                            context.getString(R.string.wallet_snackbar_movement_saved, event.wallet.name)
                        }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = message)))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is WalletScreenEvents.OnDeleteMovement -> {
                if (_stateFlow.value.isLoading) return
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        walletRepository.deleteMovement(event.movement)
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(OneTimeEvents.ShowSnackbar(SnackbarEvent(message = context.getString(R.string.wallet_snackbar_movement_deleted))))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is WalletScreenEvents.OnPhysicalDraftChanged -> _stateFlow.update { it.copy(physicalDraft = event.draft) }

            // Saved before step 2, so the physical wallet exists even if the app closes on the "add more" step
            WalletScreenEvents.OnSavePhysicalWallet -> {
                if (_stateFlow.value.isLoading) return
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        walletRepository.savePhysicalWallet(_stateFlow.value.physicalDraft.toRequest())
                        _stateFlow.update { it.copy(isLoading = false, setupStep = 1) }
                        onEvent(WalletScreenEvents.OnGetWallets)
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            // Step 2 can edit the physical wallet in the sheet, so step 1 reloads it from the live list instead of keeping a stale draft
            WalletScreenEvents.OnSetupStepBack -> _stateFlow.update { state ->
                state.copy(setupStep = 0, physicalDraft = state.wallets.firstOrNull { it.isPhysical } ?: state.physicalDraft)
            }

            WalletScreenEvents.OnSignOut -> logout()
        }
    }

    private suspend fun collectWalletTransactions(walletId: String, isLoadMore: Boolean) {
        walletRepository.getWalletTransactions(walletId, transactionsLimit).asResult()
            .onEach { res ->
                when (res) {
                    ResultState.Completed -> _stateFlow.update { it.copy(isTransactionsLoading = false, isLoadingMoreTransactions = false) }
                    is ResultState.Error -> {
                        _stateFlow.update { it.copy(isTransactionsLoading = false, isLoadingMoreTransactions = false, hasTransactionsError = true) }
                        onError(res.exception)
                    }
                    ResultState.Loading -> if (!isLoadMore) _stateFlow.update { it.copy(isTransactionsLoading = true, hasTransactionsError = false) }
                    is ResultState.Success -> _stateFlow.update {
                        it.copy(
                            walletTransactions = res.data,
                            hasMoreTransactions = res.data.size.toLong() == transactionsLimit,
                            hasTransactionsError = false,
                            isTransactionsLoading = false,
                            isLoadingMoreTransactions = false
                        )
                    }
                }
            }.collect()
    }

    private suspend fun collectWalletMovements(walletId: String, isLoadMore: Boolean) {
        walletRepository.getWalletMovements(walletId, movementsLimit).asResult()
            .onEach { res ->
                when (res) {
                    ResultState.Completed -> _stateFlow.update { it.copy(isMovementsLoading = false, isLoadingMoreMovements = false) }
                    is ResultState.Error -> {
                        _stateFlow.update { it.copy(isMovementsLoading = false, isLoadingMoreMovements = false, hasMovementsError = true) }
                        onError(res.exception)
                    }
                    ResultState.Loading -> if (!isLoadMore) _stateFlow.update { it.copy(isMovementsLoading = true, hasMovementsError = false) }
                    is ResultState.Success -> _stateFlow.update {
                        it.copy(
                            walletMovements = res.data,
                            hasMoreMovements = res.data.size.toLong() == movementsLimit,
                            hasMovementsError = false,
                            isMovementsLoading = false,
                            isLoadingMoreMovements = false
                        )
                    }
                }
            }.collect()
    }

    private fun WalletModel.toRequest() = WalletRequest(
        docId = docId,
        name = name,
        colorIndex = colorIndex,
        iconKey = iconKey,
        balance = balance,
        type = type,
        creditLimit = if (isCreditCard) creditLimit else 0L,
        statementDay = if (isCreditCard) statementDay else 0,
        paymentDueDays = if (isCreditCard) paymentDueDays else 0,
    )

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
