package com.spacetecsolutions.meatapp.core.designsystem.component

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons

enum class AppImageCrop { ROUNDED, CIRCLE, NONE }

@Composable
fun AppImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    crop: AppImageCrop = AppImageCrop.ROUNDED,
    cornerRadius: Dp = 12.dp,
    contentScale: ContentScale = ContentScale.Crop,
    filterQuality: FilterQuality = FilterQuality.Low,
) {
    val context = LocalContext.current
    val remoteUrl = (model as? String)?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    var failed by remember(model) { mutableStateOf(false) }
    val encryptedBytes by produceState<ByteArray?>(remoteUrl?.let(EncryptedImageCache::peek), remoteUrl) {
        value = remoteUrl?.let { EncryptedImageCache.load(context.applicationContext, it) }
        if (remoteUrl != null && value == null) failed = true
    }
    val localModel = when {
        remoteUrl != null -> encryptedBytes
        model is String && (model.startsWith("content:") || model.startsWith("file:")) -> Uri.parse(model)
        else -> model
    }
    val shape = crop.shape(cornerRadius)
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        when {
            remoteUrl != null && encryptedBytes == null && !failed ->
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            localModel != null && !failed -> {
                val request = remember(localModel, remoteUrl) {
                    ImageRequest.Builder(context)
                        .data(localModel)
                        .memoryCacheKey(remoteUrl ?: localModel.toString())
                        .diskCachePolicy(if (remoteUrl != null) CachePolicy.DISABLED else CachePolicy.ENABLED)
                        .crossfade(if (remoteUrl == null) 0 else 100)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    filterQuality = filterQuality,
                    onError = { failed = true },
                )
            }
            else -> Icon(painter = painterResource(AppIcons.ProductsDefaultIcon), null, tint = Color.Unspecified)
        }
    }
}

@Composable
fun RoundImage(model: Any?, contentDescription: String?, size: Dp, modifier: Modifier = Modifier) =
    AppImage(model, contentDescription, modifier.size(size), crop = AppImageCrop.CIRCLE)

@Composable
fun RoundedSquareImage(
    model: Any?,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) = AppImage(model, contentDescription, modifier.size(size), AppImageCrop.ROUNDED, cornerRadius)

private fun AppImageCrop.shape(radius: Dp): Shape = when (this) {
    AppImageCrop.ROUNDED -> RoundedCornerShape(radius)
    AppImageCrop.CIRCLE -> CircleShape
    AppImageCrop.NONE -> RoundedCornerShape(0.dp)
}

/** Evicts immutable image URLs after an administrator has successfully deleted catalog media. */
fun evictCachedImages(context: android.content.Context, urls: Collection<String>) =
    EncryptedImageCache.evict(context.applicationContext, urls)
