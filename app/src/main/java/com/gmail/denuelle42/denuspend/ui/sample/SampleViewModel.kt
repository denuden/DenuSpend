package com.gmail.denuelle42.denuspend.ui.sample

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.denuelle42.denuspend.data.remote.error.ErrorModel
import com.gmail.denuelle42.denuspend.data.repositories.sample.request.GetRequest
import com.gmail.denuelle42.denuspend.domain.repositories.sample.SampleUseCase
import com.gmail.denuelle42.denuspend.utils.OneTimeEvents
import com.gmail.denuelle42.denuspend.utils.network.ResultState
import com.gmail.denuelle42.denuspend.utils.network.asResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class SampleViewModel @Inject constructor(
    private val sampleUseCase: SampleUseCase
): ViewModel(){
    private val TAG = SampleViewModel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<SampleScreenState>(SampleScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    fun onEvent(event : SampleScreenEvents) {
        when(event){
            is SampleScreenEvents.OnGetEvent -> {
                viewModelScope.launch {
                    sampleUseCase.getRequest(GetRequest()).asResult().onEach { res ->
                        when(res) {
                            ResultState.Completed -> _stateFlow.update { it.copy(isLoading = false) }
                            is ResultState.Error -> Log.e(TAG, res.exception.toString())
                            ResultState.Loading -> _stateFlow.update { it.copy(isLoading = true) }
                            is ResultState.Success ->  _stateFlow.update {
                                it.copy(name = event.name)
                            }
                        }
                    }

                }
            }
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
        }
    }

    private fun sendEvent(event: OneTimeEvents) {
        viewModelScope.launch {
            _channel.send(event)
        }
    }
}