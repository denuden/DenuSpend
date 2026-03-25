package com.gmail.vondenuelle.denuspend.ui.home.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.home.HomeScreenEvents
import com.gmail.vondenuelle.denuspend.ui.home.HomeViewmodel
import com.gmail.vondenuelle.denuspend.ui.home.components.section.MonthlySpendingSection
import com.gmail.vondenuelle.denuspend.ui.home.components.section.QuickTabSection
import com.gmail.vondenuelle.denuspend.ui.home.components.section.BudgetSection
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
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

//    ComposableLifecycle { owner, event ->
//       if (event == Lifecycle.Event.ON_START){
//           viewModel.onEvent(HomeScreenEvents.OnGetCurrentUser)
//       }
//    }

    HomeScreenContent(
        onEvent = viewModel::onEvent
    )
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    onEvent : (HomeScreenEvents) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier

    ) {

        Column {
            MonthlySpendingSection(
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            BudgetSection()
            QuickTabSection(
                modifier = Modifier.fillMaxWidth()
            ) {
                //todo
            }

        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    DenuSpendTheme {
        Surface {
            HomeScreenContent(
                modifier = Modifier.fillMaxSize()
            ){}
        }
    }
}