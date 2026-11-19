package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.common.components.AppTextField

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
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState).padding(16.dp)
            ,
    ) {

        Text("Please input your credentials to continue")
        Spacer(modifier = Modifier.height(16.dp))
        // Username
        AppTextField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.login_lbl_email),
            placeholder = stringResource(R.string.login_hint_email),
            leadingIcon = Icons.Filled.Email,
            error = emailError,
            keyboardType = KeyboardType.Email,
        )

        // Password
        Spacer(modifier = Modifier.height(16.dp))
        AppTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.login_lbl_password),
            placeholder = stringResource(R.string.login_hint_password),
            leadingIcon = Icons.Filled.Password,
            error = passwordError,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = onValidateCredentials,
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