package com.vidalvl.app.ui.screens.missions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.vidalvl.app.domain.model.PhotoRules
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

/**
 * Takes a photo in memory (nothing is written to storage). The result is already scaled down
 * to [PhotoRules.MAX_SIDE_PX] and rotated upright; [onDone] runs on the main thread, with null on failure.
 */
internal fun ImageCapture.takeScaledPhoto(workExecutor: Executor, onDone: (Bitmap?) -> Unit) {
    val main = Handler(Looper.getMainLooper())
    takePicture(workExecutor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val bitmap = try {
                image.toUprightScaledBitmap()
            } catch (e: Exception) {
                null
            } finally {
                image.close()
            }
            main.post { onDone(bitmap) }
        }

        override fun onError(exception: ImageCaptureException) {
            main.post { onDone(null) }
        }
    })
}

private fun ImageProxy.toUprightScaledBitmap(): Bitmap {
    val raw = toBitmap()
    // Scale first: a smaller bitmap is cheaper to rotate.
    val (width, height) = PhotoRules.targetSize(raw.width, raw.height)
    val scaled = if (width == raw.width && height == raw.height) raw
    else Bitmap.createScaledBitmap(raw, width, height, true)
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return scaled
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
}

internal fun Bitmap.toJpegBytes(): ByteArray =
    ByteArrayOutputStream().use { out ->
        compress(Bitmap.CompressFormat.JPEG, PhotoRules.JPEG_QUALITY, out)
        out.toByteArray()
    }
