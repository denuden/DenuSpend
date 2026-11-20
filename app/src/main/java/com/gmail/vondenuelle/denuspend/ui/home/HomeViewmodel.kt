package com.gmail.vondenuelle.denuspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.repositories.AuthRepository
import com.gmail.vondenuelle.denuspend.di.modules.TokenProvider
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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

            else -> Unit
        }
    }

    private fun onError(e: Throwable?) {
        sendEvent(OneTimeEvents.ShowError(e?.message.orEmpty()))
    }

    private fun sendEvent(event: OneTimeEvents) {
        viewModelScope.launch {
            _channel.send(event)
        }
    }
}