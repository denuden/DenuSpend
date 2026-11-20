package com.gmail.vondenuelle.denuspend.ui.auth

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotLogoutException
import com.gmail.vondenuelle.denuspend.data.remote.error.InvalidCredentialsException
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.repositories.AuthRepository
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.domain.usecase.auth.RegisterUseCase
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val registerUseCase: RegisterUseCase,
    // Application context (not an Activity), only used to read strings.xml, so it can't leak a screen
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val TAG = AuthViewModel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    // Pre-fills Forgot password with the email typed on Login. Type-safe routes store args by property name,
    // so this reads ForgotPasswordNavigation.email; Login/Register have no "email" arg, so it's "" there.
    private val _stateFlow = MutableStateFlow(
        AuthScreenState(forgotPassEmail = savedStateHandle.get<String>("email").orEmpty())
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun onEvent(event: AuthScreenEvents) {
        when (event) {
            is AuthScreenEvents.OnLoginWithEmailAndPassword -> {
                val emailError = emailError(event.request.email)
                val passwordError = passwordError(event.request.password, checkLength = false)
                _stateFlow.update { it.copy(emailError = emailError, passwordError = passwordError) }
                if (emailError.isNotEmpty() || passwordError.isNotEmpty()) return

                viewModelScope.launch {
                    _stateFlow.update { it.copy(isSigningIn = true) }

                    try {
                        val user = repository.login(event.request)
                        if(user.email != null) {
                            // Creates the users/{uid} doc if it's missing (older accounts) and refreshes name/email
                            profileRepository.syncUserDoc()
                            _stateFlow.update { it.copy(userModel = user) }
                            sendEvent(OneTimeEvents.ShowToast(context.getString(R.string.login_toast_welcome, user.name.ifBlank { user.email })))
                            sendEvent(OneTimeEvents.OnLoginOrRegisterCompleted)
                        }else{
                            sendEvent(OneTimeEvents.ShowError(context.getString(R.string.error_user_not_existing)))
                        }
                        _stateFlow.update { it.copy(isSigningIn = false) }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy( isSigningIn = false) }
                        onError(e)
                    }
                }
            }

            is AuthScreenEvents.OnRegisterWithEmailAndPassword -> {
                val nameError = if (event.request.name.isBlank()) context.getString(R.string.error_name_required) else ""
                val emailError = emailError(event.request.email)
                val passwordError = passwordError(event.request.password, checkLength = true)
                _stateFlow.update { it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError) }
                if (nameError.isNotEmpty() || emailError.isNotEmpty() || passwordError.isNotEmpty()) return

                viewModelScope.launch {
                    _stateFlow.update { it.copy(isSigningUp = true) }

                    try {
                        val user = registerUseCase.invoke(event.request)
                        if(user.email != null) {
                            _stateFlow.update { it.copy(userModel = user) }
                            sendEvent(OneTimeEvents.ShowToast(context.getString(R.string.register_toast_created, user.name.ifBlank { user.email })))
                            sendEvent(OneTimeEvents.OnLoginOrRegisterCompleted)
                        }else{
                            sendEvent(OneTimeEvents.ShowError(context.getString(R.string.error_user_not_existing)))
                        }
                        _stateFlow.update { it.copy(isSigningUp = false) }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy( isSigningUp = false) }
                        onError(e)
                    }
                }
            }
            is AuthScreenEvents.OnChangeEmailField ->
                _stateFlow.update { it.copy(email = event.value, emailError = "") }
            is AuthScreenEvents.OnChangePasswordField ->
                _stateFlow.update { it.copy(password = event.value, passwordError = "") }
            is AuthScreenEvents.OnChangeNameField ->
                _stateFlow.update { it.copy(name = event.value, nameError = "") }
            is AuthScreenEvents.OnNavigateToLogin ->
                sendEvent(OneTimeEvents.OnNavigate(AuthScreens.LoginNavigation,  behavior = NavBehavior.PopUpTo(AuthScreens.RegisterNavigation, inclusive =  true)))
            is AuthScreenEvents.OnNavigateToRegister ->
                sendEvent(OneTimeEvents.OnNavigate(AuthScreens.RegisterNavigation,  behavior = NavBehavior.PopUpTo(AuthScreens.LoginNavigation, inclusive =  true)))
            // Pushed on top of Login (no PopUpTo), so Back returns to Login with its fields intact
            is AuthScreenEvents.OnNavigateToForgotPassword ->
                sendEvent(OneTimeEvents.OnNavigate(AuthScreens.ForgotPasswordNavigation(_stateFlow.value.email)))
            is AuthScreenEvents.OnNavigateBack ->
                sendEvent(OneTimeEvents.OnPopBackStack)

            is AuthScreenEvents.OnSendPasswordReset -> {
                val emailError = emailError(event.value.email)
                _stateFlow.update { it.copy(forgotPassEmailError = emailError) }
                if (emailError.isNotEmpty()) return

                viewModelScope.launch {
                    _stateFlow.update { it.copy(isLoading = true) }

                    try {
                        repository.sendPasswordReset(event.value)
                        _stateFlow.update { it.copy(isLoading = false, isPasswordResetSent = true) }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy( isLoading = false) }
                        onError(e)
                    }
                }
            }
            is AuthScreenEvents.OnChangeForgotPassEmailField -> {
                _stateFlow.update { it.copy(forgotPassEmail = event.value, forgotPassEmailError = "") }
            }
        }
    }

    // Field checks done before calling Firebase. Each returns the message to show under the field, or "" when valid.
    private fun emailError(email: String): String = when {
        email.isBlank() -> context.getString(R.string.error_email_required)
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> context.getString(R.string.error_email_invalid)
        else -> ""
    }

    // checkLength is only for Register: Firebase requires 6+ characters, but existing logins shouldn't be blocked by it
    private fun passwordError(password: String, checkLength: Boolean): String = when {
        password.isBlank() -> context.getString(R.string.error_password_required)
        checkLength && password.trim().length < 6 -> context.getString(R.string.error_password_short)
        else -> ""
    }

    private fun logout(){
        viewModelScope.launch {
            try {
                profileRepository.logout()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    private fun onError(e: Throwable?) {
        when (e) {
            //Top level exceptions (e.g retrofit)
            is IOException -> {
                sendEvent(OneTimeEvents.ShowError(e.message.orEmpty()))
            }
            is IllegalArgumentException -> {
                sendEvent(OneTimeEvents.ShowError(context.getString(R.string.error_invalid_inputs)))
            }
            // User defined custom Exceptions
            is NoUserException -> {
                // Already on the auth screens, just make sure nothing is left signed in
                sendEvent(OneTimeEvents.ShowToast(context.getString(R.string.error_no_user_signed_in)))
                logout()
            }
            is InvalidCredentialsException -> {
                sendEvent(OneTimeEvents.ShowError(e.message.orEmpty()))
            }
            is CannotLogoutException -> {
                sendEvent(OneTimeEvents.ShowError(e.message.orEmpty()))
            }
            else -> {
                sendEvent(OneTimeEvents.ShowError(e?.message.orEmpty()))
            }
        }
    }

    private fun sendEvent(event: OneTimeEvents) {
        viewModelScope.launch {
            _channel.send(event)
        }
    }
}