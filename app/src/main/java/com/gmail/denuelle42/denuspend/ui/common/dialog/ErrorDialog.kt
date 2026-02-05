package com.gmail.denuelle42.denuboilerplate.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gmail.denuelle42.denuboilerplate.R
import com.gmail.denuelle42.denuboilerplate.ui.common.dialog.DialogSettings.standardDialogProperties

@Composable
fun ErrorDialog(
    modifier: Modifier = Modifier,
    text: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    if (showDialog) {
        Dialog(onDismissRequest = { onDismissRequest() }, properties = standardDialogProperties) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE5CAC6))
            ) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = stringResource(R.string.error_error), tint = Color(
                        0xFFB71C1C
                    ), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.padding(vertical = 6.dp))
                    Text(
                        text = text,
                        textAlign = TextAlign.Center,
                    )
                }

            }
        }
    }
}
