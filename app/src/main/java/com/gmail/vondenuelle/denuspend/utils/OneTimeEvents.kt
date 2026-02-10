package com.gmail.vondenuelle.denuspend.utils

import com.gmail.vondenuelle.denuspend.data.remote.error.ErrorData
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens

/**
 * One time events that are only triggered once, to be used in Channel
 */
sealed class OneTimeEvents {
    data class OnNavigate(val route : NavigationScreens,  val behavior: NavBehavior = NavBehavior.None) : OneTimeEvents()
    object OnPopBackStack : OneTimeEvents()
    data class ShowSnackbar(val snackbarEvent: SnackbarEvent)  : OneTimeEvents()
    data class ShowToast(val message : String)  : OneTimeEvents()
    data class ShowInputError(val errors : ErrorData)  : OneTimeEvents()
    data class ShowError(val msg : String)  : OneTimeEvents()
}