package com.gmail.vondenuelle.denuspend.ui.wallet.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenEvents
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenState
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletViewModel
import com.gmail.vondenuelle.denuspend.ui.wallet.components.AmountRow
import com.gmail.vondenuelle.denuspend.ui.wallet.components.StatementRow
import com.gmail.vondenuelle.denuspend.ui.wallet.components.StatementSection
import com.gmail.vondenuelle.denuspend.ui.wallet.components.formatBalance
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * One credit card statement, laid out like the bank's statement: purchases, subscriptions, installments, other charges, then payments.
 * The arrows switch cycles (monthsBack 0 = the open one). It listens from the shown cycle's first day to today,
 * because a closed statement's balance is today's owed amount minus everything after it closed.
 */
@Composable
fun WalletStatementScreen(
    walletId: String,
    initialMonthsBack: Int,
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val wallet = state.wallets.find { it.docId == walletId }
    var monthsBack by rememberSaveable { mutableIntStateOf(initialMonthsBack) }

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

    val cycle = wallet?.takeIf { it.isCreditCard && it.statementDay in 1..31 }
        ?.let { CardStatementHelper.cycle(it.statementDay, monthsBack) }
    val since by rememberUpdatedState(cycle?.start)
    LaunchedEffect(cycle?.start) {
        cycle?.start?.let { viewModel.onEvent(WalletScreenEvents.OnGetStatement(walletId, it)) }
    }

    // Pushed screen, so both listeners stop on ON_STOP
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(WalletScreenEvents.OnGetWallets)
                since?.let { viewModel.onEvent(WalletScreenEvents.OnGetStatement(walletId, it)) }
            }
            Lifecycle.Event.ON_STOP -> {
                viewModel.stopWalletsListener()
                viewModel.stopStatementListener()
            }
            else -> Unit
        }
    }

    // The card was closed, or is no longer a card (edited elsewhere): leave once the list has loaded
    var hasLeft by remember { mutableStateOf(false) }
    LaunchedEffect(state.hasLoadedWallets, cycle == null) {
        if (state.hasLoadedWallets && cycle == null && !hasLeft) {
            hasLeft = true
            onPopBackStack()
        }
    }

    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    WalletStatementScreenContent(
        wallet = wallet,
        cycle = cycle,
        monthsBack = monthsBack,
        state = state,
        onMonthsBackChange = { monthsBack = it },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun WalletStatementScreenContent(
    modifier: Modifier = Modifier,
    wallet: WalletModel?,
    cycle: CardStatementHelper.Cycle?,
    monthsBack: Int,
    state: WalletScreenState,
    onMonthsBackChange: (Int) -> Unit,
) {
    if (wallet == null || cycle == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val trackedFrom = CardStatementHelper.toLocalDate(wallet.createdAt)
    // Cycles go back to the one tracking started in (its end is on or after the day the card was added). A bill that closed
    // before the card was added was never tracked: the amount owed entered belongs to the cycle it was added in.
    val canGoBack = !cycle.start.minusDays(1).isBefore(trackedFrom)
    val isOpen = monthsBack == 0

    val inCycle = { date: LocalDate -> cycle.contains(date) }
    val cycleTransactions = state.statementTransactions.filter { inCycle(CardStatementHelper.toLocalDate(it.date)) }
    val cycleMovements = state.statementMovements.filter { inCycle(CardStatementHelper.toLocalDate(it.date)) }
    val afterTotals = CardStatementHelper.totals(
        state.statementTransactions.filter { CardStatementHelper.toLocalDate(it.date).isAfter(cycle.end) },
        state.statementMovements.filter { CardStatementHelper.toLocalDate(it.date).isAfter(cycle.end) },
    )
    val cycleTotals = CardStatementHelper.totals(cycleTransactions, cycleMovements)
    // The cycle the card was added in carries the amount owed entered. The listener starts on this cycle's first day, so it holds
    // everything since the card was added.
    val owedWhenAdded = if (cycle.contains(trackedFrom)) {
        CardStatementHelper.owedWhenAdded(wallet, CardStatementHelper.totals(state.statementTransactions, state.statementMovements))
    } else 0L
    val sections = statementSections(cycleTransactions, cycleMovements, state.wallets)
    // The lists belong to the cycle on screen (a month switch restarts the listener, and the old rows must not show as the new month)
    val isReady = state.hasLoadedStatement && !state.isStatementLoading && state.statementLoadedSince == cycle.start

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MonthSwitcher(
                cycle = cycle,
                isOpen = isOpen,
                canGoBack = canGoBack,
                canGoForward = monthsBack > 0,
                onBack = { onMonthsBackChange(monthsBack + 1) },
                onForward = { onMonthsBackChange(monthsBack - 1) },
            )
        }

        if (isReady && !state.hasStatementError) item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (isOpen) {
                        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), cycle.end).toInt()
                        Text(stringResource(R.string.wallet_lbl_charges_so_far), style = MaterialTheme.typography.titleSmall)
                        Text(formatBalance(cycleTotals.charges + owedWhenAdded), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (daysLeft <= 0) stringResource(R.string.wallet_lbl_statement_closes_today)
                            else pluralStringResource(R.plurals.wallet_lbl_statement_closes_in, daysLeft, daysLeft),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val statementBalance = CardStatementHelper.statementBalance(wallet, afterTotals)
                        Text(stringResource(R.string.wallet_lbl_statement_balance), style = MaterialTheme.typography.titleSmall)
                        Text(formatBalance(statementBalance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        AmountRow(stringResource(R.string.wallet_lbl_charges_this_period), formatBalance(cycleTotals.charges + owedWhenAdded))
                        // "Paid since" only means something for the latest bill; older ones were paid by the bills after them
                        if (monthsBack == 1) {
                            val stillUnpaid = (statementBalance - afterTotals.payments).coerceAtLeast(0L)
                            AmountRow(
                                stringResource(R.string.wallet_lbl_paid_since, CardStatementHelper.formatDay(cycle.end)),
                                formatBalance(afterTotals.payments),
                                valueColor = if (afterTotals.payments > 0) colorResource(R.color.verified_green_text) else MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
                            AmountRow(
                                stringResource(R.string.wallet_lbl_still_unpaid),
                                formatBalance(stillUnpaid),
                                valueColor = if (stillUnpaid > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (owedWhenAdded > 0) {
                        Text(
                            stringResource(R.string.wallet_lbl_includes_owed_when_added, formatBalance(owedWhenAdded)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        if (trackedFrom.isAfter(cycle.start)) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(R.string.wallet_lbl_tracking_started, CardStatementHelper.formatDay(trackedFrom, withYear = true)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        when {
            state.hasStatementError -> item {
                Text(
                    stringResource(R.string.wallet_lbl_statement_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            !isReady -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            sections.isEmpty() -> item {
                Text(
                    stringResource(R.string.wallet_lbl_statement_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            }
            else -> items(sections, key = { it.title }) { section ->
                StatementSection(title = section.title, rows = section.rows, isCredit = section.isCredit)
            }
        }
    }
}

// "‹  Nov 28, 2026  ›" with "Oct 29 – Nov 28 · open" under it
@Composable
private fun MonthSwitcher(
    cycle: CardStatementHelper.Cycle,
    isOpen: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.wallet_cd_previous_statement))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    CardStatementHelper.formatDay(cycle.end, withYear = true),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val period = stringResource(R.string.wallet_lbl_statement_period, CardStatementHelper.formatDay(cycle.start), CardStatementHelper.formatDay(cycle.end))
                Text(
                    stringResource(if (isOpen) R.string.wallet_lbl_statement_state_open else R.string.wallet_lbl_statement_state_closed, period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.wallet_cd_next_statement))
            }
        }
    }
}

private data class SectionData(val title: String, val rows: List<StatementRow>, val isCredit: Boolean = false)

// Groups one cycle's entries like a bank statement. Empty groups are left out.
@Composable
private fun statementSections(
    transactions: List<TransactionModel>,
    movements: List<WalletMovementModel>,
    wallets: List<WalletModel>,
): List<SectionData> {
    val context = LocalContext.current
    val closedWallet = stringResource(R.string.wallet_lbl_closed_wallet)

    fun movementRow(m: WalletMovementModel): StatementRow {
        val other = wallets.find { it.docId == m.otherWalletId }?.name ?: closedWallet
        val transferText = when {
            !m.isTransfer -> ""
            m.amount > 0 -> context.getString(R.string.wallet_lbl_transfer_from_wallet, other)
            else -> context.getString(R.string.wallet_lbl_transfer_to_wallet, other)
        }
        return StatementRow(
            key = "m_${m.docId}",
            date = CardStatementHelper.toLocalDate(m.date),
            title = m.title,
            subtitle = listOf(transferText, m.note).filter { it.isNotBlank() }.joinToString(" · "),
            amount = abs(m.amount),
            isCredit = m.amount > 0
        )
    }

    val purchases = transactions.filter { it.amount < 0 }.map { t ->
        StatementRow(
            key = "t_${t.docId}",
            date = CardStatementHelper.toLocalDate(t.date),
            title = t.title,
            subtitle = listOf(t.category, t.description).filter { it.isNotBlank() && it != "---" }.joinToString(" · "),
            amount = -t.amount
        )
    }
    val outs = movements.filter { it.amount < 0 }
    val subscriptions = outs.filter { it.presetKey == WalletMovementModel.PRESET_SUBSCRIPTION }.map(::movementRow)
    val installments = outs.filter { it.presetKey == WalletMovementModel.PRESET_INSTALLMENT }.map(::movementRow)
    val otherCharges = outs.filter { it.presetKey != WalletMovementModel.PRESET_SUBSCRIPTION && it.presetKey != WalletMovementModel.PRESET_INSTALLMENT }.map(::movementRow)
    val payments = movements.filter { it.amount > 0 }.map(::movementRow)

    return listOf(
        SectionData(stringResource(R.string.wallet_lbl_section_purchases), purchases),
        SectionData(stringResource(R.string.wallet_lbl_section_subscriptions), subscriptions),
        SectionData(stringResource(R.string.wallet_lbl_section_installments), installments),
        SectionData(stringResource(R.string.wallet_lbl_section_other_charges), otherCharges),
        SectionData(stringResource(R.string.wallet_lbl_section_payments), payments, isCredit = true),
    ).filter { it.rows.isNotEmpty() }
}

@Preview
@Composable
private fun WalletStatementScreenPreview() {
    DenuSpendTheme {
        Surface {
            WalletStatementScreenContent(
                wallet = WalletModel(docId = "c", name = "Visa card", type = WalletModel.TYPE_CREDIT_CARD, balance = -1234000, statementDay = 28),
                cycle = CardStatementHelper.cycle(28, 0),
                monthsBack = 0,
                state = WalletScreenState(
                    hasLoadedStatement = true,
                    statementLoadedSince = CardStatementHelper.cycle(28, 0).start,
                    statementTransactions = listOf(TransactionModel(docId = "1", title = "Groceries", category = "Food", amount = -182000)),
                    statementMovements = listOf(WalletMovementModel(docId = "2", title = "Card payment", amount = 782000)),
                ),
                onMonthsBackChange = {},
            )
        }
    }
}
