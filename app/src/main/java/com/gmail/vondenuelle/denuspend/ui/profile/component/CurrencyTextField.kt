package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils

@Composable
fun CurrencyTextField(
    modifier: Modifier = Modifier,
    amountInCents: Long,
    onAmountChange: (Long) -> Unit,
    maxDigits: Int = 12,
    colors : TextFieldColors =  TextFieldDefaults.colors().copy(
        focusedContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent
    ),
    textStyle: TextStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 26.sp, fontWeight = FontWeight.Bold),
) {

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = CurrencyUtils.formatCents(amountInCents),
                selection = TextRange(CurrencyUtils.formatCents(amountInCents).length)
            )
        )
    }

    TextField(
        value = textFieldValue,
        onValueChange = { input ->
            val cleanString = input.text.filter { it.isDigit() }

            // enforce max digits
            if (cleanString.length > maxDigits) {
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(textFieldValue.text.length)
                )
                return@TextField
            }

            // convert to cents
            val newCents = if (cleanString.isNotEmpty()) cleanString.toLong() else 0L
            onAmountChange(newCents)

            // format string for display
            val formatted = CurrencyUtils.formatCents(newCents)
            textFieldValue = TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length) // cursor at end
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        placeholder = { Text ("00.00", style = TextStyle(textAlign = TextAlign.Center, fontSize = 26.sp, fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth())},
        prefix = { Text("₱",fontSize = 26.sp,) },
        colors = colors,
        textStyle = textStyle,
        modifier = modifier
    )
}