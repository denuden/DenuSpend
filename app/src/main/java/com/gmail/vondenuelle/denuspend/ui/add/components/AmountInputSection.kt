package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.common.components.InformationTooltip
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

// Quick amounts in cents: +₱50 … +₱1,000
private val QUICK_AMOUNTS = listOf(5000L, 10000L, 20000L, 50000L, 100000L)

/**
 * The big amount at the top of the expense/allowance form, with the info tooltip and quick "+₱" chips.
 * Chips add to what's typed, so ₱100 then +₱50 = ₱150.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmountInputSection(
    modifier: Modifier = Modifier,
    amountInCents: Long,
    onAmountChange: (Long) -> Unit,
    prefixText: String,
    amountColor: Color,
    tooltipTitle: String,
    tooltipText: String,
    todayLine: String? = null, // "Food left today: …", shown right under the amount so it's in view while typing
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.add_lbl_amount).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            // Wrapped, because the tooltip's own modifier doesn't place it in this Box
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                InformationTooltip(richTooltipSubheadText = tooltipTitle, richTooltipText = tooltipText)
            }
        }

        CurrencyTextField(
            amountInCents = amountInCents,
            onAmountChange = onAmountChange,
            prefixText = prefixText,
            inlinePrefix = true,
            textStyle = MaterialTheme.typography.displayMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = amountColor
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        todayLine?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // FlowRow so the chips wrap instead of clipping on narrow phones or big font sizes
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            QUICK_AMOUNTS.forEach { cents ->
                AssistChip(
                    onClick = { onAmountChange(amountInCents + cents) },
                    label = {
                        Text(
                            stringResource(R.string.add_lbl_quick_amount, "%,d".format(cents / 100)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun AmountInputSectionPreview() {
    DenuSpendTheme {
        Surface {
            AmountInputSection(
                amountInCents = 15000,
                onAmountChange = {},
                prefixText = "-₱",
                amountColor = MaterialTheme.colorScheme.error,
                tooltipTitle = "Adding Expense Transaction",
                tooltipText = "Add the amount of the expense transaction.",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
