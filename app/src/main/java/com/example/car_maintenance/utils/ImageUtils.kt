package com.example.car_maintenance.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

object ImageUtils {
    
    fun getImageDirectory(context: Context, carId: Long): File {
        val dir = File(context.filesDir, "images/car_$carId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun getThumbnailDirectory(context: Context, carId: Long): File {
        val dir = File(context.filesDir, "images/car_$carId/thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun saveImage(
        context: Context,
        uri: Uri,
        carId: Long,
        activityId: Long,
        quality: Int = 85
    ): Pair<String, String>? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            // Rotate image if needed based on EXIF data
            val rotatedBitmap = rotateImageIfRequired(context, uri, originalBitmap)
            
            // Compress and save full image
            val imageFile = File(
                getImageDirectory(context, carId),
                "activity_${activityId}_${UUID.randomUUID()}.jpg"
            )
            val compressedBitmap = compressImage(rotatedBitmap, 1920, 1080)
            saveBitmapToFile(compressedBitmap, imageFile, quality)
            
            // Create and save thumbnail
            val thumbnailFile = File(
                getThumbnailDirectory(context, carId),
                "thumb_${imageFile.name}"
            )
            val thumbnailBitmap = createThumbnail(rotatedBitmap, 200, 200)
            saveBitmapToFile(thumbnailBitmap, thumbnailFile, 80)
            
            // Clean up
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle()
            }
            originalBitmap.recycle()
            compressedBitmap.recycle()
            thumbnailBitmap.recycle()
            
            return Pair(imageFile.absolutePath, thumbnailFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun rotateImageIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()
            
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: IOException) {
            return bitmap
        }
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun compressImage(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val ratio = Math.min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun createThumbnail(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    
    private fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
    }
    
    fun deleteImagesForCar(context: Context, carId: Long) {
        val imageDir = getImageDirectory(context, carId)
        if (imageDir.exists()) {
            imageDir.deleteRecursively()
        }
    }
}