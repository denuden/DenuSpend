package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun ProfileUpdateEmail(
    modifier: Modifier = Modifier,
    email : String,
    emailError : String,
    onEmailChange : (String) -> Unit,
    onSaveChanges : () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        Text(stringResource(R.string.profile_email_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.profile_email_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        AppTextField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.login_lbl_email),
            placeholder = stringResource(R.string.login_hint_email),
            leadingIcon = Icons.Filled.Email,
            error = emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = onSaveChanges,
        )

        Spacer(modifier = Modifier.height(24.dp))
        ProfileSheetButton(
            text = stringResource(R.string.profile_btn_save),
            onClick = onSaveChanges
        )
    }
}

@Preview
@Composable
private fun ProfileUpdateEmailPreview() {
    DenuSpendTheme {
        Surface {
            ProfileUpdateEmail(
                email = "",
                emailError = "",
                onEmailChange = {},
                onSaveChanges = {},
            )
        }
    }
}
