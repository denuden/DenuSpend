package com.gmail.denuelle42.denuspend.ui.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gmail.denuelle42.denuspend.navigation.NavigationScreens

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