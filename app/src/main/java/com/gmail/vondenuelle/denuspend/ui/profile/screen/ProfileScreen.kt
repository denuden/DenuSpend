package com.gmail.vondenuelle.denuspend.ui.profile.screen

import android.provider.ContactsContract.Profile
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun ProfileScreen(
    onNavigate : (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack : () -> Unit,
) {
    ProfileScreenContent()
}

@Composable
fun ProfileScreenContent(modifier: Modifier = Modifier) {
    Text("profile")
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    DenuSpendTheme {
        androidx.compose.material3.Surface {
            ProfileScreenContent()
        }
    }
}