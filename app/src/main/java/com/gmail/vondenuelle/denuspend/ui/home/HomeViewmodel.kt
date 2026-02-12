package com.gmail.vondenuelle.denuspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.ErrorModel
import com.gmail.vondenuelle.denuspend.data.repositories.AuthRepository
import com.gmail.vondenuelle.denuspend.di.modules.TokenProvider
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
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
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class HomeViewmodel @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val repository: AuthRepository
) : ViewModel() {
    private val TAG = HomeViewmodel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<HomeScreenState>(HomeScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    fun onEvent(event: HomeScreenEvents) {
        when (event) {
            is HomeScreenEvents.OnGetCurrentUser -> sendEvent(OneTimeEvents.ShowToast(message = tokenProvider.getName()))
            is HomeScreenEvents.OnSignOut -> {
                viewModelScope.launch {
                    try {
                        repository.logout()
                        sendEvent(OneTimeEvents.OnNavigate(AuthScreens.LoginNavigation,  behavior = NavBehavior.ClearAll))
                    } catch (e : Exception){
                        onError(e)
                    }
                }
            }
            else -> Unit
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