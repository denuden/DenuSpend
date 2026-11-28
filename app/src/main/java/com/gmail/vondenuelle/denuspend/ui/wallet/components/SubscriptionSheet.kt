package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CurrencyTextField
import com.gmail.vondenuelle.denuspend.ui.common.dialog.DatePickerDialogModal
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import com.gmail.vondenuelle.denuspend.utils.RecurringChargeHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Add / edit a subscription on a card. It opens from the card, so there's no "Charge to", and no category: any name works.
 * A start date in the past is fine (it sets the day of the month), but nothing before today is charged.
 * Once saved, only the name can be edited; anything else is delete (with a confirm) and add again.
 */
@Composable
fun SubscriptionSheet(
    draft: RecurringChargeModel?, // null (or an installment) = closed
    card: WalletModel?,
    onDraftChange: (RecurringChargeModel) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheetDialog(
        showDialog = draft != null && !draft.isInstallment && card != null,
        onDismissRequest = onDismiss,
        skipPartiallyExpanded = true,
    ) {
        if (draft == null || card == null) return@ModalBottomSheetDialog
        SubscriptionSheetContent(draft = draft, card = card, onDraftChange = onDraftChange, onSave = onSave, onDelete = onDelete)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionSheetContent(
    draft: RecurringChargeModel,
    card: WalletModel,
    onDraftChange: (RecurringChargeModel) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val isNew = draft.docId.isEmpty()
    val today = LocalDate.now()
    val start = CardStatementHelper.toLocalDate(draft.startDate)
    // A new one skips the dates before the open statement (so a date earlier this cycle shows, and is charged on Save). A saved one keeps its own count (its schedule can't be edited).
    val next = if (isNew) RecurringChargeHelper.dateAt(draft, RecurringChargeHelper.datesBefore(draft, today, card.statementDay), card.statementDay)
    else RecurringChargeHelper.nextDate(draft, card.statementDay)
    val canSave = draft.name.isNotBlank() && (!isNew || draft.amount > 0)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    DatePickerDialogModal(
        initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        showDialog = showDatePicker,
        // The picker gives UTC midnight of the picked day; it's stored as the start of that day on this phone
        onDateSelected = { millis ->
            millis?.let {
                val picked = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                onDraftChange(draft.copy(startDate = CardStatementHelper.startOfDay(picked)))
            }
        },
        onDismiss = { showDatePicker = false },
    )

    // Only the fields scroll; Save stays pinned under them
    Column(modifier = Modifier.imePadding()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                if (isNew) stringResource(R.string.wallet_title_new_subscription, card.name) else stringResource(R.string.wallet_title_edit_subscription),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            // Once saved only the name can change, so a charge is never posted twice or skipped by an edit
            if (isNew) CurrencyTextField(
                amountInCents = draft.amount,
                onAmountChange = { onDraftChange(draft.copy(amount = it)) },
                prefixText = "-₱",
                inlinePrefix = true,
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) else Text(
                "-${formatBalance(draft.amount)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Text(
                stringResource(R.string.wallet_lbl_charged_even_over_limit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            SheetFieldLabel(stringResource(R.string.wallet_lbl_recurring_name))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { if (it.length <= 30) onDraftChange(draft.copy(name = it)) },
                placeholder = { Text(stringResource(R.string.wallet_hint_subscription_name)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors().copy(unfocusedIndicatorColor = outlinedColor),
                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth()
            )

            if (isNew) {
                Spacer(modifier = Modifier.height(16.dp))
                SheetFieldLabel(stringResource(R.string.wallet_lbl_repeats_every))
                NumberStepper(value = draft.repeatEvery, onValueChange = { onDraftChange(draft.copy(repeatEvery = it)) }, range = 1..365)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RecurringChargeModel.UNIT_DAY to R.string.wallet_lbl_unit_day,
                        RecurringChargeModel.UNIT_WEEK to R.string.wallet_lbl_unit_week,
                        RecurringChargeModel.UNIT_MONTH to R.string.wallet_lbl_unit_month,
                        RecurringChargeModel.UNIT_YEAR to R.string.wallet_lbl_unit_year,
                    ).forEach { (unit, label) ->
                        FilterChip(
                            selected = draft.repeatUnit == unit,
                            onClick = { onDraftChange(draft.copy(repeatUnit = unit)) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
    
                Spacer(modifier = Modifier.height(16.dp))
                SheetFieldLabel(stringResource(R.string.wallet_lbl_started_on))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(CardStatementHelper.formatDay(start, withYear = true), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                }
            }

            // What the schedule works out to, so a past start date is clearly not back-charged
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (!isNew) AmountRow(stringResource(R.string.wallet_lbl_repeats_every), repeatText(draft))
                    AmountRow(stringResource(R.string.wallet_lbl_next_charge), CardStatementHelper.formatDay(next, withYear = true))
                    if (isNew && start.isBefore(today)) {
                        AmountRow(stringResource(R.string.wallet_lbl_before_today), stringResource(R.string.wallet_lbl_not_charged))
                    }
                }
            }

            if (!isNew) {
                Text(
                    stringResource(R.string.wallet_lbl_edit_name_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.wallet_btn_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
        ) {
            Text(stringResource(R.string.wallet_btn_save_subscription, formatBalance(draft.amount)))
        }
    }
}

@Composable
internal fun SheetFieldLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Preview
@Composable
private fun SubscriptionSheetPreview() {
    DenuSpendTheme {
        Surface {
            SubscriptionSheetContent(
                draft = RecurringChargeModel(name = "AI assistant", amount = 110000),
                card = WalletModel(name = "Visa card", type = WalletModel.TYPE_CREDIT_CARD, statementDay = 28),
                onDraftChange = {},
                onSave = {},
                onDelete = {},
            )
        }
    }
}
