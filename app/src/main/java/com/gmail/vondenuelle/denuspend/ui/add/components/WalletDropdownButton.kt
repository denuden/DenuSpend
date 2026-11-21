package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletStyle
import com.gmail.vondenuelle.denuspend.ui.wallet.components.formatBalance

/**
 * "Paid from" picker: the user's wallets plus "Don't deduct from a wallet".
 * An id that's no longer in the list (after the list loaded) is a closed wallet, so it's named that instead of showing blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDropdownButton(
    modifier: Modifier = Modifier,
    wallets: List<WalletModel>,
    hasLoadedWallets: Boolean,
    selectedId: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val selected = wallets.find { it.docId == selectedId }
    val label = when {
        selectedId.isEmpty() -> stringResource(R.string.wallet_lbl_no_wallet)
        selected != null -> selected.name
        hasLoadedWallets -> stringResource(R.string.wallet_lbl_closed_wallet)
        else -> ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            leadingIcon = selected?.let {
                { Icon(WalletStyle.icon(it.iconKey), contentDescription = null, tint = WalletStyle.color(it.colorIndex)) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = outlinedColor,
                disabledIndicatorColor = outlinedColor,
                errorIndicatorColor = outlinedColor
            ),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            wallets.forEach { wallet ->
                DropdownMenuItem(
                    text = { Text(wallet.name, style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = {
                        Icon(WalletStyle.icon(wallet.iconKey), contentDescription = null, tint = WalletStyle.color(wallet.colorIndex))
                    },
                    trailingIcon = {
                        Text(
                            formatBalance(wallet.balance),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (wallet.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(wallet.docId)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.wallet_lbl_no_wallet), style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Outlined.MoneyOff, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSelected("")
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
        }
    }
}
