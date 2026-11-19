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

@Composable
fun ProfileUpdatePassword(
    modifier: Modifier = Modifier,
    newPassword: String,
    newPasswordError: String,
    onChangeNewPassword: (String) -> Unit,
    reEnterPassword: String,
    reEnterPasswordError: String,
    onChangeReEnterPassword: (String) -> Unit,
    onSaveChanges: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        Text(stringResource(R.string.profile_password_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.profile_password_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        // Next moves to the re-enter field instead of closing the keyboard
        AppTextField(
            value = newPassword,
            onValueChange = onChangeNewPassword,
            label = stringResource(R.string.lbl_enter_your_new_password),
            placeholder = stringResource(R.string.hint_password),
            leadingIcon = Icons.Filled.Password,
            error = newPasswordError,
            isPassword = true,
            imeAction = ImeAction.Next,
        )

        Spacer(modifier = Modifier.height(16.dp))
        AppTextField(
            value = reEnterPassword,
            onValueChange = onChangeReEnterPassword,
            label = stringResource(R.string.lbl_re_enter_new_password),
            placeholder = stringResource(R.string.hint_password),
            leadingIcon = Icons.Filled.Password,
            error = reEnterPasswordError,
            isPassword = true,
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
private fun ProfileUpdatePasswordPreview() {
    DenuSpendTheme {
        Surface {
            ProfileUpdatePassword(
                newPassword = "",
                newPasswordError = "",
                onChangeNewPassword = { },
                reEnterPassword = "",
                reEnterPasswordError = "",
                onChangeReEnterPassword = { },
                onSaveChanges = { },
            )
        }
    }
}
