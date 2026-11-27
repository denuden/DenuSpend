package com.gmail.vondenuelle.denuspend.ui.budget.components

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetMoneyModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletStyle

/**
 * "By wallet": where the money came out of in the range. Expenses (from the day/month summaries) plus bills and other
 * money out (from the wallet history), per wallet, biggest first. A wallet that was closed since shows as "Closed wallet".
 * Expenses set to "Don't deduct" aren't from any wallet, so they aren't listed.
 */
@Composable
fun BudgetWalletsCard(
    summary: BudgetTotalSummaryModel,
    money: BudgetMoneyModel,
    onWalletClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val walletsById = money.wallets.associateBy { it.docId }
    val rows = (summary.expenseByWallet.keys + money.outByWallet.keys)
        .filter { it.isNotEmpty() }
        .associateWith { (summary.expenseByWallet[it] ?: 0L) + (money.outByWallet[it] ?: 0L) }
        .filterValues { it > 0 }
        .toList()
        .sortedByDescending { it.second }
    if (rows.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            stringResource(R.string.budget_lbl_by_wallet),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.budget_lbl_by_wallet_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                rows.forEachIndexed { i, (walletId, amount) ->
                    val wallet = walletsById[walletId]
                    WalletRow(
                        wallet = wallet,
                        amount = amount,
                        // A closed wallet has no details screen to open
                        onClick = if (wallet != null) ({ onWalletClick(walletId) }) else null
                    )
                    if (i < rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun WalletRow(wallet: WalletModel?, amount: Long, onClick: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Same color and icon as the wallet's own card
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(wallet?.let { WalletStyle.color(it.colorIndex) } ?: MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
        ) {
            Icon(
                wallet?.let { WalletStyle.icon(it.iconKey) } ?: Icons.Outlined.NoAccounts,
                contentDescription = null,
                tint = if (wallet != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                wallet?.name ?: stringResource(R.string.wallet_lbl_closed_wallet),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (wallet != null) {
                Text(
                    stringResource(if (wallet.isCreditCard) R.string.budget_lbl_by_wallet_card else R.string.budget_lbl_by_wallet_wallet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(peso(amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun BudgetWalletsCardPreview() {
    DenuSpendTheme {
        Surface {
            BudgetWalletsCard(
                summary = BudgetTotalSummaryModel(expenseByWallet = mapOf("cash" to 924000L, "payroll" to 1160000L, "gone" to 12000L)),
                money = BudgetMoneyModel(
                    outByWallet = mapOf("visa" to 519133L),
                    wallets = listOf(
                        WalletModel(docId = "cash", name = "Physical Wallet"),
                        WalletModel(docId = "payroll", name = "Payroll", colorIndex = 1),
                        WalletModel(docId = "visa", name = "Visa Classic", colorIndex = 2),
                    )
                ),
                onWalletClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
