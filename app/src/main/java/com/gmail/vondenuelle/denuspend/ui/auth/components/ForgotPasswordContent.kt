package com.gmail.vondenuelle.denuspend.ui.auth.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun ForgotPasswordContent(
    email : String,
    emailError : String,
    onChangeEmail : (String) -> Unit,
    onSend : () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
    ) {
        Text(stringResource(R.string.lbl_password_reset_instructions))
        Spacer(modifier = Modifier.height(16.dp))
        val fieldColors = TextFieldDefaults.colors().copy(
            focusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
        val fieldBorder = Modifier.border(
            1.dp, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.error
        )
        // Username
        Text(stringResource(R.string.login_lbl_email), modifier = Modifier.align(Alignment.Start))
        TextField(
            value = email,
            onValueChange = {
                onChangeEmail(it)
            },
            label = {
                Text(text = stringResource(R.string.login_hint_email))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = stringResource(R.string.login_hint_email)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email
            ),
            shape = MaterialTheme.shapes.large,
            colors = fieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (emailError.isNotEmpty()) fieldBorder else Modifier
                )
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                onSend()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = ButtonDefaults.buttonElevation(0.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                stringResource(R.string.btn_send_password_reset).uppercase(),
            )
        }


    }
}

@Preview
@Composable
private fun ForgotPasswordContentPreview() {
    DenuSpendTheme {
        Surface {
            ForgotPasswordContent(
                email = "",
                emailError = "",
                onChangeEmail = {},
                onSend = {}
            )
        }
    }
}