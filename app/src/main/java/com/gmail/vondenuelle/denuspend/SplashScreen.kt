package com.gmail.vondenuelle.denuspend

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.auth.AuthScreenEvents
import com.gmail.vondenuelle.denuspend.ui.auth.AuthViewModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.ShowError -> Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()

            is OneTimeEvents.OnNavigate -> {
                val options = NavOptions.Builder().apply {
                    when (event.behavior) {
                        NavBehavior.ClearAll -> {
                            setPopUpTo(0, inclusive = true)
                            setLaunchSingleTop(true)
                        }

                        is NavBehavior.PopUpTo -> {
                            setPopUpTo(
                                event.behavior.destination,
                                inclusive = event.behavior.inclusive
                            )
                        }
                        NavBehavior.None -> Unit
                    }
                }.build()

                onNavigate(event.route, options)
            }

            else -> Unit
        }
    }
    val scope = rememberCoroutineScope()
    ComposableLifecycle { source, event ->
        if (event == Lifecycle.Event.ON_START) {
            scope.launch {
                delay(1000)
                viewModel.onEvent(AuthScreenEvents.OnGetCurrentUser)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "SPLASHSCREEN",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.align(
                Alignment.Center
            )
        )
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    DenuSpendTheme {
        Surface(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .fillMaxSize()
        ) {
        }
    }
}