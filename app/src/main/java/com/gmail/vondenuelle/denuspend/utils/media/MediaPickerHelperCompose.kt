package com.gmail.vondenuelle.denuspend.utils.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MediaPickerHelperCompose(
    private val context: Context,
    private val viewModel: MediaPickerViewModel,
    // ActivityResultLaunchers are provided from Compose
    private val takePictureLauncher: ActivityResultLauncher<Uri>,
    private val photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
) {

    private val appContext = context.applicationContext


    /*** Camera & Gallery ***/
    private fun openCamera() {
        val cameraUri = createCameraUri(context)

        // Save URI in ViewModel
        viewModel.setTempPendingCameraUri(cameraUri)
        takePictureLauncher.launch(cameraUri)
    }

    private fun openPhotoPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    /*** Permissions ***/
    fun checkPermissionsAndOpenCamera() {
        if (hasCameraPermission()) openCamera()
        else permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }


    // Check camera permissions and open photo picker
    fun checkPermissionsAndOpenPhotoPicker() {
        //api 33 above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (hasReadMediaImagesPermission()) {
                openPhotoPicker()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasReadMediaImagesPermission()) {
                openPhotoPicker()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            }
        } else {
            if (hasReadExternalStoragePermission()) {
                openPhotoPicker()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    // Check if CAMERA permission is granted
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Check if READ_EXTERNAL_STORAGE is granted (for Android 12 and below)
    private fun hasReadExternalStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return true // Not needed for Android 13+
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Check if READ_MEDIA_IMAGES permission is granted (for Android 13+)
    private fun hasReadMediaImagesPermission(): Boolean {
        return when {
            // Android 14 (API 34) and above: partial access via user-selected photos/videos
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED
            }

            // Android 13 (API 33): full access via READ_MEDIA_IMAGES or READ_MEDIA_VIDEO
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val hasImages = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED

                val hasVideos = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED

                hasImages || hasVideos
            }

            // Android 12 and below: legacy READ_EXTERNAL_STORAGE
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

}
