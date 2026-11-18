package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavOptions

sealed class NavBehavior {
    object None : NavBehavior()
    data class PopUpTo(
        val destination: NavigationScreens,
        val inclusive: Boolean = true
    ) : NavBehavior()
}

// Turns the behavior a ViewModel asked for into NavOptions, so screens don't each build it by hand.
fun NavBehavior.toNavOptions(): NavOptions? = when (this) {
    NavBehavior.None -> null
    is NavBehavior.PopUpTo -> NavOptions.Builder()
        .setPopUpTo(destination, inclusive = inclusive)
        .build()
}
