package com.spacetecsolutions.meatapp.core.data.firebase.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.spacetecsolutions.meatapp.core.common.coroutines.AppDispatchers
import com.spacetecsolutions.meatapp.core.data.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal class ImageProcessingException(val userMessage: String, cause: Throwable? = null) : IOException(userMessage, cause)

internal class FirebaseImageProcessor @Inject constructor(
    private val contentResolver: ContentResolver,
    private val dispatchers: AppDispatchers,
    @param:ApplicationContext private val context: Context,
) {
    suspend fun process(localUri: String, kind: ImageUploadKind): ProcessedImage = withContext(dispatchers.io) {
        val uri = Uri.parse(localUri)
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        var resized: Bitmap? = null
        var flattened: Bitmap? = null
        var output: File? = null
        try {
            val decodedBitmap = decode(uri, kind.maxDimension)
            decoded = decodedBitmap
            val orientedBitmap = applyExifOrientation(uri, decodedBitmap)
            oriented = orientedBitmap
            val source = orientedBitmap
            val scale = minOf(1f, kind.maxDimension.toFloat() / maxOf(source.width, source.height))
            val resizedBitmap = if (scale < 1f) Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            ) else source
            resized = resizedBitmap
            val flattenedBitmap = flattenOnWhite(resizedBitmap)
            flattened = flattenedBitmap

            val directory = File(context.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }
            if (!directory.isDirectory) throw ImageProcessingException(context.getString(R.string.image_processing_no_cache))
            val outputFile = File.createTempFile("upload_", ".jpg", directory)
            output = outputFile
            var encodedQuality = QUALITY_STEPS.last()
            for (quality in QUALITY_STEPS) {
                coroutineContext.ensureActive()
                FileOutputStream(outputFile, false).use { stream ->
                    if (!flattenedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                        throw ImageProcessingException(context.getString(R.string.image_processing_encode_failed))
                    }
                    stream.fd.sync()
                }
                encodedQuality = quality
                if (outputFile.length() <= kind.targetBytes) break
            }
            if (outputFile.length() > kind.maxBytes) {
                throw ImageProcessingException(context.getString(R.string.image_processing_too_large))
            }
            ProcessedImage(outputFile, flattenedBitmap.width, flattenedBitmap.height, encodedQuality)
        } catch (cancelled: CancellationException) {
            output?.delete()
            throw cancelled
        } catch (error: ImageProcessingException) {
            output?.delete()
            throw error
        } catch (error: SecurityException) {
            output?.delete()
            throw ImageProcessingException(context.getString(R.string.image_processing_read_failed), error)
        } catch (error: OutOfMemoryError) {
            output?.delete()
            throw ImageProcessingException(context.getString(R.string.image_processing_low_memory), error)
        } catch (error: Exception) {
            output?.delete()
            throw ImageProcessingException(context.getString(R.string.image_processing_unsupported), error)
        } finally {
            listOf(flattened, resized, oriented, decoded).distinct().forEach { bitmap ->
                if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
            }
            cleanupTemporaryCrop(uri)
        }
    }

    private fun decode(uri: Uri, maxDimension: Int): Bitmap {
        val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            // Cropped images are written into the app cache and passed back as file:// URIs.
            // ImageDecoder's ContentResolver source is intended for content URIs and can reject
            // file URIs on some Android versions/devices.
            val source = if (uri.scheme == ContentResolver.SCHEME_FILE) {
                ImageDecoder.createSource(File(requireNotNull(uri.path)))
            } else {
                ImageDecoder.createSource(contentResolver, uri)
            }
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                val edge = maxOf(info.size.width, info.size.height)
                if (edge > maxDimension * 2) {
                    val scale = maxDimension * 2f / edge
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw ImageProcessingException(context.getString(R.string.image_processing_unsupported))
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension * 2)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw ImageProcessingException(context.getString(R.string.image_processing_unsupported))
        }
        return bitmap
    }

    private fun openInputStream(uri: Uri) = if (uri.scheme == ContentResolver.SCHEME_FILE) {
        uri.path?.let(::File)?.inputStream()
    } else {
        contentResolver.openInputStream(uri)
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        // ImageDecoder applies EXIF orientation itself; BitmapFactory does not.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) return bitmap
        val orientation = contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                else -> return bitmap
            }
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flattenOnWhite(source: Bitmap): Bitmap = Bitmap.createBitmap(
        source.width,
        source.height,
        Bitmap.Config.ARGB_8888,
    ).also { output ->
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (maxOf(width / sample, height / sample) > maxEdge && sample <= Int.MAX_VALUE / 2) sample *= 2
        return sample
    }

    private fun cleanupTemporaryCrop(uri: Uri) {
        if (uri.scheme != "file") return
        runCatching {
            val cropDirectory = File(context.cacheDir, "cropped_images").canonicalFile
            val source = File(requireNotNull(uri.path)).canonicalFile
            if (source.parentFile == cropDirectory) source.delete()
        }
    }

    private companion object {
        const val CACHE_DIRECTORY = "processed_image_uploads"
        val QUALITY_STEPS = intArrayOf(95, 92, 90, 88, 85, 82, 80)
    }
}
