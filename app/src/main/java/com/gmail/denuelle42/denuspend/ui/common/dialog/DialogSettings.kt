package com.gmail.denuelle42.denuboilerplate.ui.common.dialog

import androidx.compose.ui.window.DialogProperties

object DialogSettings {
    val standardDialogProperties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = true)
    val indismissibleDialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = true)
}


