package com.spacetecsolutions.meatapp.feature.catalog

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

internal const val MAX_SOURCE_IMAGE_MB = 8
private const val MAX_SOURCE_IMAGE_BYTES = MAX_SOURCE_IMAGE_MB * 1024L * 1024L

internal fun validateSelectedImage(context: Context, uri: Uri): String? {
    val type = context.contentResolver.getType(uri)
    if (type != null && !type.startsWith("image/")) return "Select a valid image file"
    val queriedSize = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
        if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
    }
    val size = queriedSize ?: runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
    }.getOrNull()
    return if (size != null && size > MAX_SOURCE_IMAGE_BYTES) {
        "Image must be $MAX_SOURCE_IMAGE_MB MB or smaller"
    } else null
}
