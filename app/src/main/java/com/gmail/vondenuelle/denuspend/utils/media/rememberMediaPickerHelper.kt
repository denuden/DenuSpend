package com.gmail.vondenuelle.denuspend.utils.media

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberMediaPickerHelper(
    mediaPickerViewModel: MediaPickerViewModel
): MediaPickerHelperCompose {
    val context = LocalContext.current

    // Launchers must be created inside a Composable
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSaved ->
        if (isSaved) mediaPickerViewModel.confirmCameraResultAndSave()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            // Persist access for Android 14+
            if (Build.VERSION.SDK_INT >= 34) {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            mediaPickerViewModel.setMediaUri(selectedUri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false

        //if android 13 and above then get the permission, else just false
        val readMediaGranted =
            permissions[if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                false
            }] ?: false

        val readStorageGranted =
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        when {
            cameraGranted -> {
                val cameraUri = createCameraUri(context)

                // Save URI in ViewModel
                mediaPickerViewModel.setTempPendingCameraUri(cameraUri)
                takePictureLauncher.launch(cameraUri)
            }

            readMediaGranted || readStorageGranted -> photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )

            else -> {
                Toast.makeText(context, "Camera and storage access is needed. Go to settings and allow permissions.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Create helper with launchers & viewmodel
    return remember {
        MediaPickerHelperCompose(
            context = context,
            viewModel = mediaPickerViewModel,
            takePictureLauncher = takePictureLauncher,
            photoPickerLauncher = photoPickerLauncher,
            permissionLauncher = permissionLauncher
        )
    }
}


