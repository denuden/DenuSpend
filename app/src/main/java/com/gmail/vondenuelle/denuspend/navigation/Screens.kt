package com.gmail.vondenuelle.denuspend.navigation

import com.gmail.vondenuelle.denuspend.domain.models.SampleModel
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


sealed class RootGraphs {
    @Serializable
    data object SampleGraph : RootGraphs()
    @Serializable
    data object MainGraph : RootGraphs()
    @Serializable
    data object ProfileGraph : RootGraphs()
    @Serializable
    data object AddGraph : RootGraphs()
}




/**
 *  General or shared type of all screens
 */
sealed interface NavigationScreens

/**
 * Top level screens - used with different navhost
 */
sealed class AppRootScreens : NavigationScreens {
    @Serializable
    data object SplashTopLevel : AppRootScreens()
    @Serializable
    data object AuthTopLevel : AppRootScreens()
    @Serializable
    data object MainTopLevel : AppRootScreens()
}

sealed class MainTopLevelScreens : NavigationScreens {
    @Serializable
    data object MainTopLevelNavigation : MainTopLevelScreens()
}

sealed class SampleScreens : NavigationScreens {
    @Serializable
    data object SampleNavigation : SampleScreens()
    @Serializable
    data class SampleDetailsNavigation(val sampleModel: SampleModel) : SampleScreens()
}

sealed class AuthScreens : NavigationScreens {
    @Serializable
    data object AuthMainNavigation : AuthScreens()
    @Serializable
    data object LoginNavigation : AuthScreens()
    @Serializable
    data object RegisterNavigation : AuthScreens()
}

sealed class MainScreens : NavigationScreens {
    @Serializable
    data object HomeNavigation : MainScreens()
    @Serializable
    data object AddNavigation : MainScreens()
    @Serializable
    data object BudgetNavigation : MainScreens()
}

sealed class ProfileScreens : NavigationScreens {
    @Serializable
    data object ProfileNavigation : ProfileScreens()
}

sealed class AddScreens : NavigationScreens {
    @Serializable
    data object AddIncomeScreenNavigation : AddScreens()
    @Serializable
    data object AddExpenseScreenNavigation : AddScreens()
    @Serializable
    data object AllRecentTransactionsNavigation : AddScreens()
}

