package com.spacetecsolutions.meatapp.feature.catalog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spacetecsolutions.meatapp.core.designsystem.component.AppBackTopBar
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
internal fun ImageCropDialog(
    source: Uri,
    circular: Boolean,
    onDismiss: () -> Unit,
    onCropped: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap by produceState<Bitmap?>(null, source) { value = loadCropBitmap(context, source) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var saving by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
    )) {
        Scaffold(
            topBar = { AppBackTopBar(if (circular) "Crop Category Photo" else "Crop Product Photo", onDismiss) },
            bottomBar = { Surface(shadowElevation = 6.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(AppSpacing.small),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                    TextButton(onDismiss, Modifier.weight(1f), enabled = !saving) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            val image = bitmap ?: return@TextButton
                            saving = true
                            scope.launch {
                                runCatching { saveCrop(context, image, viewport, zoom, offset) }
                                    .onSuccess(onCropped)
                                saving = false
                            }
                        },
                        modifier = Modifier.weight(1f), enabled = bitmap != null && viewport.width > 0 && !saving,
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Use Photo")
                    }
                }
            } },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(AppSpacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Drag to reposition · Pinch to zoom", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(AppSpacing.medium))
                val shape = if (circular) CircleShape else RoundedCornerShape(20.dp)
                Box(
                    Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(1f).clip(shape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                        .onSizeChanged { viewport = it }
                        .pointerInput(bitmap, viewport) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val image = bitmap ?: return@detectTransformGestures
                                val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                val bounds = panBounds(image, viewport, nextZoom)
                                zoom = nextZoom
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-bounds.x, bounds.x),
                                    (offset.y + pan.y).coerceIn(-bounds.y, bounds.y),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    bitmap?.let {
                        Image(
                            it.asImageBitmap(), null,
                            Modifier.fillMaxSize().graphicsLayer {
                                scaleX = zoom; scaleY = zoom
                                translationX = offset.x; translationY = offset.y
                            },
                            contentScale = ContentScale.Crop,
                        )
                    } ?: CircularProgressIndicator()
                }
            }
        }
    }
}

private suspend fun loadCropBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= 28) ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(context.contentResolver, uri),
    ) { decoder, info, _ ->
        val divisor = max(info.size.width, info.size.height).toFloat() / 2048f
        if (divisor > 1f) decoder.setTargetSize((info.size.width / divisor).toInt(), (info.size.height / divisor).toInt())
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 2048) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
    }
}

private fun panBounds(bitmap: Bitmap, viewport: IntSize, zoom: Float): Offset {
    if (viewport.width == 0) return Offset.Zero
    val base = max(viewport.width / bitmap.width.toFloat(), viewport.height / bitmap.height.toFloat())
    return Offset(
        ((bitmap.width * base * zoom - viewport.width) / 2f).coerceAtLeast(0f),
        ((bitmap.height * base * zoom - viewport.height) / 2f).coerceAtLeast(0f),
    )
}

private suspend fun saveCrop(
    context: Context, bitmap: Bitmap, viewport: IntSize, zoom: Float, offset: Offset,
): String = withContext(Dispatchers.IO) {
    val base = max(viewport.width / bitmap.width.toFloat(), viewport.height / bitmap.height.toFloat())
    val scale = base * zoom
    val cropSize = (viewport.width / scale).toInt().coerceIn(1, minOf(bitmap.width, bitmap.height))
    val originX = (viewport.width - bitmap.width * scale) / 2f + offset.x
    val originY = (viewport.height - bitmap.height * scale) / 2f + offset.y
    val left = (-originX / scale).toInt().coerceIn(0, bitmap.width - cropSize)
    val top = (-originY / scale).toInt().coerceIn(0, bitmap.height - cropSize)
    val cropped = Bitmap.createBitmap(bitmap, left, top, cropSize, cropSize)
    val output = if (cropSize == 1200) cropped else Bitmap.createScaledBitmap(cropped, 1200, 1200, true)
    val directory = File(context.cacheDir, "cropped_images").apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    file.outputStream().buffered().use { check(output.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
    if (output !== cropped) output.recycle()
    cropped.recycle()
    Uri.fromFile(file).toString()
}
