package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.gmail.vondenuelle.denuspend.ui.profile.ProfileScreenEvents

@Composable
fun DeleteAccountDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean = false,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = {},
            text = {
                Text("Are you sure you want to delete your account?")
            },
            title = {
                Text("Delete Account", color = MaterialTheme.colorScheme.error)
            },
            icon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors()
                        .copy(containerColor = Color(0xFFE5CAC6)),
                    onClick = {
                        onConfirm()
                    }
                ) {
                    Text(
                        "Confirm",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )

    }
}