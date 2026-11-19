package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.common.components.AppTextField
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

// The email is the signed-in user's, so only the password is asked for
@Composable
fun ProfileReauthenticateUser(
    modifier: Modifier = Modifier,
    email : String,
    password : String,
    passwordError : String,
    onPasswordChange : (String) -> Unit,
    onValidateCredentials : () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        Text(stringResource(R.string.profile_reauth_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.profile_reauth_text, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

        Spacer(modifier = Modifier.height(24.dp))
        ProfileSheetButton(
            text = stringResource(R.string.profile_btn_continue),
            onClick = onValidateCredentials
        )
    }
}

@Preview
@Composable
private fun ProfileReauthenticateUserPreview() {
    DenuSpendTheme {
        Surface {
            ProfileReauthenticateUser(
                email = "user@mail.com",
                password = "",
                passwordError = "",
                onPasswordChange = {},
                onValidateCredentials = {},
            )
        }
    }
}
