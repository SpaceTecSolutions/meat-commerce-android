package com.spacetecsolutions.meatapp.core.data.firebase.image

import java.io.File

internal data class ProcessedImage(
    val file: File,
    val width: Int,
    val height: Int,
    val quality: Int,
)
