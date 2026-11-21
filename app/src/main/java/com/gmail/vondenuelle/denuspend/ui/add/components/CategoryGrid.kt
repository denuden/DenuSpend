package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

/**
 * One-tap category picker: the dropdown's categories as a 3-column grid of tiles.
 * Uses CategoryDropdownButtonModel's icons and colors, so it matches Budget and the transaction rows.
 */
@Composable
fun CategoryGrid(
    modifier: Modifier = Modifier,
    selected: String,
    onSelected: (String) -> Unit,
) {
    val categories = CategoryDropdownButtonModel.list.drop(1) // [0] is the "" nothing-selected value
    // A plain chunked grid, not LazyVerticalGrid, because it sits inside the form's verticalScroll
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        categories.withIndex().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (index, name) ->
                    CategoryTile(
                        name = name,
                        index = index,
                        isSelected = name == selected,
                        onClick = { onSelected(name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// The pastel category color is the icon badge; selected tiles also get a tint and a border in the darker shade
@Composable
private fun CategoryTile(name: String, index: Int, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = CategoryDropdownButtonModel.colors[index]
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) color.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(2.dp, if (isSelected) CategoryDropdownButtonModel.selectedColors[index] else Color.Transparent),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(color, CircleShape)
            ) {
                // Dark icon on purpose: the badge colors are light pastels in both themes
                Icon(
                    CategoryDropdownButtonModel.icons[index],
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun CategoryGridPreview() {
    DenuSpendTheme {
        Surface {
            CategoryGrid(selected = "Food", onSelected = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
