package com.gmail.vondenuelle.denuspend.ui.profile

import com.gmail.vondenuelle.denuspend.domain.models.UserModel

data class ProfileScreenState(
    val isLoading: Boolean = false,

    //determines which save to retry after the credentials sheet
    val pendingAction: PendingAction = PendingAction.NONE,

    val showMediaOptionDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showDeleteConfirmSheet: Boolean = false,
    val showCredentialsDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showUpdatePasswordDialog: Boolean = false,
    val showUpdateEmailDialog: Boolean = false,
    val showSignOutDialog: Boolean = false,


    val profile: UserModel? = null,

    //for fields
    val name: String = "",
    val nameError: String = "",
    val photo: String = "",

    val newEmail: String = "",


    val emailError: String = "",
    val password: String = "",
    val passwordError: String = "",

    val newPassword: String = "",
    val newPasswordError: String = "",
    val reEnterPassword: String = "",
    val reEnterPasswordError: String = "",
)

enum class PendingAction { NONE, UPDATE_PASSWORD, UPDATE_EMAIL }
