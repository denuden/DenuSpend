package com.gmail.denuelle42.denuspend.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

data class SnackbarEvent(
    val message : String,
    val action : SnackbarAction? = null,
)

data class SnackbarAction(
    val name : String,
    val action : () -> Unit
)

/**
 * Making Snackback Controller that can be called from anywhere to trigger
 * snackbar events
 */
object SnackBarController {
    private val _events = Channel<SnackbarEvent>()
    val events = _events.receiveAsFlow()

    suspend fun sendEvent(event : SnackbarEvent){
        _events.send(event)
    }
}