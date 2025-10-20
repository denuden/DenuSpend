package com.gmail.denuelle42.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.gmail.denuelle42.denuspend.data.remote.models.SampleModel
import com.gmail.denuelle42.denuspend.utils.parcelableType
import kotlin.reflect.typeOf


/**
 * Sample Navigation Graph
 */
fun NavGraphBuilder.addSampleNavGraph(
    navController: NavController
){
    navigation<RootGraphs.SampleGraph>(startDestination = SampleScreens.SampleNavigation){
        composable<SampleScreens.SampleNavigation>(typeMap = mapOf(typeOf<SampleModel>() to parcelableType<SampleModel>())) {

        }
        composable<SampleScreens.SampleDetailsNavigation> {

        }
    }
}