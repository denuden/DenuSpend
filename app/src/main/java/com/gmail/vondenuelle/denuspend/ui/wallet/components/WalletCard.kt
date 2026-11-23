package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

@Composable
fun WalletCard(
    modifier: Modifier = Modifier,
    wallet: WalletModel,
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
                }
                if (wallet.balance < 0) NegativeBadge()
            }

            Spacer(modifier = Modifier.height(20.dp))

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

// The "!" in a negative wallet's top-right corner, so it stands out before reading the balance
@Composable
fun NegativeBadge(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
    ) {
        Icon(
            Icons.Filled.PriorityHigh,
            contentDescription = stringResource(R.string.wallet_lbl_negative),
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
            }
        }
    }
}
