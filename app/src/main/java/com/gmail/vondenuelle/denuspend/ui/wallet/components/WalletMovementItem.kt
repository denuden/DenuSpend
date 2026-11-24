package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.ui.common.components.ExpenseIcon
import com.gmail.vondenuelle.denuspend.ui.common.components.IncomeIcon
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.formatFirebaseDate

/**
 * One money in/out entry on the wallet details list. Same layout as TransactionItem so the merged list reads as one,
 * but the amount has a sign and is green for money in. Tapping it opens the delete confirm.
 */
@Composable
fun WalletMovementItem(
    modifier: Modifier = Modifier,
    movement: WalletMovementModel,
    otherWalletName: String?, // transfers only: the wallet on the other side (null = it was closed)
    onClick: () -> Unit,
) {
    val isIn = movement.amount > 0
    val icon = WalletMovementPresets.icon(if (movement.isTransfer) WalletMovementModel.PRESET_TRANSFER else movement.presetKey)
    // "To BDO · rent money", or just the note
    val transferText = when {
        !movement.isTransfer -> ""
        isIn -> stringResource(R.string.wallet_lbl_transfer_from_wallet, otherWalletName ?: stringResource(R.string.wallet_lbl_closed_wallet))
        else -> stringResource(R.string.wallet_lbl_transfer_to_wallet, otherWalletName ?: stringResource(R.string.wallet_lbl_closed_wallet))
    }
    val subtitle = listOf(transferText, movement.note).filter { it.isNotBlank() }.joinToString(" · ")

    Column(modifier = modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isIn) {
                IncomeIcon(icon = icon, modifier = Modifier.padding(end = 8.dp))
            } else {
                ExpenseIcon(icon = icon, modifier = Modifier.padding(end = 8.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    movement.title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleMedium
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        fontWeight = FontWeight.Light,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                if (isIn) "+${formatBalance(movement.amount)}" else formatBalance(movement.amount),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = if (isIn) colorResource(R.color.verified_green_text) else MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            formatFirebaseDate(movement.date.toDate(), format = "hh:mm a"),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp, bottom = 8.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(1.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun WalletMovementItemPreview() {
    DenuSpendTheme {
        Surface {
            Column {
                WalletMovementItem(
                    movement = WalletMovementModel(title = "Salary", presetKey = "salary", amount = 1500000),
                    otherWalletName = null,
                    onClick = {}
                )
                WalletMovementItem(
                    movement = WalletMovementModel(title = "Transfer", amount = -500000, transferId = "t", note = "rent money"),
                    otherWalletName = "BDO",
                    onClick = {}
                )
            }
        }
    }
}
