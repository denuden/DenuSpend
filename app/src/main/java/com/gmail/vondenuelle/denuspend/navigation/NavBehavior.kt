package com.gmail.vondenuelle.denuspend.navigation

sealed class NavBehavior {
    object None : NavBehavior()
    object ClearAll : NavBehavior()
    data class PopUpTo(
        val destination: NavigationScreens,
        val inclusive: Boolean = true
    ) : NavBehavior()
}
