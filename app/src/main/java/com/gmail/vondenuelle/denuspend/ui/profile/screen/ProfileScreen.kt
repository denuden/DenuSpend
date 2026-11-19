package com.gmail.vondenuelle.denuspend.ui.profile.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.common.dialog.CustomAlertDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.profile.ProfileScreenEvents
import com.gmail.vondenuelle.denuspend.ui.profile.ProfileScreenState
import com.gmail.vondenuelle.denuspend.ui.profile.ProfileViewModel
import com.gmail.vondenuelle.denuspend.ui.profile.component.DeleteAccountSheet
import com.gmail.vondenuelle.denuspend.ui.profile.component.ProfileButtons
import com.gmail.vondenuelle.denuspend.ui.profile.component.ProfileEdit
import com.gmail.vondenuelle.denuspend.ui.profile.component.ProfileHeader
import com.gmail.vondenuelle.denuspend.ui.profile.component.ProfileReauthenticateUser
import com.gmail.vondenuelle.denuspend.ui.profile.component.ProfileUpdateEmail
import com.gmail.vondenuelle.denuspend.ui.profile.component.ProfileUpdatePassword
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import com.gmail.vondenuelle.denuspend.utils.media.MediaPickerViewModel
import com.gmail.vondenuelle.denuspend.utils.media.OptionDialogPicker
import com.gmail.vondenuelle.denuspend.utils.media.SelectedOption
import com.gmail.vondenuelle.denuspend.utils.media.rememberMediaPickerHelper
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    mediaPickerViewModel: MediaPickerViewModel = hiltViewModel()
) {

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> {
                onPopBackStack()
            }

            is OneTimeEvents.OnNavigate -> {
                onNavigate(event.route, event.behavior.toNavOptions())
            }

            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT)
                .show()

            is OneTimeEvents.ShowSnackbar -> {
                scope.launch {
                    SnackBarController.sendEvent(event.snackbarEvent)
                }
            }

            OneTimeEvents.OnCloseDialog -> {}
            OneTimeEvents.OnLoginOrRegisterCompleted -> {}
            is OneTimeEvents.ShowInputError -> {}
        }
    }

    //Media Picker
    ObserveAsEvents(mediaPickerViewModel.uriStateFlow) {
        it.singleFile?.let { uri ->
            //save to gallery after initial temporary save
            mediaPickerViewModel.saveToGallery(uri)
        }
        it.savedToGalleryUri?.let { uri ->
            viewModel.onEvent(ProfileScreenEvents.OnChangePhoto(uri.toString()))
        }
    }

    val mediaPickerHelper = rememberMediaPickerHelper(mediaPickerViewModel)

    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            viewModel.onEvent(ProfileScreenEvents.OnGetUserProfile)
        }
    }

    LoadingDialog(
        showDialog = state.isLoading,
        text = stringResource(R.string.lbl_loading),
    ) { }
    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    OptionDialogPicker(
        showDialog = state.showMediaOptionDialog,
        onDismiss = {
            viewModel.onEvent(ProfileScreenEvents.OnShowMediaOptionDialog(false))
        },
        openCamera = {
            mediaPickerHelper.checkPermissionsAndOpenCamera()
        },
        openPhotoPicker = {
            mediaPickerHelper.checkPermissionsAndOpenPhotoPicker()
        }
    )


    ModalBottomSheetDialog(
        showDialog = state.showEditDialog,
        onDismissRequest = {
            viewModel.onEvent(ProfileScreenEvents.OnShowEditDialog(false))
        }
    ) {
        ProfileEdit(
            name = state.name,
            nameError = state.nameError,
            onChangeName = {
                viewModel.onEvent(ProfileScreenEvents.OnChangeName(it))
            },
            photo = state.photo,
            onChangePhoto = {
                mediaPickerViewModel.setSelectedOption(SelectedOption.SELECTED_SINGLE_FILE)
                viewModel.onEvent(ProfileScreenEvents.OnShowMediaOptionDialog(true))
            },
            onSave = {
                viewModel.onEvent(ProfileScreenEvents.OnSaveProfileChanges)
            }
        )
    }

    ModalBottomSheetDialog(
        showDialog = state.showUpdatePasswordDialog,
        onDismissRequest = {
            viewModel.onEvent(ProfileScreenEvents.OnShowUpdatePasswordDialog(false))
        }
    ) {
        ProfileUpdatePassword(
            newPassword = state.newPassword,
            newPasswordError = state.newPasswordError,
            onChangeNewPassword = { viewModel.onEvent(ProfileScreenEvents.OnChangeNewPassword(it)) },
            reEnterPassword = state.reEnterPassword,
            reEnterPasswordError = state.reEnterPasswordError,
            onChangeReEnterPassword = {
                viewModel.onEvent(
                    ProfileScreenEvents.OnChangeReEnterPassword(
                        it
                    )
                )
            },
            onSaveChanges = {
                viewModel.onEvent(ProfileScreenEvents.OnSavePasswordChanges)
            }
        )
    }

    CustomAlertDialog(
        showDialog = state.showSignOutDialog,
        onDismissRequest = {
            viewModel.onEvent(ProfileScreenEvents.OnShowSignOutDialog(false))
        },
        onConfirmation = {
            viewModel.onEvent(ProfileScreenEvents.OnSignOut)
        },
        dialogTitle = stringResource(R.string.profile_dialog_sign_out_title),
        dialogText = stringResource(R.string.profile_dialog_sign_out_text),
        icon = Icons.AutoMirrored.Filled.Logout,
        confirmText = stringResource(R.string.profile_row_sign_out),
        dismissText = stringResource(R.string.profile_btn_cancel),
        confirmColor = MaterialTheme.colorScheme.primary,
    )

    // Delete step 1: explains what gets deleted
    CustomAlertDialog(
        showDialog = state.showDeleteAccountDialog,
        onDismissRequest = {
            viewModel.onEvent(ProfileScreenEvents.OnShowDeleteAccountDialog(false))
        },
        onConfirmation = {
            //continue to the password + slide sheet
            viewModel.onEvent(ProfileScreenEvents.OnShowDeleteConfirmSheet(true))
        },
        dialogTitle = stringResource(R.string.profile_dialog_delete_title),
        dialogText = stringResource(R.string.profile_dialog_delete_text),
        icon = Icons.Filled.DeleteForever,
        confirmText = stringResource(R.string.profile_btn_continue),
        dismissText = stringResource(R.string.profile_btn_cancel),
        confirmColor = MaterialTheme.colorScheme.error,
    )

    // Delete step 2: password + slide to confirm
    ModalBottomSheetDialog(
        showDialog = state.showDeleteConfirmSheet,
        onDismissRequest = {
            viewModel.onEvent(ProfileScreenEvents.OnShowDeleteConfirmSheet(false))
        }
    ) {
        DeleteAccountSheet(
            email = state.profile?.email.orEmpty(),
            password = state.password,
            onPasswordChange = {
                viewModel.onEvent(ProfileScreenEvents.OnChangePassword(it))
            },
            isLoading = state.isLoading,
            onConfirm = {
                viewModel.onEvent(ProfileScreenEvents.OnDeleteAccount)
            }
        )
    }

    ModalBottomSheetDialog(
        showDialog = state.showCredentialsDialog,
        onDismissRequest = {
            viewModel.onEvent(ProfileScreenEvents.OnShowCredentialsDialog(false))
        }
    ) {
        ProfileReauthenticateUser(
            email = state.profile?.email.orEmpty(),
            password = state.password,
            passwordError = state.passwordError,
            onPasswordChange = {
                viewModel.onEvent(ProfileScreenEvents.OnChangePassword(it))
            },
            onValidateCredentials = {
                viewModel.onEvent(ProfileScreenEvents.OnValidateCredentials)
            }
        )
    }


    ModalBottomSheetDialog(
        showDialog = state.showUpdateEmailDialog,
        onDismissRequest = {
            viewModel.onEvent(ProfileScreenEvents.OnShowUpdateEmailDialog(false))
        }
    ) {
        ProfileUpdateEmail (
            email = state.newEmail,
            emailError = state.emailError,
            onEmailChange = {
                viewModel.onEvent(ProfileScreenEvents.OnChangeNewEmail(it))
            },
            onSaveChanges = {
                viewModel.onEvent(ProfileScreenEvents.OnSaveEmailChanges)
            }
        )
    }


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

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileHeader(
                photo = state.profile?.photo.orEmpty(),
                onPopBackStack = {
                    onEvent(ProfileScreenEvents.OnPopBackStack)
                }
            )

            val profile = state.profile
            if (profile == null) {
                // First load: wait for the profile so "not verified" doesn't flash before the real status
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profile.name.ifEmpty { stringResource(R.string.profile_lbl_hello_user) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))
                if (profile.isEmailVerified) {
                    VerifiedLabel()
                } else {
                    VerifyEmailCard(
                        email = profile.email,
                        onSendLink = { onEvent(ProfileScreenEvents.OnSendEmailVerification) }
                    )
                }
            }

            ProfileButtons(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 16.dp),
                onEditProfile = { onEvent(ProfileScreenEvents.OnShowEditDialog(true)) },
                onUpdatePassword = { onEvent(ProfileScreenEvents.OnShowUpdatePasswordDialog(true)) },
                onUpdateEmail = { onEvent(ProfileScreenEvents.OnShowUpdateEmailDialog(true)) },
                onDeleteAccount = { onEvent(ProfileScreenEvents.OnShowDeleteAccountDialog(true)) },
                onSignOut = { onEvent(ProfileScreenEvents.OnShowSignOutDialog(true)) }
            )
        }
    }
}

// Small green label, nothing to tap
@Composable
private fun VerifiedLabel() {
    val green = colorResource(R.color.verified_green_text)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            tint = green,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.profile_lbl_verified),
            style = MaterialTheme.typography.labelLarge,
            color = green
        )
    }
}

// Says what to do and gives a clear button, instead of a tappable pill
@Composable
private fun VerifyEmailCard(
    email: String,
    onSendLink: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MarkEmailUnread,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_verify_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.profile_verify_text, email),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            TextButton(onClick = onSendLink) {
                Text(stringResource(R.string.profile_btn_send_link))
            }
        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    DenuSpendTheme {
        Surface {
            ProfileScreenContent(state = ProfileScreenState()) {}
        }
    }
}