package com.gmail.vondenuelle.denuspend.utils.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.Calendar


fun createCameraUri(context: Context): Uri {
    return try {
        val calendar = Calendar.getInstance()
        val dir = context.getExternalFilesDir("camera_pictures")
        dir?.mkdirs()

        val tempFile = File.createTempFile(
            "IMG_${calendar.timeInMillis}",
            ".jpg",
            dir
        )

        val cameraUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )
        cameraUri
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}