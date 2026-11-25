package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import com.gmail.vondenuelle.denuspend.utils.RecurringChargeHelper

/**
 * One subscription ("Every month · next Nov 30") or installment ("4 of 24 · next Nov 28" with a bar) on the card's list.
 * Paid-off ones are faded. Tapping opens the edit sheet (name only).
 */
@Composable
fun RecurringChargeItem(
    modifier: Modifier = Modifier,
    charge: RecurringChargeModel,
    statementDay: Int,
    onClick: () -> Unit,
) {
    val isPaidOff = charge.endedReason == RecurringChargeModel.ENDED_PAID_OFF
    val next = CardStatementHelper.formatDay(RecurringChargeHelper.nextDate(charge, statementDay))
    val icon = when {
        isPaidOff -> Icons.Filled.Check
        charge.isInstallment -> Icons.Filled.EventRepeat
        else -> Icons.Filled.Autorenew
    }
    val subtitle = when {
        isPaidOff -> stringResource(R.string.wallet_lbl_paid_off, charge.totalCount)
        charge.isInstallment -> stringResource(
            R.string.wallet_lbl_installment_progress,
            charge.paidBefore + charge.chargeCount + 1,
            charge.totalCount,
            next
        )
        else -> stringResource(R.string.wallet_lbl_repeat_next, repeatText(charge), next)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .alpha(if (charge.active) 1f else 0.6f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(charge.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            // "Months done" out of all of them, including the ones paid before it was added
            if (charge.isInstallment && charge.totalCount > 0) {
                LinearProgressIndicator(
                    progress = { (charge.paidBefore + charge.chargeCount).toFloat() / charge.totalCount },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    drawStopIndicator = {},
                )
            }
        }
        Text(
            when {
                // A paid-off one shows what it cost in total, since its last month absorbed the rounding (₱6,666.68, not ₱6,666.66)
                charge.isInstallment && charge.endedReason == RecurringChargeModel.ENDED_PAID_OFF -> formatBalance(charge.totalAmount)
                charge.isInstallment -> stringResource(R.string.wallet_lbl_per_month, formatBalance(charge.amount))
                else -> formatBalance(charge.amount)
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

// "Every month", "Every 3 months", "Every week"…
@Composable
fun repeatText(charge: RecurringChargeModel): String {
    val every = charge.repeatEvery
    return when (charge.repeatUnit) {
        RecurringChargeModel.UNIT_DAY -> pluralStringResource(R.plurals.wallet_lbl_repeat_day, every, every)
        RecurringChargeModel.UNIT_WEEK -> pluralStringResource(R.plurals.wallet_lbl_repeat_week, every, every)
        RecurringChargeModel.UNIT_YEAR -> pluralStringResource(R.plurals.wallet_lbl_repeat_year, every, every)
        else -> pluralStringResource(R.plurals.wallet_lbl_repeat_month, every, every)
    }
}

@Preview
@Composable
private fun RecurringChargeItemPreview() {
    DenuSpendTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                RecurringChargeItem(charge = RecurringChargeModel(name = "AI assistant", amount = 110000), statementDay = 28, onClick = {})
                RecurringChargeItem(
                    charge = RecurringChargeModel(
                        name = "Phone", kind = RecurringChargeModel.KIND_INSTALLMENT, amount = 150000,
                        totalCount = 24, paidBefore = 4, totalAmount = 3600000
                    ),
                    statementDay = 28,
                    onClick = {}
                )
            }
        }
    }
}
