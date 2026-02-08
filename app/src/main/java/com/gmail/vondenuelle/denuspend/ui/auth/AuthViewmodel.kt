package com.gmail.vondenuelle.denuspend.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.ErrorModel
import com.gmail.vondenuelle.denuspend.domain.repositories.auth.AuthUseCase
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.network.ResultState
import com.gmail.vondenuelle.denuspend.utils.network.asResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AuthViewmodel @Inject constructor(
    private val authUseCase: AuthUseCase
) : ViewModel() {
    private val TAG = AuthViewmodel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<AuthScreenState>(AuthScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    fun onEvent(event: AuthScreenEvents) {
        when (event) {
            is AuthScreenEvents.OnLogin -> {
                viewModelScope.launch {
                    authUseCase.login(event.request).asResult().onEach { res ->
                        when(res){
                            ResultState.Completed -> {
                                _stateFlow.update { it.copy(isSigningIn = false) }
                            }
                            is ResultState.Error -> onError(res.exception)
                            ResultState.Loading -> {
                                _stateFlow.update { it.copy(isSigningIn = true) }
                            }
                            is ResultState.Success -> {
                                _stateFlow.update { it.copy(userModel = res.data) }
                                sendEvent(OneTimeEvents.ShowToast(message = res.data.email.orEmpty()))
                            }
                        }
                    }.collect()
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
            is AuthScreenEvents.OnNavigateToLogin ->
                sendEvent(OneTimeEvents.OnNavigate(AuthScreens.LoginNavigation))
            is AuthScreenEvents.OnNavigateToRegister ->
                sendEvent(OneTimeEvents.OnNavigate(AuthScreens.RegisterNavigation))
        }
    }

    private fun onError(e: Throwable?) {
        when (e) {
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