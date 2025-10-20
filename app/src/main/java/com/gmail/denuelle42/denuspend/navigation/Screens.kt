package com.gmail.denuelle42.denuspend.navigation

import com.gmail.denuelle42.denuspend.data.remote.models.SampleModel
import kotlinx.serialization.Serializable

/**
 * Main Destinations for nested graph, like a web link  E.G. auth/login, auth/register. main/home. main/profile
 * article/home, article/view
 */
sealed class RootGraphs {
    @Serializable
    data object SampleGraph : RootGraphs()
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