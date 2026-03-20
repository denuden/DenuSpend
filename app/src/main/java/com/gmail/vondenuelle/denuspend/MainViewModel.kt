package com.gmail.vondenuelle.denuspend

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.gmail.vondenuelle.denuspend.di.modules.TokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenProvider: TokenProvider
) : ViewModel() {

    private val _photoState = mutableStateOf("")
    val photoState: State<String> = _photoState

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
            "ProfileNavigation" -> "Profile"
            "AddNavigation" -> "Transaction"
            "AddIncomeScreenNavigation" -> "Add Income"
            "AddExpenseScreenNavigation" -> "Add Expense"
            "AllRecentTransactionsNavigation" -> "History"
            "HomeNavigation" ->  "Welcome back, ${tokenProvider.getName().split(" ")[0]}"
            else -> "DenuSpend"
        }
    }

    fun getPhoto(){
        _photoState.value = tokenProvider.getPhoto()
    }

    fun resetPhoto(){
        _photoState.value = ""
    }
}