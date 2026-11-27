package com.gmail.vondenuelle.denuspend.ui.wallet.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.MovementType
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenEvents
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenState
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletViewModel
import com.gmail.vondenuelle.denuspend.ui.wallet.components.CardStatementSummaries
import com.gmail.vondenuelle.denuspend.ui.wallet.components.CloseWalletDialog
import com.gmail.vondenuelle.denuspend.ui.wallet.components.DeleteMovementDialog
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletCard
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletEditorSheet
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletMovementItem
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletMovementSheet
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.RecurringChargeHelper
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import com.gmail.vondenuelle.denuspend.utils.formatFirebaseDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Date

@Composable
fun WalletDetailsScreen(
    walletId: String,
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val wallet = state.wallets.find { it.docId == walletId }

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

    // A card's statements listen from the open cycle's first day. It's only known once the wallet has loaded,
    // so it starts from here when it changes, and again on ON_START after the screen was stopped.
    val statementSince = wallet?.takeIf { it.isCreditCard && it.statementDay in 1..31 }
        ?.let { CardStatementHelper.cycle(it.statementDay, 0).start }
    val currentStatementSince by rememberUpdatedState(statementSince)
    LaunchedEffect(statementSince) {
        statementSince?.let { viewModel.onEvent(WalletScreenEvents.OnGetStatement(walletId, it)) }
    }

    // Pushed screen, so all the listeners stop on ON_STOP
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(WalletScreenEvents.OnGetWallets)
                viewModel.onEvent(WalletScreenEvents.OnGetWalletTransactions(walletId))
                viewModel.onEvent(WalletScreenEvents.OnGetWalletMovements(walletId))
                viewModel.onEvent(WalletScreenEvents.OnGetRecurringCharges)
                currentStatementSince?.let { viewModel.onEvent(WalletScreenEvents.OnGetStatement(walletId, it)) }
            }
            Lifecycle.Event.ON_STOP -> {
                viewModel.stopWalletsListener()
                viewModel.stopTransactionsListener()
                viewModel.stopMovementsListener()
                viewModel.stopStatementListener()
                viewModel.stopRecurringListener()
            }
            else -> Unit
        }
    }

    // The wallet was closed (here or on another device): leave once, after the list has loaded at least once.
    // Waits while a close started here is saving, so the VM can send its snackbar before this screen (and the VM) goes away.
    var hasLeft by remember { mutableStateOf(false) }
    LaunchedEffect(state.hasLoadedWallets, wallet == null, state.isLoading) {
        if (state.hasLoadedWallets && wallet == null && !state.isLoading && !hasLeft) {
            hasLeft = true
            onPopBackStack()
        }
    }

    LoadingDialog(showDialog = state.isLoading, text = stringResource(R.string.lbl_loading)) { }

    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    // Closing a card stops its subscriptions and installments, so both close confirms say how many
    val activeRecurringCount = state.recurringCharges.count { it.walletId == walletId && it.active }

    WalletEditorSheet(
        draft = state.walletDraft,
        onDraftChange = { viewModel.onEvent(WalletScreenEvents.OnWalletDraftChanged(it)) },
        onSave = { viewModel.onEvent(WalletScreenEvents.OnSaveWalletDraft) },
        onDismiss = { viewModel.onEvent(WalletScreenEvents.OnCloseWalletEditor) },
        onCloseWallet = if (wallet != null && !wallet.isPhysical) {
            { viewModel.onEvent(WalletScreenEvents.OnCloseWallet(wallet)) }
        } else null,
        recurringCount = activeRecurringCount,
    )

    var showCloseDialog by rememberSaveable { mutableStateOf(false) }
    CloseWalletDialog(
        showDialog = showCloseDialog && wallet != null,
        walletName = wallet?.name.orEmpty(),
        recurringCount = activeRecurringCount,
        isCreditCard = wallet?.isCreditCard == true,
        owed = wallet?.owed ?: 0L,
        onDismiss = { showCloseDialog = false },
        onConfirm = {
            showCloseDialog = false
            wallet?.let { viewModel.onEvent(WalletScreenEvents.OnCloseWallet(it)) }
        },
    )

    WalletMovementSheet(
        draft = state.movementDraft,
        wallet = wallet,
        wallets = state.wallets,
        hasLoadedWallets = state.hasLoadedWallets,
        onDraftChange = { viewModel.onEvent(WalletScreenEvents.OnMovementDraftChanged(it)) },
        onSave = { wallet?.let { viewModel.onEvent(WalletScreenEvents.OnSaveMovement(it)) } },
        onDismiss = { viewModel.onEvent(WalletScreenEvents.OnCloseMovementSheet) },
    )

    // Only the id is saved, so the dialog shows the live copy and closes itself if the entry is deleted elsewhere
    var deletingMovementId by rememberSaveable { mutableStateOf<String?>(null) }
    val deletingMovement = state.walletMovements.find { it.docId == deletingMovementId }
    DeleteMovementDialog(
        movement = deletingMovement,
        walletName = wallet?.name.orEmpty(),
        otherWalletName = deletingMovement?.let { m -> state.wallets.find { it.docId == m.otherWalletId }?.name },
        isCreditCard = wallet?.isCreditCard == true,
        isOtherCreditCard = deletingMovement?.let { m -> state.wallets.find { it.docId == m.otherWalletId }?.isCreditCard } == true,
        onDismiss = { deletingMovementId = null },
        onConfirm = {
            deletingMovementId = null
            deletingMovement?.let { viewModel.onEvent(WalletScreenEvents.OnDeleteMovement(it)) }
        },
    )

    WalletDetailsScreenContent(
        wallet = wallet,
        state = state,
        onEvent = viewModel::onEvent,
        onCloseWalletClick = { showCloseDialog = true },
        onMovementClick = { deletingMovementId = it.docId },
        modifier = Modifier.fillMaxSize()
    )
}

