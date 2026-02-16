package com.gmail.vondenuelle.denuspend.utils.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class GallerySaver @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Saves an image from a temporary URI into the public gallery (DCIM/Denuspend).
     *
     * Uses scoped storage with RELATIVE_PATH.
     */
    suspend fun saveImageToGallery(sourceUri: Uri): Uri? {
        return withContext(ioDispatcher) {
            Log.d("saver", "saving gallery")
            try {

                val resolver = context.contentResolver

                // Metadata of the image we are inserting
                val contentValues = ContentValues().apply {

                    // File name
                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        "IMG_${System.currentTimeMillis()}.jpg"
                    )

                    // MIME type
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                    // Folder inside DCIM
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Denuspend")

                    // Mark file as pending while writing
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                // External storage volume (Android 10+)
                val collection = MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )

                // Insert a new row in MediaStore
                val newImageUri = resolver.insert(collection, contentValues)

                newImageUri?.let { uri ->

                    // Copy actual file content
                    resolver.openInputStream(sourceUri)?.use { input ->
                        resolver.openOutputStream(uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Mark file as finished writing
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                newImageUri

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    }
}


/// Sample Legacy support for saving in gallery
// Android 9 and below (direct file path access)
/**
 * val imagesDir = Environment.getExternalStoragePublicDirectory(
 *     Environment.DIRECTORY_DCIM // Path to DCIM folder
 * )
 *
 * val file = File(imagesDir, "IMG_${System.currentTimeMillis()}.jpg") // Create file object
 *
 * contentValues.put(MediaStore.Images.Media.DATA, file.absolutePath) // Store absolute path
 *
 * resolver.insert(
 * MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // Legacy external storage URI
 * contentValues
 * )
 *
 *
 */
