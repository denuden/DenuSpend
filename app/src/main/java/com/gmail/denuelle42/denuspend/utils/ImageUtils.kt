package com.gmail.denuelle42.denuspend.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
//import androidmads.library.qrgenearator.QRGContents
//import androidmads.library.qrgenearator.QRGEncoder
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream


/**
 * Converts Image Uri to File
 */
fun convertImageUriToFile(context: Context, uri: Uri?): File {
    // Get the input stream from the content URI
    val inputStream = uri?.let { context.contentResolver.openInputStream(it) }

    // Generate a file name for the image
    val fileName = "${System.currentTimeMillis()}.jpg"

    // Get the directory where you want to save the image
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    // Create a new file in the directory with the generated file name
    val file = File(directory, fileName)

    // Create an output stream for the file
    val outputStream = FileOutputStream(file)

    // Copy the contents of the input stream to the output stream
    inputStream?.copyTo(outputStream)

    // Close the streams
    inputStream?.close()
    outputStream.close()

    return file
}

//fun generateQRCode(context: Context, data: String, color: Int): Bitmap {
//    val resources = context.resources
//    val displayMetrics = resources.displayMetrics
//
//    val width = displayMetrics.widthPixels
//    val height = displayMetrics.heightPixels
//
//    // Calculate the desired QR code dimension based on the screen size.
//    var dimen = if (width < height) width else height
//    dimen = (dimen * 3) / 4
//
//
//    val qrgEncoder = QRGEncoder(data, null, QRGContents.Type.TEXT, dimen)
//    //set the black part of QR's color
//    qrgEncoder.colorBlack = color
//    return qrgEncoder.bitmap
//}
//
//fun rotateBitmapViaImageProxy(image : ImageProxy, lensFacing : Int?) : Bitmap{
//    val matrix = Matrix().apply{
//        postRotate(image.imageInfo.rotationDegrees.toFloat())
//        if (lensFacing == CameraSelector.LENS_FACING_FRONT){
//                postScale(-1f, 1f) //to mirror front camera result
//            }
//        }
//        val rotatedBitmap = Bitmap.createBitmap(
//        image.toBitmap(),
//        0,
//        0,
//        image.width,
//        image.height,
//        matrix,
//        true
//    )
//
//    return rotatedBitmap
//}

/**
 * Center crop a bitmap
 */
fun Bitmap.centerCrop(desiredWidth : Int, desiredHeight : Int) : Bitmap {
    val xStart = (width - desiredWidth) / 2
    val yStart = (height - desiredHeight) / 2
    if (xStart < 0 || yStart < 0 || desiredHeight > width || desiredHeight > height){
        throw IllegalArgumentException("Invalid Arguments for center cropping")
    }

    return Bitmap.createBitmap(this, xStart, yStart, desiredWidth, desiredHeight)
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
}