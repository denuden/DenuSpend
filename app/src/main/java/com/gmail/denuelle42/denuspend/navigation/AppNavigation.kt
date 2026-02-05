package com.gmail.denuelle42.denuspend.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun AppNavigation(navController: NavHostController, startDestination: RootGraphs) {
   NavHost(navController = navController, startDestination = startDestination){
      addSampleNavGraph(navController)
   }
}


/**
 * Gets proper top app bar title
 * based on current nvigation
 */
fun getTopBarTitle(currentRoute : String) : String{
   //Get route name as package  and get the last one the get the class name
   val route = currentRoute.substringAfterLast(".")

   //check if route has "/" means it has arguments, remove it so we can verify route itself
   val cleaned = if(route.contains("/")) route.substringBeforeLast("/") else route

   //check route with specific route under items in sealed class NavigationScreens (not RootGraphs)
   return when(cleaned){
      "PeopleNavigation" -> "Search People"
      "PeopleDetailsNavigation" -> "Details"
      else -> "DenuAnime"
   }
}