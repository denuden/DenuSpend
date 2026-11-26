package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CurrencyTextField
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import com.gmail.vondenuelle.denuspend.utils.RecurringChargeHelper

/**
 * Add / edit an installment. The app does the math: the total (interest included), the months and how many are already paid
 * give the monthly amount, what's left and the last month. It's charged on the card's statement day.
 * Once saved, only the name can be edited; anything else is delete (with a confirm) and add again.
 */
@Composable
fun InstallmentSheet(
    draft: RecurringChargeModel?, // null (or a subscription) = closed
    card: WalletModel?,
    onDraftChange: (RecurringChargeModel) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheetDialog(
        showDialog = draft != null && draft.isInstallment && card != null,
        onDismissRequest = onDismiss,
        skipPartiallyExpanded = true,
    ) {
        if (draft == null || card == null) return@ModalBottomSheetDialog
        InstallmentSheetContent(draft = draft, card = card, onDraftChange = onDraftChange, onSave = onSave, onDelete = onDelete)
    }
}

@Composable
private fun InstallmentSheetContent(
    draft: RecurringChargeModel,
    card: WalletModel,
    onDraftChange: (RecurringChargeModel) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val isNew = draft.docId.isEmpty()
    // The same numbers the repository saves: a new one starts on the next statement day
    val preview = draft.copy(
        startDate = if (isNew) CardStatementHelper.startOfDay(RecurringChargeHelper.firstInstallmentDate(card.statementDay)) else draft.startDate,
        amount = RecurringChargeHelper.monthlyAmount(draft.totalAmount, draft.totalCount),
    )
    val monthsLeft = RecurringChargeHelper.monthsLeft(preview)
    val monthsDone = draft.totalCount - monthsLeft // paid before adding + charged by the app since
    val canSave = draft.name.isNotBlank() && (!isNew || (draft.totalAmount > 0 && monthsLeft > 0)) // a paid-off one can still be renamed

    Column(modifier = Modifier.imePadding()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                if (isNew) stringResource(R.string.wallet_title_new_installment, card.name) else stringResource(R.string.wallet_title_edit_installment),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            SheetFieldLabel(stringResource(R.string.wallet_lbl_total_amount))
            // Once saved only the name can change, so a month is never charged twice or skipped by an edit
            if (isNew) CurrencyTextField(
                amountInCents = draft.totalAmount,
                onAmountChange = { onDraftChange(draft.copy(totalAmount = it)) },
                prefixText = "-₱",
                inlinePrefix = true,
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            ) else Text(
                "-${formatBalance(draft.totalAmount)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            SheetFieldLabel(stringResource(R.string.wallet_lbl_recurring_name))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { if (it.length <= 30) onDraftChange(draft.copy(name = it)) },
                placeholder = { Text(stringResource(R.string.wallet_hint_installment_name)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors().copy(unfocusedIndicatorColor = outlinedColor),
                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth()
            )

            // Fewer months than already paid isn't possible, so "already paid" follows the months down
            if (isNew) StepperRow(stringResource(R.string.wallet_lbl_months)) {
                NumberStepper(
                    value = draft.totalCount,
                    onValueChange = { months -> onDraftChange(draft.copy(totalCount = months, paidBefore = draft.paidBefore.coerceAtMost(months - 1))) },
                    range = (draft.paidBefore + draft.chargeCount + 1).coerceAtLeast(1)..360
                )
            }
            if (isNew) StepperRow(stringResource(R.string.wallet_lbl_already_paid)) {
                NumberStepper(
                    value = draft.paidBefore,
                    onValueChange = { onDraftChange(draft.copy(paidBefore = it)) },
                    range = 0..(draft.totalCount - draft.chargeCount - 1).coerceAtLeast(0)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    AmountRow(stringResource(R.string.wallet_lbl_every_month), formatBalance(preview.amount))
                    AmountRow(stringResource(R.string.wallet_lbl_payments_left), monthsLeft.toString())
                    AmountRow(stringResource(R.string.wallet_lbl_still_to_pay), formatBalance(RecurringChargeHelper.stillToPay(preview)))
                    if (monthsLeft > 0) {
                        AmountRow(
                            stringResource(R.string.wallet_lbl_next_last),
                            stringResource(
                                R.string.wallet_lbl_next_last_value,
                                CardStatementHelper.formatDay(RecurringChargeHelper.nextDate(preview, card.statementDay)),
                                CardStatementHelper.formatDay(RecurringChargeHelper.lastInstallmentDate(preview, card.statementDay), withYear = true)
                            )
                        )
                    }
                    // Nothing done yet = the rows above already say it all
                    if (monthsDone > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            stringResource(
                                R.string.wallet_lbl_installment_months_note,
                                pluralStringResource(R.plurals.wallet_lbl_month_count, monthsDone, monthsDone),
                                pluralStringResource(R.plurals.wallet_lbl_month_count, monthsLeft, monthsLeft),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            Text(stringResource(R.string.wallet_btn_save_installment, formatBalance(preview.amount)))
        }
    }
}

@Composable
private fun StepperRow(label: String, stepper: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        stepper()
    }
}

@Preview
@Composable
private fun InstallmentSheetPreview() {
    DenuSpendTheme {
        Surface {
            InstallmentSheetContent(
                draft = RecurringChargeModel(name = "Phone", kind = RecurringChargeModel.KIND_INSTALLMENT, totalAmount = 3600000, totalCount = 24, paidBefore = 4),
                card = WalletModel(name = "Visa card", type = WalletModel.TYPE_CREDIT_CARD, statementDay = 28),
                onDraftChange = {},
                onSave = {},
                onDelete = {},
            )
        }
    }
}
