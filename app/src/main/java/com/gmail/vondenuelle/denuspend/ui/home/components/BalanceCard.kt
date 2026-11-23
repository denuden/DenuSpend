package com.gmail.vondenuelle.denuspend.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.common.components.InformationTooltip
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.components.NegativeBadge
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletStyle
import com.gmail.vondenuelle.denuspend.ui.wallet.components.formatBalance

// Real money across all wallets, dark so it reads differently from the allowance (plan) cards above it.
// The card opens the Wallet tab; each mini card opens that wallet's details.
@Composable
fun BalanceCard(
    modifier: Modifier = Modifier,
    wallets: List<WalletModel>,
    isLoading: Boolean,
    onClick: () -> Unit,
    onWalletClick: (String) -> Unit,
) {
    val onContainer = MaterialTheme.colorScheme.inverseOnSurface

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = onContainer,
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(onContainer.copy(alpha = 0.14f), CircleShape)
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    pluralStringResource(R.plurals.home_lbl_total_balance, wallets.size, wallets.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                InformationTooltip(
                    richTooltipSubheadText = stringResource(R.string.lbl_wallet_vs_allowance_title),
                    richTooltipText = stringResource(R.string.lbl_wallet_vs_allowance_text)
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }

            Text(
                if (isLoading) "—" else formatBalance(wallets.sumOf { it.balance }),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
            )

            // Edge to edge inside the card so more than 3 wallets scroll sideways
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(wallets, key = { it.docId }) { wallet ->
                    MiniWalletCard(wallet = wallet, onClick = { onWalletClick(wallet.docId) })
                }
            }
        }
    }
}

// Same color and icon as the wallet's real card. The wallet colors are made for white text.
@Composable
private fun MiniWalletCard(wallet: WalletModel, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = WalletStyle.color(wallet.colorIndex),
        contentColor = Color.White,
        modifier = Modifier
            .width(120.dp)
            .height(72.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(WalletStyle.icon(wallet.iconKey), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.weight(1f))
                if (wallet.balance < 0) NegativeBadge(size = 18.dp)
            }
            Column {
                Text(
                    wallet.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatBalance(wallet.balance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
private fun BalanceCardPreview() {
    DenuSpendTheme {
        Surface {
            BalanceCard(
                wallets = listOf(
                    WalletModel(docId = "u_physical", name = "Cash", colorIndex = 4, iconKey = "cash", balance = 120000),
                    WalletModel(docId = "2", name = "GCash", colorIndex = 1, iconKey = "ewallet", balance = 325000),
                    WalletModel(docId = "3", name = "BPI Savings", colorIndex = 6, iconKey = "bank", balance = 800000),
                ),
                isLoading = false,
                onClick = {},
                onWalletClick = {}
            )
        }
    }
}
