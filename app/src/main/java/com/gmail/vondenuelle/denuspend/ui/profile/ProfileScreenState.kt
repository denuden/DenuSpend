package com.gmail.vondenuelle.denuspend.ui.profile

import com.gmail.vondenuelle.denuspend.domain.models.UserModel

data class ProfileScreenState(
    val isLoading: Boolean = false,

    //determines which type if event is clicked
    val typeOfEvent : String = "",

    val showMediaOptionDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showCredentialsDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showUpdatePasswordDialog: Boolean = false,


    val profile: UserModel? = null,

    //for fields
    val name: String = "",
    val nameError: String = "",
    val photo: String = "",
    val photoError: String = "",

    val email: String = "",
    val emailError: String = "",
    val password: String = "",
    val passwordError: String = "",

    val newPassword: String = "",
    val newPasswordError: String = "",
    val reEnterPassword: String = "",
    val showPassword: Boolean = false,
)
