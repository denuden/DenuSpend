package com.gmail.vondenuelle.denuspend.ui.profile.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.profile.ProfileScreenEvents
import com.gmail.vondenuelle.denuspend.ui.profile.ProfileScreenState
import com.gmail.vondenuelle.denuspend.ui.profile.ProfileViewModel
import com.gmail.vondenuelle.denuspend.ui.profile.component.ProfileEdit
import com.gmail.vondenuelle.denuspend.ui.profile.component.ProfileHeader
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents

@Composable
fun ProfileScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var error by remember { mutableStateOf("") }

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> {
                onPopBackStack()
            }

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

            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT)
                .show()

            else -> Unit
        }
    }

    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            viewModel.onEvent(ProfileScreenEvents.OnGetUserProfile)
        }
    }
    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }


    ProfileScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    state: ProfileScreenState,
    onEvent: (ProfileScreenEvents) -> Unit
) {

    var editDialog by remember { mutableStateOf(false) }

    ModalBottomSheetDialog(
        showDialog = editDialog,
        onDismissRequest = { editDialog = false }
    ) {
        ProfileEdit(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            name = state.name,
            nameError = state.nameError,
            onChangeName = {
                onEvent(ProfileScreenEvents.OnChangeName(it))
            },
            photo = state.photo,
            photoError = state.photoError,
            onChangePhoto = {
                //TODO add picker dialog for photo or gallery
                //then call onevent to change and trigger state
//                onEvent(ProfileScreenEvents.OnChangePhoto(it))
            },
            onSave = {
                onEvent(ProfileScreenEvents.OnSaveChanges)
            }
        )
    }


    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            ProfileHeader(
                onEdit = { editDialog = true },
                onPopBackStack = {
                    onEvent(ProfileScreenEvents.OnPopBackStack)
                }
            )

            Text(
                text = state.profile?.name.orEmpty().ifEmpty { "Hello, User" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = state.profile?.email.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            val isEmailVerified = state.profile?.isEmailVerified ?: false
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
                    .background(
                        color = if (isEmailVerified) colorResource(R.color.verified_green) else colorResource(
                            R.color.not_verified_red
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = if (isEmailVerified) "Email Verified" else "Not yet verified",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                if (isEmailVerified) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        tint = Color.White,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    DenuSpendTheme {
        androidx.compose.material3.Surface {
            ProfileScreenContent(state = ProfileScreenState()) {}
        }
    }
}