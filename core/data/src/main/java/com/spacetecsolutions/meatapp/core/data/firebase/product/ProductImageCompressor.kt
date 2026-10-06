package com.spacetecsolutions.meatapp.core.data.firebase.product

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.spacetecsolutions.meatapp.core.common.coroutines.AppDispatchers
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.withContext

internal class ProductImageCompressor @Inject constructor(
    private val contentResolver: ContentResolver,
    private val dispatchers: AppDispatchers,
) {
    suspend fun compress(localUri: String): ByteArray = withContext(dispatchers.io) {
        val uri = Uri.parse(localUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = contentResolver.openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options) ?: error("Unsupported image")
        }
        val scale = minOf(1f, MAX_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        ) else bitmap
        ByteArrayOutputStream().use { output ->
            check(resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output))
            if (resized !== bitmap) resized.recycle()
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width / sample, height / sample) > MAX_DIMENSION * 2) sample *= 2
        return sample
    }

    private companion object {
        const val MAX_DIMENSION = 1600
        const val JPEG_QUALITY = 82
    }
}
