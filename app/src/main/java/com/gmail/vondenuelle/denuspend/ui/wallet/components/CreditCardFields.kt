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
 * A credit card's own fields in the wallet editor: amount owed, an optional limit and the statement day.
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
        StatementDayPicker(
            day = draft.statementDay,
            onDayChange = { onDraftChange(draft.copy(statementDay = it)) }
        )
        HelpText(stringResource(R.string.wallet_lbl_statement_day_help))
    }
}

// A read-only field that opens a 1–31 menu ("28th of every month")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatementDayPicker(day: Int, onDayChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    // Opens scrolled to the picked day (2 rows above it) instead of the 1st. scrollTo clamps at the end of the list.
    val menuScrollState = rememberScrollState()
    val itemHeightPx = with(LocalDensity.current) { 48.dp.toPx() }
    LaunchedEffect(expanded) {
        if (expanded && day > 3) menuScrollState.scrollTo(((day - 3) * itemHeightPx).toInt())
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (day in 1..31) stringResource(R.string.wallet_lbl_statement_day_value, CardStatementHelper.ordinal(day)) else "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(stringResource(R.string.wallet_lbl_statement_day_pick)) },
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
            (1..31).forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.wallet_lbl_statement_day_value, CardStatementHelper.ordinal(option))) },
                    onClick = {
                        expanded = false
                        onDayChange(option)
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
