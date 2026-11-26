package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CurrencyTextField
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper

/**
 * A credit card's own fields in the wallet editor: amount owed, an optional limit, the statement day and the payment due days.
 * Owed is stored as a negative balance, so expenses (which subtract) make it grow and payments (which add) shrink it.
 */
@Composable
fun CreditCardFields(
    modifier: Modifier = Modifier,
    draft: WalletModel,
    onDraftChange: (WalletModel) -> Unit,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.wallet_lbl_amount_owed))
        CurrencyTextField(
            amountInCents = draft.owed,
            onAmountChange = { onDraftChange(draft.copy(balance = -it)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.wallet_lbl_credit_limit))
        CurrencyTextField(
            amountInCents = draft.creditLimit,
            onAmountChange = { onDraftChange(draft.copy(creditLimit = it)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        HelpText(stringResource(R.string.wallet_lbl_credit_limit_help))

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.wallet_lbl_statement_day))
        MenuPickerField(
            selected = draft.statementDay,
            options = (1..31).toList(),
            optionText = { stringResource(R.string.wallet_lbl_statement_day_value, CardStatementHelper.ordinal(it)) },
            placeholder = stringResource(R.string.wallet_lbl_statement_day_pick),
            onPick = { onDraftChange(draft.copy(statementDay = it)) }
        )
        HelpText(stringResource(R.string.wallet_lbl_statement_day_help))

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.wallet_lbl_payment_due))
        MenuPickerField(
            selected = draft.paymentDueDays,
            options = (0..CardStatementHelper.MAX_DUE_DAYS).toList(),
            optionText = { days ->
                if (days == 0) stringResource(R.string.wallet_lbl_payment_due_not_set)
                else pluralStringResource(R.plurals.wallet_lbl_payment_due_value, days, days)
            },
            onPick = { onDraftChange(draft.copy(paymentDueDays = it)) }
        )
        HelpText(stringResource(R.string.wallet_lbl_payment_due_help))
    }
}

// A read-only field that opens a menu of numbers ("28th of every month", "20 days after the statement day").
// A selected value that isn't in options shows the placeholder (a new card has no statement day yet).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuPickerField(
    selected: Int,
    options: List<Int>,
    optionText: @Composable (Int) -> String,
    onPick: (Int) -> Unit,
    placeholder: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    // Opens scrolled to the picked option (2 rows above it) instead of the first. scrollTo clamps at the end of the list.
    val menuScrollState = rememberScrollState()
    val itemHeightPx = with(LocalDensity.current) { 48.dp.toPx() }
    val selectedIndex = options.indexOf(selected)
    LaunchedEffect(expanded) {
        if (expanded && selectedIndex > 2) menuScrollState.scrollTo(((selectedIndex - 2) * itemHeightPx).toInt())
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (selectedIndex >= 0) optionText(selected) else "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = outlinedColor,
                disabledIndicatorColor = outlinedColor,
                errorIndicatorColor = outlinedColor
            ),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            scrollState = menuScrollState,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = {
                        expanded = false
                        onPick(option)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun HelpText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
    )
}

@Preview
@Composable
private fun CreditCardFieldsPreview() {
    DenuSpendTheme {
        Surface {
            CreditCardFields(
                draft = WalletModel(name = "Visa card", type = WalletModel.TYPE_CREDIT_CARD, balance = -1234000, creditLimit = 5000000, statementDay = 28),
                onDraftChange = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
