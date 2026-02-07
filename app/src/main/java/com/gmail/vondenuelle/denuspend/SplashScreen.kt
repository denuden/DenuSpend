package com.gmail.vondenuelle.denuspend

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.gmail.vondenuelle.denuspend.ui.sample.SampleViewModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: (isLoggedIn: Boolean) -> Unit,
    viewModel: SampleViewModel = hiltViewModel()
) {

    ObserveAsEvents(viewModel.channel) {
        when (it) {
            is OneTimeEvents.OnNavigate -> {
                //if success
//                if(it.route == AuthScreens.SplashNavigation){
                //TODO
//                    onFinished(true)
//                } else { //if unauthorized
//                    onFinished(false)
//                }
            }

            else -> Unit
        }
    }

    val scope = rememberCoroutineScope()
    ComposableLifecycle { source, event ->
        if (event == Lifecycle.Event.ON_START) {
            scope.launch {
                delay(1000)
                onFinished(false)

//                viewModel.onEvent(AuthScreenEvents.OnRefreshToken)
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