package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gmail.vondenuelle.denuspend.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// "Nov 3 – Nov 9", or one date when both ends are the same day. The millis are UTC midnight, so format in UTC.
fun formatPickerRange(startMillis: Long, endMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val start = Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val end = Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return if (start == end) start.format(formatter) else "${start.format(formatter)} – ${end.format(formatter)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit,
) {
    val state = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            // No future days. Compare calendar days, because the picker's millis are UTC midnight.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now())
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val start = state.selectedStartDateMillis
            TextButton(
                enabled = start != null,
                // Picking only a start day means a one-day range
                onClick = { if (start != null) onConfirm(start, state.selectedEndDateMillis ?: start) }
            ) { Text(stringResource(R.string.lbl_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.lbl_cancel)) }
        }
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}
