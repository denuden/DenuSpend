package com.gmail.vondenuelle.denuspend.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotLogoutException
import com.gmail.vondenuelle.denuspend.data.remote.error.ErrorModel
import com.gmail.vondenuelle.denuspend.data.remote.error.InvalidCredentialsException
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.repositories.AuthRepository
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.domain.usecase.auth.RegisterUseCase
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
import com.gmail.vondenuelle.denuspend.navigation.MainScreens
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {
    private val TAG = AuthViewModel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<AuthScreenState>(AuthScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    fun onEvent(event: AuthScreenEvents) {
        when (event) {
            is AuthScreenEvents.OnLoginWithEmailAndPassword -> {
                viewModelScope.launch {
                    _stateFlow.update { it.copy(isSigningIn = true) }

                    try {
                        val user = repository.login(event.request)
                        Log.e("inigewgt", user.toString())
                        if(user.email != null) {
                            _stateFlow.update { it.copy(userModel = user) }
                            sendEvent(OneTimeEvents.ShowToast(message = user.email))
                            sendEvent(OneTimeEvents.OnNavigate(MainScreens.HomeNavigation,  behavior = NavBehavior.ClearAll))
                        }else{
                            sendEvent(OneTimeEvents.ShowError("User is not existing"))
                        }
                        _stateFlow.update { it.copy(isSigningIn = false) }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy( isSigningIn = false) }
                        onError(e)
                    }
                }
            }

            is AuthScreenEvents.OnRegisterWithEmailAndPassword -> {
                viewModelScope.launch {
                    _stateFlow.update { it.copy(isSigningUp = true) }

                    try {
                        val user = registerUseCase.invoke(event.request)
                        if(user.email != null) {
                            _stateFlow.update { it.copy(userModel = user) }
                            sendEvent(OneTimeEvents.ShowToast(message = user.email))
                            sendEvent(OneTimeEvents.OnNavigate(MainScreens.HomeNavigation,  behavior = NavBehavior.ClearAll))
                        }else{
                            sendEvent(OneTimeEvents.ShowError("User is not existing"))
                        }
                        _stateFlow.update { it.copy(isSigningUp = false) }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy( isSigningUp = false) }
                        onError(e)
                    }
                }
            }
            is AuthScreenEvents.OnGetCurrentUser -> {
                viewModelScope.launch {
                    _stateFlow.update { it.copy(isSigningIn = true) }

                    try {
                        val user = repository.getCurrentUser()

                        _stateFlow.update { it.copy(userModel = user, isSigningIn = false) }
                        //if user is existing, just send SplashNavigation for placeholder
                        sendEvent(OneTimeEvents.OnNavigate(AuthScreens.SplashNavigation))
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy( isSigningIn = false) }
                        onError(e)
                    }
                }
            }
            is AuthScreenEvents.OnChangeEmailField ->
                _stateFlow.update { it.copy(email = event.value) }
            is AuthScreenEvents.OnChangePasswordField ->
                _stateFlow.update { it.copy(password = event.value) }
            is AuthScreenEvents.OnChangeNameField ->
                _stateFlow.update { it.copy(name = event.value) }
            is AuthScreenEvents.OnChangeRememberMeCheckBox ->
                _stateFlow.update { it.copy(shouldRememberMe = event.value) }
            is AuthScreenEvents.OnChangePasswordVisibility ->
                _stateFlow.update { it.copy(showPassword = event.value) }
            is AuthScreenEvents.OnNavigateToLogin ->
                sendEvent(OneTimeEvents.OnNavigate(AuthScreens.LoginNavigation,  behavior = NavBehavior.PopUpTo(AuthScreens.RegisterNavigation, inclusive =  true)))
            is AuthScreenEvents.OnNavigateToRegister ->
                sendEvent(OneTimeEvents.OnNavigate(AuthScreens.RegisterNavigation,  behavior = NavBehavior.PopUpTo(AuthScreens.LoginNavigation, inclusive =  true)))

            is AuthScreenEvents.OnSendPasswordReset -> {
                viewModelScope.launch {
                    _stateFlow.update { it.copy(isLoading = true) }

                    try {
                        repository.sendPasswordReset(event.value)
                        _stateFlow.update { it.copy(isLoading = false, showForgotPasswordDialog = false) }
                        sendEvent(OneTimeEvents.ShowToast("Password reset has been sent"))
                    } catch (e: Exception) {
                        Log.e("gewgwe", e.toString())
                        _stateFlow.update { it.copy( isLoading = false) }
                        onError(e)
                    }
                }
            }
            is AuthScreenEvents.OnOpenForgotPassDialog -> {
                _stateFlow.update { it.copy(showForgotPasswordDialog = event.value) }
            }
            is AuthScreenEvents.OnChangeForgotPassEmailField -> {
                _stateFlow.update { it.copy(forgotPassEmail = event.value) }
            }
        }
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
            is HttpException -> {
                val statusCode = e.code()
                val errorBody = e.response()?.errorBody()
                val gson = Gson()
                val type = object : TypeToken<ErrorModel>() {}.type
                val errorResponse: ErrorModel? = gson.fromJson(errorBody?.charStream(), type)

                // Example: Handle specific status
                when(statusCode) {
                    401 -> {
//                        sendEvent(OneTimeEvents.OnNavigate(AuthScreens.LoginNavigation))
                        return
                    }
                }

                //if this is not null, then there is a message regarding bad request of params
                if (errorResponse?.errors != null) {
                    _stateFlow.update {
                        it.copy(
                        )
                    }
                    sendEvent(OneTimeEvents.ShowInputError(errorResponse.errors))
                } else if (errorResponse?.message != null) {
                    sendEvent(OneTimeEvents.ShowError(errorResponse.message))
                }
            }

            is IllegalArgumentException -> {
                sendEvent(OneTimeEvents.ShowError("Input fields are empty or invalid. Please make sure your inputs are correct"))
            }
            // User defined custom Exceptions
            is NoUserException -> {
                //send to login
                //remove stored creds
                sendEvent(OneTimeEvents.ShowToast("No user is signed in"))
                logout()
                sendEvent(OneTimeEvents.OnNavigate(AuthScreens.LoginNavigation, behavior = NavBehavior.ClearAll))
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