// One row of the merged list: an expense paid from this wallet, or a money in/out entry
private sealed interface WalletEntry {
    val date: Date
    val key: String

    data class Expense(val transaction: TransactionModel) : WalletEntry {
        override val date: Date get() = transaction.date.toDate()
        override val key get() = "t_${transaction.docId}"
    }

    data class Movement(val movement: WalletMovementModel) : WalletEntry {
        override val date: Date get() = movement.date.toDate()
        override val key get() = "m_${movement.docId}"
    }
}

// Both lists are paged separately, newest first. A list that still has more pages is cut at its oldest loaded item,
// so older entries of the other list don't show before the missing ones. "Load more" fills the gap.
private fun mergeEntries(state: WalletScreenState): List<WalletEntry> {
    val all = (state.walletTransactions.map { WalletEntry.Expense(it) } + state.walletMovements.map { WalletEntry.Movement(it) })
        .sortedByDescending { it.date }
    val cutoff = listOfNotNull(
        state.walletTransactions.lastOrNull()?.date?.toDate()?.takeIf { state.hasMoreTransactions },
        state.walletMovements.lastOrNull()?.date?.toDate()?.takeIf { state.hasMoreMovements },
    ).maxOrNull() ?: return all
    return all.filter { it.date >= cutoff }
}

