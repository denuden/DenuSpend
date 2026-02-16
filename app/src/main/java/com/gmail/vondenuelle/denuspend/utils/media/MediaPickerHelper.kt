//package com.gmail.vondenuelle.denuspend.utils.media
//
//import android.Manifest
//import android.app.AlertDialog
//import android.content.Context
//import android.content.DialogInterface
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.activity.result.ActivityResultCaller
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import java.io.File
//import java.util.Calendar
//
//class MediaPickerHelper(
//    private val caller: ActivityResultCaller,
//    private val context: Context, // AppCompatActivity or Fragment
//    private val viewModel: MediaPickerViewModel, //Viewmodel for configuration change
//) {
////    private var mediaCropperHelper: MediaCropperHelper? = null
//
//
//    private val appContext = context.applicationContext    //use app context
//
//    /**
//     * Initialize cropper if needed
//     */
////    fun initializeCropper() {
////        mediaCropperHelper = MediaCropperHelper(
////            context = context,
////            onCropSuccess = { uri ->
////                if (uri != null) {
////                    viewModel.setMediaUri(uri)
////                } else {
////                    throw NullPointerException("Cannot crop, uri returned from cropper is null")
////                }
////            },
////            onCropError = { error ->
////                error?.printStackTrace()
////            }
////        )
////    }
//
//    //take picture
//    // Register the appropriate ActivityResult
//    private val takePicture =
//        caller.registerForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
//            if (isSaved) {
//                viewModel.confirmCameraResultAndSave()
//            }
//            //If cropper is initialized
////                if (mediaCropperHelper != null && isSaved) {
////                    viewModel.getSpecificUri()?.let { sp -> mediaCropperHelper!!.startCropping(sp) }
////                }
//        }
//
//
//    // Register for picking media (gallery)
//    //Use GetContent() instead of PickVisualMedia due to limitations of accessing images and albums
//    //2026 update now use PickVisualMedia as this is stable
//    private val singlePhotoPickerLauncher =
//        caller.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
//            uri?.let { viewModel.setMediaUri(it) }// Save URI to ViewModel
////                    if (mediaCropperHelper != null) {
////                        viewModel.getSpecificUri()?.let { sp -> mediaCropperHelper!!.startCropping(sp) }
////                    }
//        }
//
//
//    // Register for permissions
//    private val permissionLauncher =
//        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
//
//            //if android 13 and above then get the permission, else just false
//            val readMediaGranted =
//                permissions[if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    Manifest.permission.READ_MEDIA_IMAGES
//                } else {
//                    false
//                }] ?: false
//
//            val readStorageGranted =
//                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
//
//            if (cameraGranted) {
//                openCamera()
//            } else if (readMediaGranted || readStorageGranted) {
//                openPhotoPicker()
//            } else {
//                showPermissionDeniedDialog()
//            }
//        }
//
//    // Open Dialog for Choosing Camera or Gallery
//    fun openMediaOptionPicker() {
//        val choices = arrayOf("Camera", "Gallery")
//        val mBuilder =
//            AlertDialog.Builder(context)
//        mBuilder.setTitle("Choose an action")
//        mBuilder.setSingleChoiceItems(choices, -1) { dialogInterface: DialogInterface, i: Int ->
//            if (choices[i] == "Camera") {
//                checkPermissionsAndOpenCamera()
//            } else {
//                checkPermissionsAndOpenPhotoPicker()
//            }
//            dialogInterface.dismiss()
//        }
//        mBuilder.setNeutralButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
//            dialogInterface.cancel()
//        }
//        val mDialog = mBuilder.create()
//        mDialog.show()
//    }
//
//    private fun openCamera() {
//        val calendar = Calendar.getInstance()
//        val dir = appContext.getExternalFilesDir("camera_pictures")
//
//        dir?.mkdirs()
//        val temporaryFile = File.createTempFile(
//            "IMG_${calendar.timeInMillis}",
//            ".jpg",
//            dir
//        )
//
//        val cameraUri = FileProvider.getUriForFile(appContext,
//            "${appContext.packageName}.provider",
//            temporaryFile
//        )
//
//
//        // Save it temporarily in a field, but do NOT call setMediaUri here
//        viewModel.setTempPendingCameraUri(cameraUri)
//        takePicture.launch(cameraUri)
//    }
//
//    //open gallery chooser
//    private fun openPhotoPicker() {
//        try {
//            singlePhotoPickerLauncher.launch(    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//
//    // Check camera permissions and open camera
//    private fun checkPermissionsAndOpenCamera() {
//        if (hasCameraPermission()) {
//            openCamera()
//        } else {
//            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
//        }
//    }
//
//    // Check camera permissions and open photo picker
//    private fun checkPermissionsAndOpenPhotoPicker() {
//        //api 33 above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (hasReadMediaImagesPermission()) {
//                openPhotoPicker()
//            } else {
//                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
//            }
//        } else {
//            if (hasReadExternalStoragePermission()) {
//                openPhotoPicker()
//            } else {
//                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
//            }
//        }
//    }
//
//    // Check if CAMERA permission is granted
//    private fun hasCameraPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(
//            appContext,
//            Manifest.permission.CAMERA
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    // Check if READ_EXTERNAL_STORAGE is granted (for Android 12 and below)
//    private fun hasReadExternalStoragePermission(): Boolean {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return true // Not needed for Android 13+
//        return ContextCompat.checkSelfPermission(
//            appContext,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    // Check if READ_MEDIA_IMAGES permission is granted (for Android 13+)
//    private fun hasReadMediaImagesPermission(): Boolean {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true // Not needed for Android 12 and below
//        return ContextCompat.checkSelfPermission(
//            appContext,
//            Manifest.permission.READ_MEDIA_IMAGES
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    // Show permission denied message
//    private fun showPermissionDeniedDialog() {
//        AlertDialog.Builder(context)
//            .setTitle("Permission Required")
//            .setMessage("Camera access is needed to take pictures. Go to the app settings and grant the permission.")
//            .setPositiveButton("OK", null)
//            .show()
//    }
//}