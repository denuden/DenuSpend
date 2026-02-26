package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun ProfileUpdatePassword(
    modifier: Modifier = Modifier,
    newPassword: String,
    newPasswordError: String,
    onChangeNewPassword: (String) -> Unit,
    reEnterPassword: String,
    onChangeReEnterPassword: (String) -> Unit,
    onSaveChanges: () -> Unit,
) {

    var showNewPassword by remember { mutableStateOf(false) }
    var showReEnterPassword by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        val fieldColors = TextFieldDefaults.colors().copy(
            focusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
        val fieldBorder = Modifier.border(
            1.dp,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.error
        )

        Text("Make sure your password contains 1 uppercase, 1 lowercase, 1 numeric, and 1 special character")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.lbl_enter_your_new_password),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = newPassword,
            onValueChange = {
                onChangeNewPassword(it)
            },
            label = {
                Text(text = stringResource(R.string.hint_password))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Password,
                    contentDescription = stringResource(R.string.hint_password)
                )
            },
            trailingIcon = {
                IconButton(onClick = {
                    showNewPassword = !showNewPassword
                }) {
                    Icon(
                        imageVector = if (showNewPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password
            ),
            shape = MaterialTheme.shapes.large,
            colors = fieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (newPasswordError.isNotEmpty()) fieldBorder else Modifier
                )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            stringResource(R.string.lbl_re_enter_new_password),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = reEnterPassword,
            onValueChange = {
                onChangeReEnterPassword(it)
            },
            label = {
                Text(text = stringResource(R.string.hint_password))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Password,
                    contentDescription = stringResource(R.string.hint_password)
                )
            },
            trailingIcon = {
                IconButton(onClick = {
                    showReEnterPassword = !showReEnterPassword
                }) {
                    Icon(
                        imageVector = if (showReEnterPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardActions = KeyboardActions(
                onDone = {
                    onSaveChanges()
                }
            ),
            visualTransformation = if (showReEnterPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password
            ),
            shape = MaterialTheme.shapes.large,
            colors = fieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (newPasswordError.isNotEmpty()) fieldBorder else Modifier
                )
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                onSaveChanges()
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
                stringResource(R.string.profile_btn_save).uppercase(),
            )
        }
    }

}

@Preview
@Composable
private fun ProfileUpdatePasswordPreview() {
    DenuSpendTheme {
        Surface {
            ProfileUpdatePassword(
                newPassword = "",
                newPasswordError = "",
                onChangeNewPassword = { },
                reEnterPassword = "",
                onChangeReEnterPassword = { },
                onSaveChanges = { },
            )
        }
    }
}