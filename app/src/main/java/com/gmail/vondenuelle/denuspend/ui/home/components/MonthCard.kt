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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// This month's spending vs this month's allowance, from the monthly doc. Tapping it opens the Budget tab.
// The month's allowance is only the daily allowances added so far (there's no monthly plan), so what's left is shown as
// "accumulated allowance" plus how long it lasts if only that is spent. When over, the red numbers carry the amount.
@Composable
fun MonthCard(
    modifier: Modifier = Modifier,
    allowance: Long, // cents, whole month
    spent: Long, // cents, whole month
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    // Not remembered, so it shows the new day/month after the listener restarts
    val today = LocalDate.now()
    val daysInMonth = today.lengthOfMonth()
    val daysLeft = daysInMonth - today.dayOfMonth + 1 // today counts as a day you can still spend
    val lastDay = today.withDayOfMonth(daysInMonth).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

    val isOver = spent > allowance
    val progress = when {
        allowance > 0 -> (spent.toFloat() / allowance).coerceIn(0f, 1f)
        spent > 0 -> 1f
        else -> 0f
    }
    // Neutral card so the month reads calmer than the purple Today card above it. The bar keeps the mauve tertiary.
    val onContainer = MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.home_lbl_month_day, today.dayOfMonth, daysInMonth),
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }

            Text(
                stringResource(if (allowance > 0) R.string.home_lbl_month_spent_of_caption else R.string.home_lbl_month_spent_caption),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
            // "₱spent / ₱allowance" in one Text: the no-break space keeps "/ ₱allowance" together,
            // so long amounts wrap as a whole second line instead of squeezing into a column
            val smallStyle = MaterialTheme.typography.titleMedium
            Text(
                if (isLoading) {
                    buildAnnotatedString { append("—") }
                } else {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = if (isOver) MaterialTheme.colorScheme.error else onContainer)) {
                            append("₱${CurrencyUtils.formatCents(spent)}")
                        }
                        if (allowance > 0) {
                            withStyle(SpanStyle(fontSize = smallStyle.fontSize, fontWeight = FontWeight.Normal)) {
                                append(" / ₱${CurrencyUtils.formatCents(allowance)}")
                            }
                        }
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )

            LinearProgressIndicator(
                progress = { if (isLoading) 0f else progress },
                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                trackColor = onContainer.copy(alpha = 0.16f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .height(8.dp)
            )

            when {
                isLoading -> Text(stringResource(R.string.lbl_loading), style = MaterialTheme.typography.bodySmall)
                allowance == 0L && spent == 0L -> Text(
                    stringResource(R.string.home_lbl_month_no_allowance),
                    style = MaterialTheme.typography.bodySmall
                )
                isOver -> Text(
                    stringResource(R.string.home_lbl_month_overspent),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {
                    val left = allowance - spent
                    Text(
                        stringResource(R.string.home_lbl_month_accumulated, "₱${CurrencyUtils.formatCents(left)}"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.verified_green_text)
                    )
                    Text(
                        stringResource(R.string.home_lbl_month_lasts_until, lastDay, "₱${CurrencyUtils.formatCents(left / daysLeft)}"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MonthCardPreview() {
    DenuSpendTheme {
        Surface {
            MonthCard(allowance = 1100000, spent = 823000, isLoading = false, onClick = {})
        }
    }
}

@Preview
@Composable
private fun MonthCardOverPreview() {
    DenuSpendTheme {
        Surface {
            MonthCard(allowance = 500000, spent = 620000, isLoading = false, onClick = {})
        }
    }
}
