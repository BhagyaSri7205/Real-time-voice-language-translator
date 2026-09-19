package com.voxtranslate.app.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

sealed class OcrResult {
    data class Success(val text: String) : OcrResult()
    data class Error(val message: String) : OcrResult()
}

/**
 * Wraps ML Kit's on-device Text Recognition. Equivalent to the desktop
 * app's `backend/ocr.py` (which used OpenCV + pytesseract); this uses
 * Google's on-device Latin-script text recognizer, which needs no external
 * Tesseract install and works fully offline.
 */
object OcrManager {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, rotationDegrees)
            val result = recognizer.process(image).await()
            val text = result.text
            if (text.isBlank()) {
                OcrResult.Error("No text found in frame. Point the camera at some text and try again.")
            } else {
                OcrResult.Success(text)
            }
        } catch (e: Exception) {
            OcrResult.Error("Couldn't read text: ${e.message ?: "unknown error"}")
        }
    }

    /**
     * Converts a captured JPEG ImageProxy (as returned by
     * ImageCapture.OnImageCapturedCallback) into a Bitmap. ImageCapture's
     * in-memory JPEG output does not reliably expose a media.Image, so we
     * decode the JPEG bytes directly instead of relying on imageProxy.image.
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer = imageProxy.planes.firstOrNull()?.buffer ?: return null
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    suspend fun recognize(imageProxy: ImageProxy): OcrResult {
        val bitmap = imageProxyToBitmap(imageProxy)
            ?: return OcrResult.Error("Couldn't read the camera frame.")
        // Rotation is already applied by imageProxyToBitmap, so pass 0 here.
        return recognize(bitmap, 0)
    }
}