@Composable
fun WalletDetailsScreenContent(
    modifier: Modifier = Modifier,
    wallet: WalletModel?,
    state: WalletScreenState,
    onEvent: (WalletScreenEvents) -> Unit,
    onCloseWalletClick: () -> Unit,
    onMovementClick: (WalletMovementModel) -> Unit,
) {
    if (wallet == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // A failed listener can leave old cached rows (deleted ones, missing new ones), so the list is hidden behind the error panel instead
    val hasHistoryError = state.hasTransactionsError || state.hasMovementsError
    val entries = remember(state.walletTransactions, state.walletMovements, state.hasMoreTransactions, state.hasMoreMovements, hasHistoryError) {
        if (hasHistoryError) emptyList() else mergeEntries(state)
    }
    val isHistoryLoading = state.isTransactionsLoading || state.isMovementsLoading
    val isLoadingMore = state.isLoadingMoreTransactions || state.isLoadingMoreMovements

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            WalletCard(
                wallet = wallet,
                installmentsToCome = RecurringChargeHelper.installmentsToCome(state.recurringCharges, wallet.docId),
                due = CardStatementHelper.dueStatus(wallet, state.dueCardCharges)
            )
            // Money that isn't an expense. A transfer needs a second wallet to move to.
            // A card has "Pay card" (its "Paid from" covers what a transfer did) and "Other charge" (fees) instead.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                if (wallet.isCreditCard) {
                    MoneyActionButton(
                        icon = Icons.Filled.Payments,
                        label = stringResource(R.string.wallet_btn_pay_card),
                        onClick = { onEvent(WalletScreenEvents.OnOpenMovementSheet(MovementType.IN, isCreditCard = true)) },
                        modifier = Modifier.weight(1f)
                    )
                    MoneyActionButton(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        label = stringResource(R.string.wallet_btn_other_charge),
                        onClick = { onEvent(WalletScreenEvents.OnOpenMovementSheet(MovementType.OUT, isCreditCard = true)) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MoneyActionButton(
                        icon = Icons.Filled.Add,
                        label = stringResource(R.string.wallet_btn_add_money),
                        onClick = { onEvent(WalletScreenEvents.OnOpenMovementSheet(MovementType.IN)) },
                        modifier = Modifier.weight(1f)
                    )
                    MoneyActionButton(
                        icon = Icons.Filled.Remove,
                        label = stringResource(R.string.wallet_btn_take_out),
                        onClick = { onEvent(WalletScreenEvents.OnOpenMovementSheet(MovementType.OUT)) },
                        modifier = Modifier.weight(1f)
                    )
                    MoneyActionButton(
                        icon = Icons.Filled.SwapHoriz,
                        label = stringResource(R.string.wallet_btn_transfer),
                        enabled = state.wallets.count { !it.isCreditCard } > 1, // cards can't be transferred to
                        onClick = { onEvent(WalletScreenEvents.OnOpenMovementSheet(MovementType.TRANSFER)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // The physical wallet can't be closed, so Edit takes the full width there
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                FilledTonalButton(
                    onClick = { onEvent(WalletScreenEvents.OnOpenWalletEditor(wallet)) },
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(stringResource(R.string.wallet_btn_edit), maxLines = 1, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
                if (!wallet.isPhysical) {
                    OutlinedButton(
                        onClick = onCloseWalletClick,
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Text(
                            stringResource(R.string.wallet_btn_close_wallet),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                        )
                    }
                }
            }

            if (wallet.isCreditCard && wallet.statementDay in 1..31) {
                CardStatementSummaries(
                    card = wallet,
                    transactions = state.statementTransactions,
                    movements = state.statementMovements,
                    hasLoaded = state.hasLoadedStatement,
                    hasError = state.hasStatementError,
                    onOpenStatement = { onEvent(WalletScreenEvents.OnNavigateToStatement(wallet.docId, it)) },
                    installments = state.recurringCharges.filter { it.walletId == wallet.docId && it.isInstallment && it.active },
                    onOpenRecurring = { onEvent(WalletScreenEvents.OnNavigateToRecurring(wallet.docId)) },
                    modifier = Modifier.padding(top = 24.dp)
                )
                RecurringChargesRow(
                    charges = state.recurringCharges.filter { it.walletId == wallet.docId },
                    statementDay = wallet.statementDay,
                    onClick = { onEvent(WalletScreenEvents.OnNavigateToRecurring(wallet.docId)) },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.wallet_lbl_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.wallet_lbl_history_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
        }

        if (isHistoryLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (entries.isEmpty()) {
            item {
                if (hasHistoryError) {
                    WalletExpensesPlaceholder(
                        icon = Icons.Outlined.ErrorOutline,
                        title = stringResource(R.string.wallet_empty_error_title),
                        text = stringResource(R.string.wallet_lbl_transactions_error),
                        isError = true
                    )
                } else {
                    WalletExpensesPlaceholder(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = stringResource(R.string.wallet_empty_title),
                        text = stringResource(if (wallet.isCreditCard) R.string.wallet_empty_text_card else R.string.wallet_empty_text, wallet.name)
                    )
                }
            }
        }

        // Expenses are display only: edit/delete still happen from the Add tab or History, which know the transaction's day.
        // Money in/out entries open the delete confirm.
        // The list spans many days but the rows only show the time, so each new day gets a small date label
        itemsIndexed(entries, key = { _, it -> it.key }) { index, entry ->
            val day = formatFirebaseDate(entry.date)
            if (index == 0 || formatFirebaseDate(entries[index - 1].date) != day) {
                Text(
                    day,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }
            when (entry) {
                is WalletEntry.Expense -> TransactionItem(transactionModel = entry.transaction)
                is WalletEntry.Movement -> WalletMovementItem(
                    movement = entry.movement,
                    otherWalletName = state.wallets.find { it.docId == entry.movement.otherWalletId }?.name,
                    onClick = { onMovementClick(entry.movement) }
                )
            }
        }

        if (!hasHistoryError && (state.hasMoreTransactions || state.hasMoreMovements || isLoadingMore)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else {
                        TextButton(onClick = { onEvent(WalletScreenEvents.OnLoadMoreWalletTransactions(wallet.docId)) }) {
                            Text(stringResource(R.string.wallet_btn_load_more))
                        }
                    }
                }
            }
        }
    }
}

/** "Subscriptions & installments · 3 active · next Nov 28" on a card, opening their own screen. */
@Composable
private fun RecurringChargesRow(
    modifier: Modifier = Modifier,
    charges: List<RecurringChargeModel>,
    statementDay: Int,
    onClick: () -> Unit,
) {
    val active = charges.filter { it.active }
    val subtitle = when {
        charges.isEmpty() -> stringResource(R.string.wallet_lbl_recurring_row_empty)
        active.isEmpty() -> stringResource(R.string.wallet_lbl_recurring_none_active)
        else -> stringResource(
            R.string.wallet_lbl_recurring_row_count,
            active.size,
            // LocalDate is Comparable<ChronoLocalDate>, so the earliest is picked by its day number
            active.map { RecurringChargeHelper.nextDate(it, statementDay) }.minBy { it.toEpochDay() }
                .let { CardStatementHelper.formatDay(it, withYear = it.year != LocalDate.now().year) }
        )
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.wallet_lbl_recurring_row), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

/** Add money / Take out / Transfer: icon over a short label, so three fit in one row. */
@Composable
private fun MoneyActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.alpha(if (enabled) 1f else 0.4f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Empty or failed list: a soft panel with an icon badge, so the area under the header doesn't look blank. */
@Composable
private fun WalletExpensesPlaceholder(
    icon: ImageVector,
    title: String,
    text: String,
    isError: Boolean = false,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    )
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview
@Composable
private fun WalletDetailsScreenEmptyPreview() {
    DenuSpendTheme {
        Surface {
            WalletDetailsScreenContent(
                wallet = WalletModel(docId = "a", name = "GCash", colorIndex = 1, iconKey = "ewallet", balance = 0),
                state = WalletScreenState(),
                onEvent = {},
                onCloseWalletClick = {},
                onMovementClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun WalletDetailsScreenPreview() {
    DenuSpendTheme {
        Surface {
            WalletDetailsScreenContent(
                wallet = WalletModel(docId = "a", name = "GCash", colorIndex = 1, iconKey = "ewallet", balance = 120000),
                state = WalletScreenState(
                    walletTransactions = listOf(TransactionModel(docId = "1", title = "Lunch", category = "Food", amount = -15000))
                ),
                onEvent = {},
                onCloseWalletClick = {},
                onMovementClick = {},
            )
        }
    }
}
