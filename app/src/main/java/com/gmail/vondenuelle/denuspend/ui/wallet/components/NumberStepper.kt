package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

/** "– 24 +" for the subscription and installment sheets (repeat every, months, already paid). */
@Composable
fun NumberStepper(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        FilledTonalIconButton(onClick = { onValueChange(value - 1) }, enabled = value > range.first) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.wallet_cd_decrease))
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 40.dp)
        )
        FilledTonalIconButton(onClick = { onValueChange(value + 1) }, enabled = value < range.last) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.wallet_cd_increase))
        }
    }
}

@Preview
@Composable
private fun NumberStepperPreview() {
    DenuSpendTheme {
        Surface {
            NumberStepper(value = 24, onValueChange = {}, range = 1..360)
        }
    }
}
