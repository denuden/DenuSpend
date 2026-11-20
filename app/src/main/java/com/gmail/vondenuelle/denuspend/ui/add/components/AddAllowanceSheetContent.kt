package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateItem
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils

private val CardShape = RoundedCornerShape(16.dp)
private val CardHeight = 104.dp

// Sheet behind the "Add Allowance" button: "Custom allowance" opens the normal form,
// the grid below adds a saved template for today (the screen asks to confirm first).
@Composable
fun AddAllowanceSheetContent(
    modifier: Modifier = Modifier,
    templates: List<AllowanceTemplateModel>,
    hasLoaded: Boolean,
    onCustomClick: () -> Unit,
    onTemplateClick: (AllowanceTemplateModel) -> Unit,
    onManageClick: () -> Unit, //also used by the "New template" card
) {
    // One grid for the whole sheet, so the header rows and the cards scroll together
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    stringResource(R.string.add_btn_add_allowance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomAllowanceRow(onClick = onCustomClick)
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.add_lbl_from_template),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.add_btn_manage_templates),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable(onClick = onManageClick)
                            .padding(4.dp)
                    )
                }
                if (hasLoaded && templates.isEmpty()) {
                    Text(
                        stringResource(R.string.add_templates_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        if (!hasLoaded) {
            // Loading placeholders, same size as the cards so nothing jumps when templates arrive
            items(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CardHeight)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CardShape)
                )
            }
        } else {
            items(templates) { template ->
                TemplateCard(template = template, onClick = { onTemplateClick(template) })
            }
            item {
                NewTemplateCard(onClick = onManageClick)
            }
        }
    }
}

@Composable
private fun CustomAllowanceRow(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = colorResource(R.color.verified_green_text), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.add_lbl_custom_allowance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.add_lbl_custom_allowance_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TemplateCard(template: AllowanceTemplateModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().height(CardHeight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                template.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "₱${CurrencyUtils.formatCents(template.items.sumOf { it.amount })}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.verified_green_text),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            // Two categories fit by name; more than that shows a count
            Text(
                if (template.items.size <= 2) template.items.joinToString("  •  ") { it.category }
                else pluralStringResource(R.plurals.add_lbl_template_categories, template.items.size, template.items.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NewTemplateCard(onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    // Dashed outline drawn by hand, Material 3 has no dashed card
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(CardShape)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.add_template_editor_new),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Preview
@Composable
private fun AddAllowanceSheetContentPreview() {
    DenuSpendTheme {
        Surface {
            AddAllowanceSheetContent(
                templates = listOf(
                    AllowanceTemplateModel("1", "Weekday", listOf(AllowanceTemplateItem("a", "Food", 30000), AllowanceTemplateItem("b", "Transportation", 15000))),
                    AllowanceTemplateModel("2", "Weekend", listOf(AllowanceTemplateItem("c", "Food", 50000), AllowanceTemplateItem("d", "Entertainment", 30000))),
                    AllowanceTemplateModel("3", "Payday", listOf(AllowanceTemplateItem("e", "Food", 50000), AllowanceTemplateItem("f", "Personal", 40000), AllowanceTemplateItem("g", "Family", 30000))),
                ),
                hasLoaded = true,
                onCustomClick = {},
                onTemplateClick = {},
                onManageClick = {},
            )
        }
    }
}
