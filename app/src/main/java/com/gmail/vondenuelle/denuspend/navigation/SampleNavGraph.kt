package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.gmail.vondenuelle.denuspend.domain.models.SampleModel
import com.gmail.vondenuelle.denuspend.utils.parcelableType
import kotlin.reflect.typeOf


/**
 * Sample Navigation Graph
 */
fun NavGraphBuilder.addSampleNavGraph(
    navController: NavController
){

    //**==============NOTE
    /**
     * ADD AN INITIAL VALUE TO ARGUMENTS TO ROUTE WITH ARGUMENTS WHEN PASSING IT AS A START DESTINATION
     * See more at addAnimeNavGraph() file
     * Sample **
     *  navigation<RootGraphs.AnimeGraph>(startDestination = AnimeScreens.AnimeDetailsNavigation(id = 0)){}
     */
    navigation<RootGraphs.SampleGraph>(startDestination = SampleScreens.SampleDetailsNavigation(SampleModel(0, "name"))){

//        Old-school NavType / parcelableType approach
        composable<SampleScreens.SampleDetailsNavigation>(typeMap = mapOf(typeOf<SampleModel>() to parcelableType<SampleModel>())) {
            val args = it.toRoute<SampleScreens.SampleDetailsNavigation>()
            val model = args.sampleModel

            //            HomeScreen(
//                onPopBackStack = { navController.popBackStack() },
//                onNavigate = { navController.navigate(it) }
//            )
        }

        //Type-Safe Navigation (new Compose API)
//        composable<SampleScreens.SampleDetailsNavigation> {
//            val args = it.toRoute<SampleScreens.SampleDetailsNavigation>()
//            val model = args.sampleModel
//        }
        composable<SampleScreens.SampleNavigation> {

        }
    }
}