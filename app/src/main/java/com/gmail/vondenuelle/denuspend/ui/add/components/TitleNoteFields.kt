package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

private const val TITLE_MAX = 20
private const val NOTE_MAX = 50

/**
 * Title (20 characters) and an optional note (50 characters, the description) grouped in one soft panel.
 * The note starts as an "Add a note" button; it's already open when the transaction has one (edit mode).
 */
@Composable
fun TitleNoteFields(
    modifier: Modifier = Modifier,
    title: String,
    note: String,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    // "---" is how an empty description is stored, so it counts as no note
    var isNoteOpen by rememberSaveable { mutableStateOf(false) }
    val hasNote = note.isNotEmpty() && note != "---"
    // Edit mode's note arrives after the first frame; once open it stays open, so clearing the text doesn't hide the field
    LaunchedEffect(hasNote) { if (hasNote) isNoteOpen = true }
    val showNote = isNoteOpen || hasNote
    // Only a tap on "Add a note" moves the cursor there; edit mode opening it doesn't steal focus
    var focusNote by remember { mutableStateOf(false) }
    val noteFocus = remember { FocusRequester() }
    LaunchedEffect(showNote, focusNote) {
        if (showNote && focusNote) {
            noteFocus.requestFocus()
            focusNote = false
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            FieldWithCounter(
                label = stringResource(R.string.add_lbl_title),
                placeholder = stringResource(R.string.add_hint_title),
                value = title,
                max = TITLE_MAX,
                onValueChange = onTitleChange,
                isBold = true,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (showNote) {
                FieldWithCounter(
                    label = stringResource(R.string.add_lbl_note),
                    placeholder = stringResource(R.string.add_hint_note),
                    value = if (note == "---") "" else note,
                    max = NOTE_MAX,
                    // 2 lines so a full note shows; Enter isn't a newline, the note stays one line of text
                    onValueChange = { onNoteChange(it.replace("\n", " ")) },
                    isBold = false,
                    lines = 2,
                    modifier = Modifier.focusRequester(noteFocus),
                )
            } else {
                TextButton(
                    onClick = {
                        isNoteOpen = true
                        focusNote = true
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.add_btn_add_note), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

// Label + "n/max" counter over a borderless field. Input past max is ignored, same as the old form.
@Composable
private fun FieldWithCounter(
    label: String,
    placeholder: String,
    value: String,
    max: Int,
    onValueChange: (String) -> Unit,
    isBold: Boolean,
    lines: Int = 1,
    modifier: Modifier = Modifier,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.add_lbl_char_counter, value.length, max),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextField(
            value = value,
            onValueChange = { if (it.length <= max) onValueChange(it) },
            placeholder = { Text(placeholder) },
            singleLine = lines == 1,
            minLines = lines,
            maxLines = lines,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun TitleNoteFieldsPreview() {
    DenuSpendTheme {
        Surface {
            TitleNoteFields(
                title = "Lunch with team",
                note = "",
                onTitleChange = {},
                onNoteChange = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
