package com.gmail.vondenuelle.denuspend.ui.auth.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.RegisterRequest
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.auth.AuthScreenEvents
import com.gmail.vondenuelle.denuspend.ui.auth.AuthScreenState
import com.gmail.vondenuelle.denuspend.ui.auth.AuthViewModel
import com.gmail.vondenuelle.denuspend.ui.auth.components.AuthGlowButton
import com.gmail.vondenuelle.denuspend.ui.auth.components.AuthHeader
import com.gmail.vondenuelle.denuspend.ui.auth.components.SocialSignInButtons
import com.gmail.vondenuelle.denuspend.ui.common.components.AppTextField
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents

@Composable
fun RegisterScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    onLoginOrRegisterCompleted: () -> Unit,
    viewModel : AuthViewModel = hiltViewModel()
) {
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    ObserveAsEvents(viewModel.channel) { event ->
        when(event){
            is OneTimeEvents.OnNavigate -> {
                onNavigate(event.route, event.behavior.toNavOptions())
            }
            is OneTimeEvents.OnLoginOrRegisterCompleted -> onLoginOrRegisterCompleted()
            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LoadingDialog(
        text = stringResource(R.string.register_loading),
        showDialog = state.isSigningUp
    ) { }


    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    RegisterScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun RegisterScreenContent(
    modifier: Modifier = Modifier,
    state: AuthScreenState,
    onEvent: (AuthScreenEvents) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val onRegister = {
        onEvent(AuthScreenEvents.OnRegisterWithEmailAndPassword(RegisterRequest(
            name = state.name,
            email = state.email,
            password = state.password
        )))
    }

    Surface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = scrollState )
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Bottom
                    )
                )
                .imePadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthHeader(
                title = stringResource(R.string.register_title),
                subtitle = stringResource(R.string.register_subtitle)
            )

//            ============= FIELDS
            Spacer(modifier = Modifier.height(32.dp))

            // Username
            AppTextField(
                value = state.name,
                onValueChange = { onEvent(AuthScreenEvents.OnChangeNameField(it)) },
                label = stringResource(R.string.register_lbl_name),
                placeholder = stringResource(R.string.register_hint_name),
                leadingIcon = Icons.Filled.Person,
                error = state.nameError,
            )


            // Username
            Spacer(modifier = Modifier.height(12.dp))
            AppTextField(
                value = state.email,
                onValueChange = { onEvent(AuthScreenEvents.OnChangeEmailField(it)) },
                label = stringResource(R.string.register_lbl_email),
                placeholder = stringResource(R.string.login_hint_email),
                leadingIcon = Icons.Filled.Email,
                error = state.emailError,
                keyboardType = KeyboardType.Email,
            )

            // Password
            Spacer(modifier = Modifier.height(12.dp))
            AppTextField(
                value = state.password,
                onValueChange = { onEvent(AuthScreenEvents.OnChangePasswordField(it)) },
                label = stringResource(R.string.register_lbl_password),
                placeholder = stringResource(R.string.register_hint_password),
                leadingIcon = Icons.Filled.Password,
                error = state.passwordError,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = onRegister,
            )

            AuthGlowButton(
                text = stringResource(R.string.register_btn_create),
                onClick = onRegister,
                modifier = Modifier.padding(top = 24.dp)
            )

            // =============== OTHER SIGN INS
            Spacer(modifier = Modifier.height(24.dp))
            SocialSignInButtons()

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.register_navigate_to_login))
                TextButton(
                    onClick = {
                        onEvent(AuthScreenEvents.OnNavigateToLogin)
                    }
                ) {
                    Text(stringResource(R.string.lbl_sign_in), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Preview
@Composable
private fun RegisterScreen() {
    DenuSpendTheme {
        RegisterScreenContent(
            state = AuthScreenState()
        )
    }
}
