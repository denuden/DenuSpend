package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper

@Composable
fun WalletCard(
    modifier: Modifier = Modifier,
    wallet: WalletModel,
    installmentsToCome: Long = 0L, // a card's installments not on a statement yet; they're held against the limit
    onClick: (() -> Unit)? = null, // null = not clickable
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WalletStyle.color(wallet.colorIndex), contentColor = Color.White),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(MaterialTheme.shapes.extraLarge).clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .padding(10.dp)
                ) {
                    Icon(WalletStyle.icon(wallet.iconKey), contentDescription = null)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Skipped when the name already says it (the default "Physical Wallet")
                    val physicalLabel = stringResource(R.string.wallet_lbl_physical)
                    if (wallet.isPhysical && !wallet.name.equals(physicalLabel, ignoreCase = true)) {
                        Text(
                            physicalLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    if (wallet.isCreditCard) {
                        Text(
                            if (wallet.statementDay in 1..31) stringResource(R.string.wallet_lbl_card_kind, CardStatementHelper.ordinal(wallet.statementDay))
                            else stringResource(R.string.wallet_lbl_card_kind_no_day),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (wallet.needsAttention) NegativeBadge(isCreditCard = wallet.isCreditCard)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (wallet.isCreditCard) {
                CardBalance(wallet, installmentsToCome)
            } else {
                Text(
                    stringResource(R.string.wallet_lbl_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatBalance(wallet.balance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (wallet.balance < 0) {
                         NegativeChip(modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

// "Owed now ₱12,340.00", then a limit bar with "₱37,660 available / of ₱50,000" when a limit is set.
// Installments to come are held against the limit like a bank does: a lighter part of the bar, and "available" subtracts them.
// A card paid past ₱0 shows "Credit on card" instead, since the bank owes the user then.
@Composable
private fun CardBalance(wallet: WalletModel, installmentsToCome: Long) {
    val labelColor = Color.White.copy(alpha = 0.8f)
    Text(
        stringResource(if (wallet.balance > 0) R.string.wallet_lbl_card_credit else R.string.wallet_lbl_owed_now),
        style = MaterialTheme.typography.labelMedium,
        color = labelColor
    )
    Text(
        formatBalance(if (wallet.balance > 0) wallet.balance else wallet.owed),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    if (wallet.creditLimit > 0) {
        val used = (wallet.owed.toFloat() / wallet.creditLimit).coerceIn(0f, 1f)
        val held = ((wallet.owed + installmentsToCome).toFloat() / wallet.creditLimit).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.28f))
        ) {
            Box(modifier = Modifier.fillMaxWidth(held).fillMaxHeight().background(Color.White.copy(alpha = 0.55f)))
            Box(modifier = Modifier.fillMaxWidth(used).fillMaxHeight().background(Color.White))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            val available = wallet.creditLimit - wallet.owed - installmentsToCome
            Text(
                if (available >= 0) stringResource(R.string.wallet_lbl_available, formatBalance(available))
                else stringResource(R.string.wallet_lbl_over_limit, formatBalance(-available)),
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.wallet_lbl_of_limit, formatBalance(wallet.creditLimit)),
                style = MaterialTheme.typography.labelMedium,
                color = labelColor
            )
        }
    }
}

// The "!" in a negative wallet's top-right corner, so it stands out before reading the balance.
// On a card it means over the limit (a card's balance is always negative), so TalkBack says that instead.
@Composable
fun NegativeBadge(modifier: Modifier = Modifier, size: Dp = 24.dp, isCreditCard: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
    ) {
        Icon(
            Icons.Filled.PriorityHigh,
            contentDescription = stringResource(if (isCreditCard) R.string.wallet_cd_over_limit else R.string.wallet_lbl_negative),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(size * 0.7f)
        )
    }
}

// Shown next to a balance below ₱0, readable on any card color
@Composable
fun NegativeChip(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.wallet_lbl_negative),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Preview
@Composable
private fun WalletCardPreview() {
    DenuSpendTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                WalletCard(wallet = WalletModel(docId = "uid_physical", name = "Physical Wallet", balance = 150000))
                Spacer(modifier = Modifier.height(8.dp))
                WalletCard(wallet = WalletModel(name = "Credit Card", colorIndex = 7, iconKey = "card", balance = -320050))
                Spacer(modifier = Modifier.height(8.dp))
                WalletCard(wallet = WalletModel(name = "Visa card", colorIndex = 2, iconKey = "card", balance = -1234000, type = WalletModel.TYPE_CREDIT_CARD, creditLimit = 5000000, statementDay = 28))
            }
        }
    }
}

// The amount on the small wallet cards (Home, "Paid from"): a card shows what's owed (₱12,340.00) instead of a negative balance.
// The word "Owed" goes in the card's top row (MiniOwedLabel), since "Owed ₱12,340.00" doesn't fit a 120dp card.
fun miniBalanceText(wallet: WalletModel): String =
    if (wallet.isCreditCard && wallet.balance <= 0) formatBalance(wallet.owed)
    else formatBalance(wallet.balance)

@Composable
fun MiniOwedLabel(wallet: WalletModel, modifier: Modifier = Modifier) {
    if (wallet.isCreditCard && wallet.balance <= 0) {
        Text(stringResource(R.string.wallet_lbl_owed_short), style = MaterialTheme.typography.labelSmall, modifier = modifier)
    }
}
