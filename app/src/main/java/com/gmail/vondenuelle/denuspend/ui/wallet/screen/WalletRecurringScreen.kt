package com.gmail.vondenuelle.denuspend.ui.wallet.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenEvents
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenState
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletViewModel
import com.gmail.vondenuelle.denuspend.ui.wallet.components.DeleteRecurringDialog
import com.gmail.vondenuelle.denuspend.ui.wallet.components.InstallmentSheet
import com.gmail.vondenuelle.denuspend.ui.wallet.components.RecurringChargeItem
import com.gmail.vondenuelle.denuspend.ui.wallet.components.SubscriptionSheet
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletCard
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.RecurringChargeHelper
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch

/**
 * A credit card's subscriptions and installments, behind the row on card details. Add / edit open as bottom sheets.
 * Active ones first, then paused and paid-off ones.
 */
@Composable
fun WalletRecurringScreen(
    walletId: String,
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val card = state.wallets.find { it.docId == walletId }?.takeIf { it.isCreditCard && it.statementDay in 1..31 }

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> onPopBackStack()
            is OneTimeEvents.OnNavigate -> onNavigate(event.route, event.behavior.toNavOptions())
            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            is OneTimeEvents.ShowSnackbar -> {
                scope.launch {
                    SnackBarController.sendEvent(event.snackbarEvent)
                }
            }

            OneTimeEvents.OnCloseDialog -> {}
            OneTimeEvents.OnLoginOrRegisterCompleted -> {}
            is OneTimeEvents.ShowInputError -> {}
        }
    }

    // Pushed screen, so both listeners stop on ON_STOP
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(WalletScreenEvents.OnGetWallets)
                viewModel.onEvent(WalletScreenEvents.OnGetRecurringCharges)
            }
            Lifecycle.Event.ON_STOP -> {
                viewModel.stopWalletsListener()
                viewModel.stopRecurringListener()
            }
            else -> Unit
        }
    }

    // The card was closed elsewhere: leave once the list has loaded
    var hasLeft by remember { mutableStateOf(false) }
    LaunchedEffect(state.hasLoadedWallets, card == null) {
        if (state.hasLoadedWallets && card == null && !hasLeft) {
            hasLeft = true
            onPopBackStack()
        }
    }

    LoadingDialog(showDialog = state.isLoading, text = stringResource(R.string.lbl_loading)) { }
    ErrorDialog(text = error, showDialog = error.isNotEmpty()) { error = "" }

    // Only the id is saved, so the dialog closes itself if it's deleted on another device
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    DeleteRecurringDialog(
        charge = state.recurringCharges.find { it.docId == deletingId },
        onDismiss = { deletingId = null },
        onConfirm = {
            state.recurringCharges.find { it.docId == deletingId }?.let { viewModel.onEvent(WalletScreenEvents.OnDeleteRecurring(it)) }
            deletingId = null
        },
    )

    SubscriptionSheet(
        draft = state.recurringDraft,
        card = card,
        onDraftChange = { viewModel.onEvent(WalletScreenEvents.OnRecurringDraftChanged(it)) },
        onSave = { card?.let { viewModel.onEvent(WalletScreenEvents.OnSaveRecurring(it)) } },
        onDelete = { deletingId = state.recurringDraft?.docId },
        onDismiss = { viewModel.onEvent(WalletScreenEvents.OnCloseRecurringSheet) },
    )
    InstallmentSheet(
        draft = state.recurringDraft,
        card = card,
        onDraftChange = { viewModel.onEvent(WalletScreenEvents.OnRecurringDraftChanged(it)) },
        onSave = { card?.let { viewModel.onEvent(WalletScreenEvents.OnSaveRecurring(it)) } },
        onDelete = { deletingId = state.recurringDraft?.docId },
        onDismiss = { viewModel.onEvent(WalletScreenEvents.OnCloseRecurringSheet) },
    )

    WalletRecurringScreenContent(
        card = card,
        state = state,
        onEvent = viewModel::onEvent,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun WalletRecurringScreenContent(
    modifier: Modifier = Modifier,
    card: WalletModel?,
    state: WalletScreenState,
    onEvent: (WalletScreenEvents) -> Unit,
) {
    if (card == null || !state.hasLoadedRecurring) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val mine = state.recurringCharges.filter { it.walletId == card.docId }.sortedByDescending { it.active }
    val (installments, subscriptions) = mine.partition { it.isInstallment }
    val openSheet = { kind: String, charge: RecurringChargeModel? -> onEvent(WalletScreenEvents.OnOpenRecurringSheet(card.docId, kind, charge)) }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        item {
            WalletCard(wallet = card, installmentsToCome = RecurringChargeHelper.installmentsToCome(state.recurringCharges, card.docId))
        }
        recurringSection(
            title = R.string.wallet_lbl_subscriptions,
            emptyText = R.string.wallet_lbl_subscriptions_empty,
            charges = subscriptions,
            statementDay = card.statementDay,
            onAdd = { openSheet(RecurringChargeModel.KIND_SUBSCRIPTION, null) },
            onClick = { openSheet(RecurringChargeModel.KIND_SUBSCRIPTION, it) },
        )
        recurringSection(
            title = R.string.wallet_lbl_installments,
            emptyText = R.string.wallet_lbl_installments_empty,
            charges = installments,
            statementDay = card.statementDay,
            onAdd = { openSheet(RecurringChargeModel.KIND_INSTALLMENT, null) },
            onClick = { openSheet(RecurringChargeModel.KIND_INSTALLMENT, it) },
        )
    }
}

// "Subscriptions        + Add", then its rows (or a short hint when there are none), like "My wallets" on the Wallet tab
private fun LazyListScope.recurringSection(
    title: Int,
    emptyText: Int,
    charges: List<RecurringChargeModel>,
    statementDay: Int,
    onAdd: () -> Unit,
    onClick: (RecurringChargeModel) -> Unit,
) {
    item {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.wallet_btn_add), modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (charges.isEmpty()) {
            Text(
                stringResource(emptyText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(charges, key = { it.docId }) { charge ->
        RecurringChargeItem(charge = charge, statementDay = statementDay, onClick = { onClick(charge) })
    }
}

@Preview
@Composable
private fun WalletRecurringScreenPreview() {
    DenuSpendTheme {
        Surface {
            WalletRecurringScreenContent(
                card = WalletModel(docId = "c", name = "Visa card", type = WalletModel.TYPE_CREDIT_CARD, balance = -1234000, creditLimit = 5000000, statementDay = 28),
                state = WalletScreenState(
                    hasLoadedRecurring = true,
                    recurringCharges = listOf(
                        RecurringChargeModel(docId = "1", walletId = "c", name = "AI assistant", amount = 110000),
                        RecurringChargeModel(
                            docId = "2", walletId = "c", name = "Phone", kind = RecurringChargeModel.KIND_INSTALLMENT,
                            amount = 150000, totalCount = 24, paidBefore = 4, totalAmount = 3600000
                        ),
                    )
                ),
                onEvent = {},
            )
        }
    }
}
