package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

/** The Save button pinned under the expense/allowance form; only the form above it scrolls. */
@Composable
fun FormSaveBar(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview
@Composable
private fun FormSaveBarPreview() {
    DenuSpendTheme {
        FormSaveBar(text = "Save expense · ₱150.00", onClick = {})
    }
}
