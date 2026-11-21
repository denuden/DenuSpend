package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.BudgetFilterChip
import java.time.format.DateTimeFormatter
import java.util.Locale

// Category icon + name, the range's dates, and the range chip
@Composable
fun InsightsHeader(
    insights: BudgetInsightsModel,
    filterLabel: String,
    onChangeFilter: (BudgetChartFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = categoryIndex(insights.category)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val dates = if (insights.startDate == insights.endDate) {
        insights.startDate.format(dateFormatter)
    } else {
        "${insights.startDate.format(dateFormatter)} – ${insights.endDate.format(dateFormatter)}"
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CategoryDropdownButtonModel.colors[index])
        ) {
            Icon(
                CategoryDropdownButtonModel.icons[index],
                contentDescription = null,
                tint = CategoryDropdownButtonModel.selectedColors[index]
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                insights.category,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                pluralStringResource(R.plurals.insights_lbl_range_days, insights.dayCount, dates, insights.dayCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BudgetFilterChip(filterLabel = filterLabel, onChangeFilter = onChangeFilter)
    }
}
