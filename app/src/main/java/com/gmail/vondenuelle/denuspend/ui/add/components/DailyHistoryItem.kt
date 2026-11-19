package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun DailyHistoryItem(
    modifier: Modifier = Modifier,
    date : String,
    expense : String,
    totalBudget : String,
    remaining : String, // already formatted, e.g. "₱3,800.00 left" or "₱200.00 over"
    progress : Float,
    isOverspent : Boolean = false,
    onClick : () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    expense,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    " spent of $totalBudget",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = if (isOverspent) MaterialTheme.colorScheme.error else ProgressIndicatorDefaults.linearColor,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    remaining,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverspent) MaterialTheme.colorScheme.error else colorResource(R.color.verified_green_text)
                )
            }
        }
    }
}

@Preview
@Composable
private fun DailyHistoryItemPreview() {
    DenuSpendTheme(){
        Surface() {
            DailyHistoryItem(
                modifier = Modifier.fillMaxWidth(),
                date = "Wed, Nov 19",
                expense ="₱1,456.00",
                totalBudget = "₱3,000.00",
                remaining = "₱1,544.00 left",
                progress = 0.48f,
            ){}
        }
    }
}
