package com.gmail.vondenuelle.denuspend.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils

// Home's main answer: today's allowance minus today's spending. Tapping it opens the Add tab.
@Composable
fun TodayLeftCard(
    modifier: Modifier = Modifier,
    allowance: Long, // cents
    spent: Long, // cents
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val isOver = spent > allowance
    // Spending with no allowance shows as full, same as the Add ring
    val progress = when {
        allowance > 0 -> (spent.toFloat() / allowance).coerceIn(0f, 1f)
        spent > 0 -> 1f
        else -> 0f
    }
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = onContainer,
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(onContainer.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    stringResource(if (isOver) R.string.home_lbl_over_today else R.string.home_lbl_left_today),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) MaterialTheme.colorScheme.error else onContainer,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }

            Text(
                text = if (isLoading) "—" else "₱${CurrencyUtils.formatCents(if (isOver) spent - allowance else allowance - spent)}",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                fontWeight = FontWeight.Medium,
                color = if (isOver) MaterialTheme.colorScheme.error else onContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = when {
                    isLoading -> stringResource(R.string.lbl_loading)
                    allowance == 0L && spent == 0L -> stringResource(R.string.home_lbl_no_allowance_today)
                    else -> stringResource(
                        R.string.home_lbl_spent_of_allowance,
                        "₱${CurrencyUtils.formatCents(spent)}",
                        "₱${CurrencyUtils.formatCents(allowance)}"
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            if (isLoading) {
                LinearProgressIndicator(
                    trackColor = onContainer.copy(alpha = 0.16f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = onContainer.copy(alpha = 0.16f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun TodayLeftCardPreview() {
    DenuSpendTheme {
        Surface {
            TodayLeftCard(allowance = 90000, spent = 51000, isLoading = false, onClick = {})
        }
    }
}

@Preview
@Composable
private fun TodayLeftCardOverPreview() {
    DenuSpendTheme {
        Surface {
            TodayLeftCard(allowance = 20000, spent = 23000, isLoading = false, onClick = {})
        }
    }
}
