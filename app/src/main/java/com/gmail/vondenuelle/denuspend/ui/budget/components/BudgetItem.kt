package com.gmail.vondenuelle.denuspend.ui.budget.components

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils

// One row of the breakdown list. Meant to sit inside a shared Card, so it has no card of its own.
// The bar fills toward the category's allowance and turns red when over. No allowance = no bar.
@Composable
fun BudgetItem(
    modifier: Modifier = Modifier,
    category : String,
    icon : ImageVector,
    color : Color, //category color, same as its pie slice
    iconTint : Color,
    amount : Long, //spent, cents
    allowance : Long, //allowance for this category in the same range, cents
    transactionCount : Int,
    onClick : () -> Unit,
) {
    val isOver = allowance > 0 && amount > allowance

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(color = color, shape = CircleShape)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "₱${CurrencyUtils.formatCents(amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(R.plurals.budget_transactions_made, transactionCount, transactionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                // Same wording as the header: "of ₱720.80 · ₱7,334.00 over" (red) or "of ₱600.00 · ₱150.00 left" (green)
                val statusColor = if (isOver) MaterialTheme.colorScheme.error else colorResource(R.color.verified_green_text)
                val status = when {
                    allowance == 0L -> null
                    isOver -> stringResource(R.string.budget_lbl_over, "₱${CurrencyUtils.formatCents(amount - allowance)}")
                    else -> stringResource(R.string.budget_lbl_left, "₱${CurrencyUtils.formatCents(allowance - amount)}")
                }
                val ofAllowance = stringResource(R.string.budget_lbl_of, "₱${CurrencyUtils.formatCents(allowance)}")
                val noAllowance = stringResource(R.string.budget_lbl_no_allowance)
                Text(
                    buildAnnotatedString {
                        if (status == null) {
                            append(noAllowance)
                        } else {
                            append("$ofAllowance · ")
                            withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.SemiBold)) { append(status) }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (allowance > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (amount.toFloat() / allowance).coerceIn(0f, 1f) },
                    color = if (isOver) MaterialTheme.colorScheme.error else color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Preview
@Composable
private fun BudgetItemPreview() {
    DenuSpendTheme() {
        Surface() {
            BudgetItem(
                category = "Food",
                icon = Icons.Filled.Restaurant,
                color = Color(0xFFFF8A80),
                iconTint = Color(0xFFD32F2F),
                amount = 499500,
                allowance = 600000,
                transactionCount = 12,
                onClick = {},
            )
        }
    }
}
