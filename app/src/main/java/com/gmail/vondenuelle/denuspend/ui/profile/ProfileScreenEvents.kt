package com.gmail.vondenuelle.denuspend.ui.profile

sealed class ProfileScreenEvents {
   object OnGetUserProfile : ProfileScreenEvents()

   data class OnChangeName(val value : String) : ProfileScreenEvents()
   data class OnChangePhoto(val value : String) : ProfileScreenEvents()

   data class OnShowMediaOptionDialog(val value : Boolean) : ProfileScreenEvents()
   data class OnShowEditDialog(val value : Boolean) : ProfileScreenEvents()

   object OnSendEmailVerification : ProfileScreenEvents()
   object OnSignOut : ProfileScreenEvents()
   object OnSaveChanges : ProfileScreenEvents()
   object OnPopBackStack : ProfileScreenEvents()
}