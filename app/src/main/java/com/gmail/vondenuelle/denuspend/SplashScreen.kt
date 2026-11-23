package com.gmail.vondenuelle.denuspend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

// AppRoot's start destination while MainViewModel checks the current user. The system splash
// (MainActivity, Theme.DenuSpend.Starting) covers it the whole time, so it's left blank on purpose.
@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize())
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
            SplashScreen()
        }
    }
}
