package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateItem
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CurrencyTextField
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import java.util.UUID

@Composable
fun TemplateEditor(
    draft: AllowanceTemplateModel,
    canSave: Boolean,
    onChange: (AllowanceTemplateModel) -> Unit,
    onSave: () -> Unit,
) {
    fun updateItem(id: String, transform: (AllowanceTemplateItem) -> AllowanceTemplateItem) =
        onChange(draft.copy(items = draft.items.map { if (it.id == id) transform(it) else it }))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            stringResource(if (draft.id.isEmpty()) R.string.add_template_editor_new else R.string.add_template_editor_edit),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) },
            label = { Text(stringResource(R.string.add_lbl_template_name)) },
            placeholder = { Text(stringResource(R.string.add_hint_template_name)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        )

        // Section header: the total sits next to the rows it adds up
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        ) {
            Text(
                stringResource(R.string.add_lbl_template_categories_header),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.add_lbl_template_total, "₱${CurrencyUtils.formatCents(draft.items.sumOf { it.amount })}"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider()

        draft.items.forEach { item ->
            // Keyed by the row id, so removing a row doesn't hand its dropdown/amount state to the next row
            key(item.id) {
                // One line per category: remove · category (ellipsized) · amount (scrolls inside its field when long)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        enabled = draft.items.size > 1,
                        onClick = { onChange(draft.copy(items = draft.items.filter { it.id != item.id })) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.add_cd_remove_row),
                            // Disabled (last row left) falls back to the default grey
                            tint = if (draft.items.size > 1) MaterialTheme.colorScheme.error else LocalContentColor.current,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    CategoryPicker(
                        selected = item.category,
                        onSelected = { category -> updateItem(item.id) { it.copy(category = category) } },
                        modifier = Modifier.weight(1f)
                    )
                    CurrencyTextField(
                        amountInCents = item.amount,
                        onAmountChange = { amount -> updateItem(item.id) { it.copy(amount = amount) } },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        singleLine = true,
                        modifier = Modifier.weight(2f) //amount gets twice the room of the category
                    )
                }
                HorizontalDivider()
            }
        }

        TextButton(
            onClick = { onChange(draft.copy(items = draft.items + AllowanceTemplateItem(id = UUID.randomUUID().toString()))) },
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.add_btn_add_category_row))
        }

        FilledTonalButton(
            onClick = onSave,
            enabled = canSave,
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.add_btn_save_template))
        }
    }
}

// Small "Food ▾" picker for the editor rows; the form's CategoryGrid is too big for a one-line row.
// The whole row stays tappable even when the name is cut with an ellipsis.
@Composable
private fun CategoryPicker(selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                selected.ifEmpty { stringResource(R.string.add_hint_select_category) },
                style = MaterialTheme.typography.titleSmall,
                // The hint is lighter and not bold, so it reads as "nothing picked yet"
                fontWeight = if (selected.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (selected.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // drop(1) skips the blank "nothing selected" entry, same as the form dropdown
            CategoryDropdownButtonModel.list.drop(1).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}
