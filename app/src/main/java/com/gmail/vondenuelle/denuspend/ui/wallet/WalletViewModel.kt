package com.gmail.vondenuelle.denuspend.ui.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.data.repositories.WalletRepository
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.navigation.WalletScreens
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.OnNavigate
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import com.gmail.vondenuelle.denuspend.utils.SnackbarEvent
import com.gmail.vondenuelle.denuspend.utils.network.ResultState
import com.gmail.vondenuelle.denuspend.utils.network.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    // Expenses paid from a wallet grow by one page per load-more; the listener restarts with the bigger limit
    private var transactionsLimit = TRANSACTIONS_PAGE_SIZE

    companion object {
        private const val TRANSACTIONS_PAGE_SIZE = 20L
    }

    fun stopWalletsListener() {
        walletsJob?.cancel()
        walletsJob = null
    }

    fun stopTransactionsListener() {
        transactionsJob?.cancel()
        transactionsJob = null
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
                            is ResultState.Success -> _stateFlow.update {
                                it.copy(wallets = res.data, hasLoadedWallets = true, isListLoading = false)
                            }
                        }
                    }.collect()
                }
            }

            is WalletScreenEvents.OnNavigateToWalletDetails -> {
                sendEvent(OnNavigate(WalletScreens.WalletDetailsNavigation(event.walletId)))
            }

            is WalletScreenEvents.OnOpenWalletEditor -> _stateFlow.update {
                it.copy(walletDraft = event.wallet ?: WalletModel(iconKey = "wallet", colorIndex = it.wallets.size % 10))
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
                        walletRepository.saveWallet(draft.toRequest())
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

            is WalletScreenEvents.OnLoadMoreWalletTransactions -> {
                val current = _stateFlow.value
                if (!current.hasMoreTransactions || current.isLoadingMoreTransactions || current.isTransactionsLoading) return

                transactionsLimit += TRANSACTIONS_PAGE_SIZE
                stopTransactionsListener()
                transactionsJob = viewModelScope.launch {
                    _stateFlow.update { it.copy(isLoadingMoreTransactions = true) }
                    collectWalletTransactions(event.walletId, isLoadMore = true)
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

    private fun WalletModel.toRequest() = WalletRequest(
        docId = docId,
        name = name,
        colorIndex = colorIndex,
        iconKey = iconKey,
        balance = balance
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
