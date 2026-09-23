package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.model.QualityIssue
import com.example.model.UploadedImage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    /**
     * Inspects a Uri and creates an UploadedImage with real metadata and quality checks.
     */
    fun analyzeUri(context: Context, uri: Uri): UploadedImage {
        val resolver = context.contentResolver
        var fileName = "image_${System.currentTimeMillis()}.jpg"
        var fileSize = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        val mimeType = resolver.getType(uri) ?: "image/jpeg"

        // Decode bounds
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight

        // Quality issue detection
        val qualityIssues = mutableListOf<QualityIssue>()
        if (width > 0 && height > 0) {
            if (width < 450 || height < 450) {
                qualityIssues.add(QualityIssue.LOW_RESOLUTION)
            }
            val ratio = height.toFloat() / width.toFloat()
            if (ratio < 0.7f || ratio > 2.0f) {
                qualityIssues.add(QualityIssue.UNUSUAL_ASPECT_RATIO)
            }
        }
        if (fileSize > 10 * 1024 * 1024) {
            qualityIssues.add(QualityIssue.FILE_TOO_LARGE)
        }

        val isSupportedFormat = mimeType.contains("jpeg") || mimeType.contains("jpg") ||
                mimeType.contains("png") || mimeType.contains("webp")
        if (!isSupportedFormat) {
            qualityIssues.add(QualityIssue.NON_STANDARD_FORMAT)
        }

        return UploadedImage(
            uri = uri,
            fileName = fileName,
            fileSizeBytes = fileSize,
            width = width,
            height = height,
            mimeType = mimeType,
            isValid = width > 0 && height > 0,
            qualityIssues = qualityIssues
        )
    }

    /**
     * Loads a downscaled Bitmap from a Uri for safe preview/processing.
     */
    fun loadBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        var inSampleSize = 1
        val maxSide = maxOf(options.outWidth, options.outHeight)
        while (maxSide / (inSampleSize * 2) >= maxDimension) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    /**
     * Decodes a drawable resource into a Bitmap.
     */
    fun loadResourceBitmap(context: Context, resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    /**
     * Saves the try-on result image to Android MediaStore Pictures with clean filename tryon-ai-result.jpg.
     */
    fun saveImageToGallery(context: Context, bitmap: Bitmap): Result<Uri> {
        return try {
            val filename = "tryon-ai-result_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TryOnAI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Failed to create MediaStore entry"))

                resolver.openOutputStream(imageUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                Result.success(imageUri)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "TryOnAI").apply { mkdirs() }
                val imageFile = File(appDir, filename)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                Result.success(Uri.fromFile(imageFile))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Triggers native Android share intent.
     */
    fun shareResult(context: Context, resultUri: Uri?, shareText: String = "See how this outfit looks on me with TRYON AI.") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (resultUri != null) "image/jpeg" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My AI Virtual Try-On")
            putExtra(Intent.EXTRA_TEXT, shareText)
            if (resultUri != null) {
                putExtra(Intent.EXTRA_STREAM, resultUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Share Virtual Look")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
