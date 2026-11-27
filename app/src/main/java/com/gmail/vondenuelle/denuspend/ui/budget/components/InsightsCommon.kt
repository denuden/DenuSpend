package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import kotlin.math.roundToInt

// Helpers shared by the Insights sections (one file per section, all used only by BudgetInsightsScreen)

fun peso(cents: Long) = "₱${CurrencyUtils.formatCents(cents)}"

// Rounded, not cut off: +1.8% shows as ▲2%. [previous] must be above 0.
fun percentChange(current: Long, previous: Long): Int = ((current - previous) * 100.0 / previous).roundToInt()

// Share of [whole], rounded: 27.7% shows as 28%
fun percentOf(part: Long, whole: Long): Int = if (whole > 0) (part * 100.0 / whole).roundToInt() else 0

// ▲ red when spending went up, ▼ green when it went down. Also used by Month by month.
@Composable
fun DeltaChip(percent: Int, modifier: Modifier = Modifier) {
    val (container, content, text) = when {
        percent > 0 -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "▲ $percent%")
        percent < 0 -> Triple(colorResource(R.color.verified_green_container), colorResource(R.color.verified_green_text), "▼ ${-percent}%")
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, stringResource(R.string.insights_compare_same_chip))
    }
    Surface(shape = MaterialTheme.shapes.small, color = container, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// Plain section card used by every Insights section below the summary
@Composable
fun InsightsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) { content() }
    }
}

// Unknown categories fall back to "Others" (last in the list)
fun categoryIndex(category: String): Int {
    val names = CategoryDropdownButtonModel.list.drop(1)
    return names.indexOf(category).takeIf { it >= 0 } ?: names.lastIndex
}
