package com.gmail.denuelle42.denuspend.ui.sample

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.denuelle42.denuspend.data.repositories.sample.request.GetRequest
import com.gmail.denuelle42.denuspend.domain.repositories.sample.SampleUseCase
import com.gmail.denuelle42.denuspend.utils.OneTimeEvents
import com.gmail.denuelle42.denuspend.utils.ResultState
import com.gmail.denuelle42.denuspend.utils.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private fun sendEvent(event: OneTimeEvents) {
        viewModelScope.launch {
            _channel.send(event)
        }
    }
}