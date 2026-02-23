package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@Composable
fun ProfileReauthenticateUser(
    modifier: Modifier = Modifier,
    email : String,
    emailError : String,
    onEmailChange : (String) -> Unit,
    password : String,
    passwordError : String,
    onPasswordChange : (String) -> Unit,
    onValidateCredentials : () -> Unit,
) {
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp),
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
            1.dp, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.error
        )

        Text("Please input your credentials to continue")
        Spacer(modifier = Modifier.height(16.dp))
        // Username
        Text(
            stringResource(R.string.login_lbl_email),
            modifier = Modifier.align(Alignment.Start)
        )
        TextField(
            value = email,
            onValueChange = {
                onEmailChange(it)
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

        // Password
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.login_lbl_password),
            modifier = Modifier.align(Alignment.Start)
        )
        TextField(
            value = password,
            onValueChange = {
                onPasswordChange(it)
            },
            label = {
                Text(text = stringResource(R.string.login_hint_password))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Password,
                    contentDescription = stringResource(R.string.login_hint_password)
                )
            },
            trailingIcon = {
                IconButton(onClick = {
                    showPassword = !showPassword
                }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardActions = KeyboardActions(
                onDone = {
                    onValidateCredentials()
                }
            ),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
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
                    if (passwordError.isNotEmpty()) fieldBorder else Modifier
                )
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                onValidateCredentials()
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
private fun ProfileReauthenticateUserPreview() {

}