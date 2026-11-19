package com.gmail.vondenuelle.denuspend.ui.auth.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.EmailRequest
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.ui.auth.AuthScreenEvents
import com.gmail.vondenuelle.denuspend.ui.auth.AuthScreenState
import com.gmail.vondenuelle.denuspend.ui.auth.AuthViewModel
import com.gmail.vondenuelle.denuspend.ui.auth.components.AuthGlowButton
import com.gmail.vondenuelle.denuspend.ui.auth.components.AuthHeader
import com.gmail.vondenuelle.denuspend.ui.common.components.AppTextField
import com.gmail.vondenuelle.denuspend.ui.common.components.BackButton
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents

@Composable
fun ForgotPasswordScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnNavigate -> onNavigate(event.route, event.behavior.toNavOptions())
            is OneTimeEvents.OnPopBackStack -> onPopBackStack()
            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LoadingDialog(
        text = stringResource(R.string.lbl_loading),
        showDialog = state.isLoading
    ) { }

    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    ForgotPasswordScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun ForgotPasswordScreenContent(
    modifier: Modifier = Modifier,
    state: AuthScreenState,
    onEvent: (AuthScreenEvents) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val onSend = { onEvent(AuthScreenEvents.OnSendPasswordReset(EmailRequest(state.forgotPassEmail))) }

    Surface(modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Bottom
                    )
                )
                .imePadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BackButton(
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 8.dp)
            ) { onEvent(AuthScreenEvents.OnNavigateBack) }

            if (!state.isPasswordResetSent) {
                // ============= FORM
                AuthHeader(
                    title = stringResource(R.string.forgot_title),
                    subtitle = stringResource(R.string.lbl_password_reset_instructions)
                )
                Spacer(modifier = Modifier.height(48.dp))

                AppTextField(
                    value = state.forgotPassEmail,
                    onValueChange = { onEvent(AuthScreenEvents.OnChangeForgotPassEmailField(it)) },
                    label = stringResource(R.string.login_lbl_email),
                    placeholder = stringResource(R.string.login_hint_email),
                    leadingIcon = Icons.Filled.Email,
                    error = state.forgotPassEmailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onImeAction = onSend,
                )

                AuthGlowButton(
                    text = stringResource(R.string.btn_send_password_reset),
                    onClick = onSend,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                // ============= SENT (shown after Firebase accepted the request)
                Spacer(modifier = Modifier.height(48.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MarkEmailRead,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.forgot_sent_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Text(
                    text = stringResource(R.string.forgot_sent_message, state.forgotPassEmail.trim()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                AuthGlowButton(
                    text = stringResource(R.string.forgot_btn_back_to_sign_in),
                    onClick = { onEvent(AuthScreenEvents.OnNavigateBack) },
                    modifier = Modifier.padding(top = 40.dp)
                )
                TextButton(
                    onClick = onSend,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.forgot_btn_resend))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ForgotPasswordScreenPreview() {
    DenuSpendTheme {
        ForgotPasswordScreenContent(state = AuthScreenState())
    }
}

@Preview
@Composable
private fun ForgotPasswordScreenSentPreview() {
    DenuSpendTheme {
        ForgotPasswordScreenContent(
            state = AuthScreenState(forgotPassEmail = "user@email.com", isPasswordResetSent = true)
        )
    }
}
