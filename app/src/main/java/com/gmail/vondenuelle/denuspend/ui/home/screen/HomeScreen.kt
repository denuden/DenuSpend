package com.gmail.vondenuelle.denuspend.ui.home.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.home.HomeScreenEvents
import com.gmail.vondenuelle.denuspend.ui.home.HomeViewmodel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents

@Composable
fun HomeScreen(
    onNavigate : (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack : () -> Unit,
    viewModel : HomeViewmodel = hiltViewModel()
) {
    val context = LocalContext.current

    ObserveAsEvents(viewModel.channel) { event ->
        when(event){
            is OneTimeEvents.OnNavigate -> {
                val options = NavOptions.Builder().apply {
                    when (event.behavior) {
                        NavBehavior.ClearAll -> {
                            setPopUpTo(0, inclusive = true)
                            setLaunchSingleTop(true)
                        }
                        is NavBehavior.PopUpTo -> {
                            setPopUpTo(event.behavior.destination, inclusive = event.behavior.inclusive)
                        }
                        NavBehavior.None -> Unit
                    }
                }.build()

                onNavigate(event.route, options)
            }
            is OneTimeEvents.ShowError -> Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

    ComposableLifecycle { owner, event ->
       if (event == Lifecycle.Event.ON_START){
           viewModel.onEvent(HomeScreenEvents.OnGetCurrentUser)
       }
    }

    HomeScreenContent(
        onEvent = viewModel::onEvent
    )
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    onEvent : (HomeScreenEvents) -> Unit
) {
    Surface {
        Box(
            contentAlignment = Alignment.Center
        ){
            Button(
                onClick ={
                    onEvent(HomeScreenEvents.OnSignOut)
                }
            ) {
                Text("Sign out")
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    DenuSpendTheme {
        Surface {
            HomeScreenContent(){}
        }
    }
}