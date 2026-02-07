package com.gmail.vondenuelle.denuspend.ui.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens

@Composable
fun SampleScreen(
    onPopBackStack : () -> Unit,
    onNavigate : (NavigationScreens) -> Unit
) {
    SampleScreenContent()
}

@Composable
fun SampleScreenContent(modifier: Modifier = Modifier) {

}