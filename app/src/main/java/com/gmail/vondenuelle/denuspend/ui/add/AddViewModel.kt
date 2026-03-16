package com.gmail.vondenuelle.denuspend.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.ErrorModel
import com.gmail.vondenuelle.denuspend.data.repositories.SampleRepository
import com.gmail.vondenuelle.denuspend.navigation.AddScreens
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.*
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
class AddViewModel @Inject constructor(
    private val sampleRepository: SampleRepository
): ViewModel(){
    private val TAG = AddViewModel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<AddScreenState>(AddScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    fun onEvent(event : AddScreenEvents) {
        when(event){
            is AddScreenEvents.OnNavigateToIncomeScreen -> {
                sendEvent(OnNavigate(AddScreens.AddIncomeScreenNavigation))
            }
            is AddScreenEvents.OnNavigateToExpenseScreen -> {
                sendEvent(OnNavigate(AddScreens.AddExpenseScreenNavigation))
            }

            is AddScreenEvents.OnIncomeAmountChanged -> _stateFlow.update { it.copy(transactionAmount = event.value) }
            is AddScreenEvents.OnIncomeDescriptionChanged -> _stateFlow.update { it.copy(transactionDescription = event.value) }
            is AddScreenEvents.OnIncomeTitleChanged -> _stateFlow.update { it.copy(transactionTitle = event.value) }
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