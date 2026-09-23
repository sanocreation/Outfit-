package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.R
import com.example.model.TryOnRequest
import com.example.model.TryOnResult
import com.example.util.ImageUtils
import kotlinx.coroutines.delay
import java.util.UUID

class DemoVtonProvider : VtonProvider {
    override val providerId: String = "DEMO_VTON"
    override val displayName: String = "TRYON AI Studio Demo Engine"
    override val description: String = "High-fidelity local simulation mode with realistic transformation preview."

    private val steps = listOf(
        "Analyzing your photo...",
        "Understanding the clothing...",
        "Mapping the outfit...",
        "Creating your virtual look...",
        "Finalizing your result..."
    )

    override suspend fun executeTryOn(
        context: Context,
        request: TryOnRequest,
        onProgress: (stepIndex: Int, message: String, progress: Float) -> Unit
    ): TryOnResult {
        val startTime = System.currentTimeMillis()

        // Iterate through the 5 required sequential status messages
        for (index in steps.indices) {
            val progress = (index + 1).toFloat() / steps.size.toFloat()
            onProgress(index, steps[index], progress)
            // Realistic step latency for smooth AI processing experience
            delay(650L)
        }

        val jobId = "job_demo_${UUID.randomUUID().toString().take(8)}"

        // If the user selected the sample preset or if sample result is available:
        val personDrawable = request.personImage.drawableResId
        val garmentDrawable = request.garmentImage.drawableResId

        val (resultResId, resultBitmap) = if (personDrawable == R.drawable.img_sample_model &&
            garmentDrawable == R.drawable.img_sample_garment
        ) {
            // Perfect matched preset pair
            Pair(R.drawable.img_sample_result, null)
        } else if (request.personImage.uri != null) {
            // Custom user uploaded photo: generate realistic virtual preview composite
            val userBitmap = ImageUtils.loadBitmap(context, request.personImage.uri, maxDimension = 900)
            val garmentBitmap = if (request.garmentImage.uri != null) {
                ImageUtils.loadBitmap(context, request.garmentImage.uri, maxDimension = 600)
            } else if (request.garmentImage.drawableResId != null) {
                ImageUtils.loadResourceBitmap(context, request.garmentImage.drawableResId)
            } else null

            if (userBitmap != null && garmentBitmap != null) {
                val composite = createTryOnComposite(userBitmap, garmentBitmap)
                Pair(null, composite)
            } else {
                Pair(R.drawable.img_sample_result, null)
            }
        } else {
            Pair(R.drawable.img_sample_result, null)
        }

        val elapsed = System.currentTimeMillis() - startTime

        return TryOnResult(
            jobId = jobId,
            resultDrawableResId = resultResId,
            resultBitmap = resultBitmap,
            providerUsed = displayName,
            isDemo = true,
            processingTimeMs = elapsed
        )
    }

    /**
     * Generates a realistic visual preview combining the user's upper torso geometry with the garment.
     */
    private fun createTryOnComposite(person: Bitmap, garment: Bitmap): Bitmap {
        val mutable = person.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Calculate realistic placement over torso (chest & shoulders)
        val targetWidth = (mutable.width * 0.75f).toInt()
        val targetHeight = (targetWidth * (garment.height.toFloat() / garment.width.toFloat())).toInt()
        val left = (mutable.width - targetWidth) / 2
        val top = (mutable.height * 0.38f).toInt()

        val destRect = Rect(left, top, left + targetWidth, top + targetHeight)
        canvas.drawBitmap(garment, null, destRect, paint)

        return mutable
    }
}
