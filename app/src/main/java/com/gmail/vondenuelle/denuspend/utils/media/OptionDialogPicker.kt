package com.gmail.vondenuelle.denuspend.utils.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OptionDialogPicker(
    showDialog : Boolean = false,
    openCamera : () -> Unit,
    openPhotoPicker : () -> Unit,
    onDismiss : () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Choose an action") },
            text = {
                Row {
                    TextButton(
                        onClick = {
                            openCamera()
                            onDismiss()
                        }
                    ) {
                        Text("Camera")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            openPhotoPicker()
                            onDismiss()
                        }
                    ) {
                        Text("Gallery")
                    }
                }
            },
            confirmButton = { /* optional */ }
        )
    }

}