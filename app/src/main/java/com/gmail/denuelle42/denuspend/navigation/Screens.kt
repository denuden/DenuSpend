package com.gmail.denuelle42.denuspend.navigation

import com.gmail.denuelle42.denuspend.data.remote.models.SampleModel
import kotlinx.serialization.Serializable

/**
 * Main Destinations for nested graph, like a web link  E.G. auth/login, auth/register. main/home. main/profile
 * article/home, article/view
 */


/**
 * Added explanation or analogy on how this works
 *
 * RootGraphs are like the first part of path segments in a url
 * E.G. sample.com/maingraph ; /maingraph is the first path and thats our rootgraphs
 *
 * Inside those rootgraphs declared in each NavGraphBuilder are the other segments or pages that is associated with that rootgraph
 *
 * sample.com/maingraph/mainscreens.homenavigation
 * sample.com/maingraph/mainscreens.favoritesnavigation
 */

/**
 * ---------------------------
 *  Root Graphs & Screens
 * ---------------------------
 *
 * Think of this like website URL paths:
 *
 *    https://sample.com/main/home
 *    https://sample.com/anime/details/1
 *
 * Each "RootGraph" represents the first segment of the path (e.g. `/main`, `/anime`).
 * Inside each RootGraph are multiple "Screens" that represent the pages or sub-paths.
 *
 * Example:
 *   RootGraphs.MainGraph      → corresponds to "/main"
 *   MainScreens.HomeNavigation → corresponds to "/main/home"
 *   MainScreens.FavoritesNavigation → "/main/favorites"
 */

/**
 * For splashscreen and simple navigation
 */
sealed interface AppRootScreens {
    @Serializable
    data object Splash : AppRootScreens

    @Serializable
    data class Main(val isLoggedIn: Boolean) : AppRootScreens
}

sealed class RootGraphs {
    @Serializable
    data object SampleGraph : RootGraphs()
    @Serializable
    data object AuthGraph : RootGraphs()
    @Serializable
    data object HomeGraph : RootGraphs()
}


/**
 *  General or shared type of all screens
 */
sealed interface NavigationScreens

sealed class SampleScreens : NavigationScreens {
    @Serializable
    data object SampleNavigation : SampleScreens()
    @Serializable
    data class SampleDetailsNavigation(val sampleModel: SampleModel) : SampleScreens()
}