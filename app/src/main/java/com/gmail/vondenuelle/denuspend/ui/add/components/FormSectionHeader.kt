package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * "Category", "Paid from"… on the expense/allowance form, with an optional live note on the right (e.g. "₱500 → ₱350 after")
 * and an optional line under the title for longer notes (e.g. "Food left today: …").
 */
@Composable
fun FormSectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    trailingText: String? = null,
    supportingText: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (trailingText != null) {
                Text(
                    trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (supportingText != null) {
            Text(
                supportingText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
